package com.ytdinfo.inndoo.common.utils;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class HttpClientUtil {

    private static final int                                    socketTimeout            = 10000;

    private static final int                                    connectTimeout           = 60000;

    private static final int                                    connectionRequestTimeout = 10000;
    /**
     * 最大不要超过1000
     */
    private static final int                                    maxConnTotal             = 500;

    /**
     * 实际的单个连接池大小，如tps定为50，那就配置50
     */
    private static final int                                    maxConnPerRoute          = 100;

    private static final int                                    retryExecutionCount      = 3;

    /** 利用AtomicReference */
    private static final AtomicReference<CloseableHttpClient> HTTP_CLIENT_INSTANCE = new AtomicReference<>();

    /**
     * 用CAS确保线程安全
     */
    public static final CloseableHttpClient getHttpClient(){
        for (;;) {
            CloseableHttpClient current = HTTP_CLIENT_INSTANCE.get();
            if (current != null) {
                return current;
            }
            current = createHttpClient(maxConnTotal,maxConnPerRoute,socketTimeout,retryExecutionCount);
            if (HTTP_CLIENT_INSTANCE.compareAndSet(null, current)) {
                return current;
            }
        }
    }

    /**
     * @param maxTotal            maxTotal
     * @param maxPerRoute         maxPerRoute
     * @param socketTimeout       socketTimeout
     * @param retryExecutionCount retryExecutionCount
     * @return CloseableHttpClient
     */
    public static CloseableHttpClient createHttpClient(int maxTotal, int maxPerRoute, int socketTimeout, int retryExecutionCount) {
		SSLContext sslContext = SSLContexts.createDefault();
		try {
			sslContext.init(null, new TrustManager[]{trustAllManager}, null);
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
		// 创建Registry
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)
				.register("https",new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
				.build();
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		// 整个连接池最大连接数
		connectionManager.setMaxTotal(maxTotal);
		// 每路由最大连接数，默认值是2
		connectionManager.setDefaultMaxPerRoute(maxPerRoute);

//            RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT)
//                    .setExpectContinueEnabled(Boolean.TRUE).setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM,AuthSchemes.DIGEST))
//                    .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).build();
        IdleConnectionMonitorThread connectionMonitorThread = new IdleConnectionMonitorThread(connectionManager);
        connectionMonitorThread.start();
        try {
            connectionMonitorThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
		return HttpClientBuilder.create()
				.setConnectionManager(connectionManager)
//                    .setDefaultRequestConfig(requestConfig)
				.setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(socketTimeout).build())
				.setRetryHandler(new HttpRequestRetryHandlerImpl(retryExecutionCount))
				.build();
    }

    private static TrustManager trustAllManager = new X509TrustManager() {

        @Override
        public void checkClientTrusted(
                java.security.cert.X509Certificate[] arg0, String arg1)
                throws CertificateException {
        }

        @Override
        public void checkServerTrusted(
                java.security.cert.X509Certificate[] arg0, String arg1)
                throws CertificateException {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }

    };

    /**
     *
     * HttpClient  超时重试
     * @author LiYi
     */
    private static class HttpRequestRetryHandlerImpl implements HttpRequestRetryHandler {

        private int retryExecutionCount;

        public HttpRequestRetryHandlerImpl(int retryExecutionCount){
            this.retryExecutionCount = retryExecutionCount;
        }

        @Override
        public boolean retryRequest(
                IOException exception,
                int executionCount,
                HttpContext context) {
            if (executionCount > retryExecutionCount) {
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                return false;
            }
            if (exception instanceof UnknownHostException) {
                return false;
            }
            if (exception instanceof ConnectTimeoutException) {
                return true;
            }
            if (exception instanceof SSLException) {
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent) {
                // Retry if the request is considered idempotent
                return true;
            }
            return false;
        }

    }

    public static class IdleConnectionMonitorThread extends Thread {

        private final HttpClientConnectionManager connMgr;
        private volatile boolean shutdown;

        public IdleConnectionMonitorThread(HttpClientConnectionManager connMgr) {
            super();
            this.connMgr = connMgr;
        }

        @Override
        public void run() {
            try {
                while (!shutdown) {
                    synchronized (this) {
                        wait(1000);
                        // Close expired connections
                        connMgr.closeExpiredConnections();
                        // Optionally, close connections
                        // that have been idle longer than 30 sec
                        connMgr.closeIdleConnections(30, TimeUnit.SECONDS);
                    }
                }
            } catch (InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }

    }
}
