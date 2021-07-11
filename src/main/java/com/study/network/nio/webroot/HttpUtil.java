package com.study.network.nio.webroot;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Http工具类
 */
public class HttpUtil {
    /**
     * 解析Http请求的数据
     * @param data
     * @return
     */
    public static HttpRequest resolver(byte[] data) {
        // 使用BufferedReader，方便一行行读取
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data)));
        HttpRequest httpRequest = null;
        try {
            // 解析请求的第一行内容，通过空格分隔数据
            String[] s = reader.readLine().split(" ");
            httpRequest = new HttpRequest();

            // 获取请求的url，请求的方法和http版本号
            String url = s[1];
            int i = url.indexOf("?");
            httpRequest.setUrl(i == -1 ? url : url.substring(0, i));
            httpRequest.setMethod(s[0]);
            httpRequest.setVersion(s[2]);

            // 如果url中带了参数，解析参数
            if (i >= 0) {
                String[] params = url.substring(i + 1).split("&");
                for (String param : params) {
                    int eqi = param.indexOf("=");
                    httpRequest.getParams().put(param.substring(0, eqi), param.substring(eqi + 1));
                }
            }

            // 解析请求的头信息
            String str;
            Map<String, String> header = httpRequest.getHeaders();
            while ((str = reader.readLine()) != null) {
                if (str.length() == 0) {
                    break;
                }

                int index = str.indexOf(":");
                header.put(str.substring(0, index), str.substring(index + 1));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return httpRequest;
    }

    /**
     * 创建响应头信息
     * @param code
     * @param contentLength
     * @return
     */
    public static String createResponseHead(int code, long contentLength) {
        String strCode = "";
        if (code == 200) {
            strCode = "200 OK";
        } else {
            strCode = String.valueOf(code);
        }
        String str = "HTTP/1.1 " + strCode + "\r\n" +
                "Context-Type: text/html; charset=UTF-8\r\n"+
                "Content-Length: " + contentLength + "\r\n\r\n";
        return str;
    }
}
