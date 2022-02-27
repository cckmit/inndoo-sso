package com.ytdinfo.inndoo.common.utils;

import cn.hutool.core.util.StrUtil;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 *
 * @author timmy
 * @date 2019/9/25
 */
@Service
public class ServerUtil {

    private static String serverIP;
    private static String serverPort;
    private static String serverIPPort;

    /**
     * 获取服务器ip:port
     * @return
     */
    public String getServerIPPort(){
        if(StrUtil.isEmpty(serverIPPort)){
            serverIPPort = getIpAddress() + ":" + getPort();
        }
        return serverIPPort;
    }

    /**
     * 获取项目端口号
     * @return
     */
    public String getPort(){
        if(StrUtil.isNotEmpty(serverPort)){
            return serverPort;
        }
        Environment env = SpringContextUtil.getBean(Environment.class);
        String port = env.getProperty("server.port");
        serverPort = port;
        return port;
    }


    /**
     * 根据网卡获得IP地址
     * @return
     * @throws SocketException
     * @throws UnknownHostException
     */
    public String getIpAddress(){
        if(StrUtil.isNotEmpty(serverIP)){
            return serverIP;
        }
        String ip = "";
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                String name = intf.getName();
                if (!name.contains("docker") && !name.contains("lo")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        //获得IP
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            String ipaddress = inetAddress.getHostAddress().toString();
                            if (!ipaddress.contains("::") && !ipaddress.contains("0:0:") && !ipaddress.contains("fe80")) {

                                System.out.println(ipaddress);
                                if (!"127.0.0.1".equals(ip)) {
                                    ip = ipaddress;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        serverIP = ip;
        return ip;
    }


}