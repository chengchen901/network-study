package com.study.network.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author Hash
 * @date 2021年06月10日 13:43
 */
public class BIOServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080);) {
            System.out.println("服务器启动成功");
            while (!serverSocket.isClosed()) {
                try (Socket request = serverSocket.accept();) { // 阻塞
                    System.out.println("收到新连接：" + request.toString());

                    InputStream inputStream = request.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    String msg;
                    while ((msg = reader.readLine()) != null) {
                        if (msg.length() == 0) {
                            break;
                        }
                        System.out.println(msg);
                    }
                    System.out.println("收到数据，来自：" + request.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
