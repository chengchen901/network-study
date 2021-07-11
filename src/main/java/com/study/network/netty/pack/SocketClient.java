package com.study.network.netty.pack;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

/**
 * @author Hash
 * @date 2021年07月11日 19:06
 */
public class SocketClient {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("127.0.0.1", 8080);
        socket.setTcpNoDelay(true);
        OutputStream outputStream = socket.getOutputStream();

        // 消息长度固定为 220字节，包含有
        // 1. 目标用户ID长度为10， 10 000 000 000 ~ 19 999 999 999
        // 2. 消息内容字符串长度最多70。 按一个汉字3字节，内容的最大长度为210字节
        byte[] request = new byte[220];
        byte[] userId = "10000000000".getBytes();
        byte[] content = "我爱你baby你爱我吗我爱你baby你爱我吗我爱你baby你爱我吗我爱你baby你爱我吗".getBytes();
        System.arraycopy(userId, 0, request, 0, 10);
        System.arraycopy(content, 0, request, 10, content.length);

        CountDownLatch countDownLatch = new CountDownLatch(10);
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    countDownLatch.countDown();
                    outputStream.write(request);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        countDownLatch.await();

        /*for (int i = 0; i < 10; i++) {

            outputStream.write(request);
        }*/
        Thread.sleep(2000L); // 两秒后退出
        socket.close();
    }
}
