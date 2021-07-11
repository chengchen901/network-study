package com.study.network.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 直接基于非阻塞的写法,一个线程处理轮询所有请求
 * @author Hash
 * @date 2021年06月16日 21:28
 */
public class NIOServerV1 {

    private static ArrayList<SocketChannel> channels = new ArrayList<>();

    public static void main(String[] args) {
        // 创建网络服务端
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();) {
            serverSocketChannel.configureBlocking(false); // 设置为非阻塞模式
            serverSocketChannel.socket().bind(new InetSocketAddress(8080)); // 绑定端口
            System.out.println("启动成功");
            while (true) {
                SocketChannel socketChannel = serverSocketChannel.accept(); // 获取新tcp连接通道
                // tcp请求 读取/响应
                if (socketChannel != null) {
                    System.out.println("收到新连接 : " + socketChannel.getRemoteAddress());
                    socketChannel.configureBlocking(false); // 默认是阻塞的,一定要设置为非阻塞
                    channels.add(socketChannel);
                } else {
                    // 没有新连接的情况下,就去处理现有连接的数据,处理完的就删除掉
                    Iterator<SocketChannel> iterator = channels.iterator();
                    while (iterator.hasNext()) {
                        SocketChannel sc = iterator.next();
                        try {
                            ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
                            while (sc.isOpen() && sc.read(requestBuffer) != -1) {
                                // 长连接情况下,需要手动判断数据有没有读取结束 (此处做一个简单的判断: 超过0字节就认为请求结束了)
                                if (requestBuffer.position() > 0) {
                                    System.out.println("requestBuffer.position() > 0");
                                    break;
                                }
                            }
                            if (requestBuffer.position() == 0) {
                                System.out.println("requestBuffer.position() == 0");
                                continue; // 如果没数据了, 则不继续后面的处理
                            }
                            requestBuffer.flip();
                            byte[] content = new byte[requestBuffer.limit()];
                            requestBuffer.get(content);
                            System.out.println(new String(content));
                            System.out.println("收到数据,来自：" + sc.getRemoteAddress());

                            // 响应结果 200
                            String response = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Length: 11\r\n\r\n" +
                                    "Hello World";
                            ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
                            while (buffer.hasRemaining()) {
                                sc.write(buffer);// 非阻塞
                            }
                            iterator.remove();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            // 用到了非阻塞的API, 在设计上,和BIO可以有很大的不同.继续改进
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
