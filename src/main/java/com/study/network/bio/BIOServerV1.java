package com.study.network.bio;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Hash
 * @date 2021年06月10日 13:59
 */
public class BIOServerV1 {
    static ExecutorService executorService = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("服务器启动成功");
            while (!serverSocket.isClosed()) {
                Socket request = serverSocket.accept(); // 阻塞
                    executorService.execute(() -> {
                        try {
                            System.out.println("收到新连接：" + request.toString());

                            InputStream inputStream = request.getInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                            String msg;
                            while ((msg = reader.readLine()) != null) {
                                if (msg.length() == 0 || "bye".equals(msg)) {
                                    break;
                                }
                                System.out.println(msg);
                            }
                            System.out.println("收到数据，来自：" + request.toString());
                            OutputStream outputStream = request.getOutputStream();
                            outputStream.write("hello".getBytes());
                            outputStream.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                request.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                    });
                }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
