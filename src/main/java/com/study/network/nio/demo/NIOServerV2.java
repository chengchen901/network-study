package com.study.network.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Hash
 * @date 2021年06月19日 15:51
 */
public class NIOServerV2 {
    public static void main(String[] args) {
        // 1、创建服务端的channel对象
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();) {
            // 设置非阻塞模式
            serverSocketChannel.configureBlocking(false);

            // 2、创建selector
            Selector selector = Selector.open();

            // 3、把服务端的channel注册到selector，注册accpet事件
            final SelectionKey selectionKey = serverSocketChannel.register(selector, 0);
            selectionKey.interestOps(SelectionKey.OP_ACCEPT);

            // 4、绑定端口，启动服务
            serverSocketChannel.bind(new InetSocketAddress("localhost", 8080));
            System.out.println("启动成功");

            while (true) {
                selector.select();
                final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                final Iterator<SelectionKey> iterator = selectionKeys.iterator();
                while (iterator.hasNext()) {
                    final SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        final SocketChannel socketChannel = ((ServerSocketChannel) (key.channel())).accept();
                        socketChannel.configureBlocking(false);
                        socketChannel.register(selector, SelectionKey.OP_READ);
                        System.out.println("收到新连接：" + socketChannel);
                    } else if (key.isReadable()) {
                        try {
                            SocketChannel socketChannel = (SocketChannel) key.channel();

                            final ByteBuffer requestBuffer = ByteBuffer.allocateDirect(1024);
                            while (socketChannel.isOpen() && socketChannel.read(requestBuffer) != -1) {
                                // 长连接情况下,需要手动判断数据有没有读取结束 (此处做一个简单的判断: 超过0字节就认为请求结束了)
                                if (requestBuffer.position() > 0) {
                                    break;
                                }
                            }
                            // 没有数据
                            if (requestBuffer.position() == 0) {
                                break;
                            }
                            // 转为读取模式
                            requestBuffer.flip();
                            byte[] content = new byte[requestBuffer.remaining()];
                            requestBuffer.get(content);
                            System.out.println(new String(content));
                            System.out.println("收到数据,来自：" + socketChannel.getRemoteAddress());
                            // TODO 业务操作 数据库 接口调用等等

                            // 响应结果 200
                            String response = "HTTP/1.1 200 OK\r\n" +
                                    "Content-Length: 11\r\n\r\n" +
                                    "Hello World";
                            ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
                            while (buffer.hasRemaining()) {
                                socketChannel.write(buffer);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // 移除无用连接
                            key.cancel();
                        }

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
