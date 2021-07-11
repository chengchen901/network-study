package com.study.network.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Hash
 * @date 2021年06月16日 21:40
 */
public class SelectorDemo {
    public static void main(String[] args) {
        // 创建网络服务端
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();) {
            // 创建Selector
            Selector selector = Selector.open();
            serverSocketChannel.configureBlocking(false); // 设置为非阻塞模式
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);// serverSocketChannel注册OP_ACCEPT事件
            serverSocketChannel.socket().bind(new InetSocketAddress(8080)); // 绑定端口

            while (true) {
                int readyChannels = selector.select();// 会阻塞，直到有事件触发
                if (readyChannels == 0) {
                    continue;
                }
                Set<SelectionKey> selectedKeys = selector.selectedKeys();// 获取被触发的事件集合
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                while(keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    if(key.isAcceptable()) {
                        SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
                         sc.register(selector, SelectionKey.OP_READ);
                        // serverSocketChannel 收到一个新连接，只能作用于ServerSocketChannel

                        /*try {
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
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/
                    } else if (key.isConnectable()) {
                        // 连接到远程服务器，只在客户端异步连接时生效
                    } else if (key.isReadable()) {
                        // SocketChannel 中有数据可以读
                    } else if (key.isWritable()) {
                        // SocketChannel 可以开始写入数据
                    }
                    // 将已处理的事件移除
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
