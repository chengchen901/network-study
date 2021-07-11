package com.study.network.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * @author Hash
 * @date 2021年06月18日 21:42
 */
public class NIOClientV2 {
    public static void main(String[] args) {
        try (SocketChannel socketChannel = SocketChannel.open();
             Selector selector = Selector.open();) {
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress("127.0.0.1", 8080));

            while (true) {
                selector.select();
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey selectionKey = iterator.next();
                    iterator.remove();

                    // 连接到远程服务器
                    if (selectionKey.isConnectable()) {
                        // 完成连接
                        if (socketChannel.finishConnect()) {
                            System.out.println("连接成功-" + socketChannel);

                            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(20480);

                            // 切换到感兴趣的事件
                            selectionKey.attach(byteBuffer);
                            selectionKey.interestOps(SelectionKey.OP_WRITE);
                        }
                    } else if (selectionKey.isWritable()) {
                        // 可以开始写数据
                        ByteBuffer byteBuffer = (ByteBuffer) selectionKey.attachment();
                        byteBuffer.clear();
                        Scanner scanner = new Scanner(System.in);
                        System.out.println("请输入：");
                        // 发送内容
                        String msg = scanner.nextLine();
//                        scanner.close();

                        byteBuffer.put(msg.getBytes());
                        byteBuffer.flip();

                        while (byteBuffer.hasRemaining()) {
                            socketChannel.write(byteBuffer);
                        }

                        // 切换到感兴趣的事件
                        selectionKey.interestOps(SelectionKey.OP_READ);
                    } else if (selectionKey.isReadable()) {
                        // 读取响应
                        System.out.println("收到服务端响应：");
                        ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

                        while (socketChannel.isOpen() && socketChannel.read(byteBuffer) != -1) {
                            // 长连接情况下,需要手动判断数据有没有读取结束 (此处做一个简单的判断: 超过0字节就认为请求结束了)
                            if (byteBuffer.position() > 0) {
                                break;
                            }
                        }
                        byteBuffer.flip();
                        byte[] content = new byte[byteBuffer.remaining()];
                        byteBuffer.get(content);
                        System.out.println(new String(content));
                        selectionKey.interestOps(SelectionKey.OP_WRITE);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
