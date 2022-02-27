package com.ytdinfo.inndoo.modules.base.serviceimpl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ytdinfo.conf.core.XxlConfClient;
import com.ytdinfo.conf.core.listener.XxlConfListener;
import com.ytdinfo.inndoo.modules.base.service.ProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProxyServiceImpl implements ProxyService {

    private static String proxySet;

    private static String proxyHost;

    private static String proxyPort;

    private static String nonHosts;

    static {
         proxySet = XxlConfClient.get("inndoo-sso.proxy.set");
         proxyHost = XxlConfClient.get("inndoo-sso.proxy.host");
         proxyPort = XxlConfClient.get("inndoo-sso.proxy.port");
         nonHosts = XxlConfClient.get("inndoo-sso.proxy.nonhosts");

        /**
         * 配置变更监听
         */
        XxlConfClient.addListener("inndoo-sso.proxy.set", new XxlConfListener() {
            @Override
            public void onChange(String key, String value) throws Exception {
                if ("inndoo-sso.proxy.set".equals(key)) {
                    proxySet = value;
                    setProxy();
                }
            }
        });

        XxlConfClient.addListener("inndoo-sso.proxy.host", new XxlConfListener() {
            @Override
            public void onChange(String key, String value) throws Exception {
                if ("inndoo-sso.proxy.host".equals(key)) {
                    proxyHost = value;
                    setProxy();
                }
            }
        });

        XxlConfClient.addListener("inndoo-sso.proxy.port", new XxlConfListener() {
            @Override
            public void onChange(String key, String value) throws Exception {
                if ("inndoo-sso.proxy.port".equals(key)) {
                    proxyPort = value;
                    setProxy();
                }
            }
        });

        XxlConfClient.addListener("inndoo-sso.proxy.nonhosts", new XxlConfListener() {
            @Override
            public void onChange(String key, String value) throws Exception {
                if ("inndoo-sso.proxy.nonhosts".equals(key)) {
                    nonHosts = value;
                    setProxy();
                }
            }
        });
    }

    @Override
    public void initProxy() {
        setProxy();
    }

    private static void setProxy() {
        if (StrUtil.isEmpty(proxySet) || "false".equals(proxySet)) {
            removeProxy();
        } else if ("true".equals(proxySet)) {
            if (StrUtil.isEmpty(proxyHost) || StrUtil.isEmpty(proxyPort) || !NumberUtil.isInteger(proxyPort)) {
                removeProxy();
                return;
            }
            if (StrUtil.isEmpty(nonHosts)) {
                nonHosts = "localhost";
            } else if (!nonHosts.contains("localhost")) {
                nonHosts = "localhost|" + nonHosts;
            }

            //System.setProperty("proxyType", "4");
            System.setProperty("http.proxySet", proxySet);
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
            System.setProperty("http.nonProxyHosts", nonHosts);

            System.setProperty("https.proxySet", proxySet);
            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", proxyPort);
            System.setProperty("https.nonProxyHosts", nonHosts);
        }
    }

    private static void removeProxy() {
        System.setProperty("http.proxySet", "false");
        System.getProperties().remove("http.proxyHost");
        System.getProperties().remove("http.proxyPort");
        System.getProperties().remove("http.nonProxyHosts");

        System.setProperty("https.proxySet", "false");
        System.getProperties().remove("https.proxyHost");
        System.getProperties().remove("https.proxyPort");
        System.getProperties().remove("https.nonProxyHosts");
    }

}
