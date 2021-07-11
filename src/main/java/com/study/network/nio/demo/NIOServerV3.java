package com.study.network.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NIO selector 多路复用reactor线程模型
 * @author Hash
 * @date 2021年06月19日 19:32
 */
public class NIOServerV3 {

    /** 处理业务操作的线程 */
    private static ExecutorService workPool = Executors.newCachedThreadPool();

    abstract class ReactorThread extends Thread {

        private Selector selector;

        private volatile boolean running = false;

        public ReactorThread() throws IOException {
            this.selector = Selector.open();
        }

        @Override
        public void run() {
            // 轮询Selector事件
            while (running) {
                try {
                    selector.select(1000);

                    final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    final Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        final SelectionKey key = iterator.next();
                        iterator.remove();

                        final int readyOps = key.readyOps();
                        // 关注 Read 和 Accept俩个事件
                        if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                            try {
                                SelectableChannel channel = (SelectableChannel) key.attachment();
                                channel.configureBlocking(false);
                                handler(channel);
                                if (!channel.isOpen()) {
                                    // 如果关闭了,就取消这个KEY的订阅
                                    key.cancel();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                // 如果有异常,就取消这个KEY的订阅
                                key.cancel();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private SelectionKey register(SelectableChannel channel) throws ClosedChannelException {
            return channel.register(selector, 0);
        }

        private void doStart() {
            if ((!running)) {
                running = true;
                start();
            }
        }

        /**
         * Selector监听到有事件后,调用这个方法
         * @author Hash
         * @date 2021/6/19 19:40
         * @param channel
         * @throws Exception
         */
        public abstract void handler(SelectableChannel channel) throws Exception;
    }

    private ServerSocketChannel serverSocketChannel;
    // 1、创建多个线程 - accept处理reactor线程 (accept线程)
    private ReactorThread[] mainReactorThreads = new ReactorThread[1];
    // 2、创建多个线程 - io处理reactor线程  (I/O线程)
    private ReactorThread[] subReactorThreads = new ReactorThread[8];

    /**
     * 初始化线程组
     * @author Hash
     * @date 2021/6/19 19:35
     */
    private void newGroup() throws IOException {
        // 创建IO线程,负责处理客户端连接以后socketChannel的IO读写
        for (int i = 0; i < subReactorThreads.length; i++) {
            subReactorThreads[i] = new ReactorThread() {
                @Override
                public void handler(SelectableChannel channel) throws Exception {
                    // work线程只负责处理IO处理，不处理accept事件
                    SocketChannel socketChannel = (SocketChannel) channel;
                    socketChannel.configureBlocking(false);
                    final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                    while (socketChannel.isOpen() && socketChannel.read(byteBuffer) != -1) {
                        // 长连接情况下,需要手动判断数据有没有读取结束 (此处做一个简单的判断: 超过0字节就认为请求结束了)
                        if (byteBuffer.position() > 0) {
                            break;
                        }
                    }

                    // 如果没数据了, 则不继续后面的处理
                    if (byteBuffer.position() == 0) {
                        return;
                    }

                    byteBuffer.flip();
                    byte[] content = new byte[byteBuffer.remaining()];
                    byteBuffer.get(content);
                    System.out.println(new String(content));
                    System.out.println(Thread.currentThread().getName() + " 收到数据来自：" + socketChannel.getRemoteAddress());

                    // TODO 业务操作 数据库、接口...
                    workPool.submit(() -> {
                    });

                    // 响应结果 200
                    String response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Length: 11\r\n\r\n" +
                            "Hello World";
                    ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
                    while (buffer.hasRemaining()) {
                        socketChannel.write(buffer);
                    }
                }
            };
        }

        // 创建mainReactor线程, 只负责处理serverSocketChannel
        for (int i = 0; i < mainReactorThreads.length; i++) {
            mainReactorThreads[i] = new ReactorThread() {

                AtomicInteger incr = new AtomicInteger(0);

                @Override
                public void handler(SelectableChannel channel) throws Exception {
                    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) channel;
                    final SocketChannel socketChannel = serverSocketChannel.accept();
                    serverSocketChannel.configureBlocking(false);

                    // 收到连接建立的通知之后，分发给I/O线程继续去读取数据
                    int index = incr.getAndIncrement() % subReactorThreads.length;
                    ReactorThread workEventLoop = subReactorThreads[index];
                    workEventLoop.doStart();
                    SelectionKey selectionKey = workEventLoop.register(socketChannel);
                    selectionKey.interestOps(SelectionKey.OP_READ);
                    System.out.println(Thread.currentThread().getName() + "收到新连接 : " + socketChannel.getRemoteAddress());
                }
            };
        }
    }

    /**
     * 初始化服务端的channel,并且开启主线程
     * @author Hash
     * @date 2021/6/19 19:35
     */
    private void initAndRegister() throws IOException {
        // 1、 创建ServerSocketChannel
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        // 2、 将serverSocketChannel注册到selector
        int index = new Random().nextInt(mainReactorThreads.length);
        mainReactorThreads[index].doStart();
        SelectionKey selectionKey = mainReactorThreads[index].register(serverSocketChannel);
        selectionKey.interestOps(SelectionKey.OP_ACCEPT);
    }

    /**
     * 绑定端口
     * @author Hash
     * @date 2021/6/19 19:36
     */
    private void bind() throws IOException {
        //  1、 正式绑定端口，对外服务
        serverSocketChannel.bind(new InetSocketAddress(8080));
        System.out.println("启动完成，端口8080");
    }

    public static void main(String[] args) throws IOException {
        final NIOServerV3 nioServerV3 = new NIOServerV3();
        nioServerV3.newGroup();
        nioServerV3.initAndRegister();
        nioServerV3.bind();
    }
}
