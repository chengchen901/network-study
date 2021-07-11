package com.study.network.nio.webroot;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 启动器
 */
public class ServerBootstrap {
    // 主线程，处理客户端的连接
    private ReactorThread mainReactor;
    // 子线程(I/O线程)，处理客户端的请求
    private ReactorThread[] subReactor = new ReactorThread[8];

    // 主线程的事件处理器
    private ChannelHandler handler;
    // 子线程的事件处理器
    private ChannelHandler childHandler;

    /** 处理业务操作的线程 */
    private static ExecutorService workPool = Executors.newCachedThreadPool();

    public ServerBootstrap handler(ChannelHandler handler) {
        this.handler = handler;
        return this;
    }

    public ServerBootstrap childHandler(ChannelHandler handler) {
        this.childHandler = handler;
        return this;
    }

    public ServerBootstrap() throws IOException {
        // 创建默认的处理器，防止空指针异常
        handler = childHandler = new ChannelHandlerAdapter();

        // 初始化子线程组
        for (int i = 0; i < subReactor.length; i++) {
            subReactor[i] = new ReactorThread() {
                @Override
                public void handler(SelectableChannel channel, SelectionKey key) throws Exception {
                    workPool.execute(() -> {
                        // 客户端Channel有数据可读取，调用handler的channelRead
                        try {
                            childHandler.channelRead(channel);
                        } catch (Exception e) {
                            key.cancel();
                        }
                    });
                }
            };
        }

        // 初始化主线程
        this.mainReactor = new ReactorThread() {
            AtomicInteger incr = new AtomicInteger(0);

            @Override
            public void handler(SelectableChannel channel, SelectionKey key) throws Exception {
                // 有客户端连接

                // 只做请求分发，不做具体的数据读取
                ServerSocketChannel ch = (ServerSocketChannel) channel;
                SocketChannel socketChannel = ch.accept();
                socketChannel.configureBlocking(false);

                // 收到连接建立的通知之后，分发给工作线程
                int index = incr.getAndIncrement() % subReactor.length;
                ReactorThread workEventLoop = subReactor[index];
                workEventLoop.doStart();

                // 将客户端Channel注册到工作线程的selector
                SelectionKey selectionKey = workEventLoop.register(socketChannel);
                childHandler.channelRegistered(socketChannel);
                childHandler.channelActive(socketChannel);
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
        };
    }

    /**
     * 绑定端口
     * @param port
     * @throws Exception
     */
    public void bind(int port) throws Exception {
        doBind(new InetSocketAddress(port));
    }

    /**
     * 初始化+绑定端口
     * @param inetSocketAddress
     * @throws Exception
     */
    private void doBind(InetSocketAddress inetSocketAddress) throws Exception {
        ServerSocketChannel channel = initAndRegister();
        channel.bind(inetSocketAddress);
    }

    /**
     * 初始化并将Channel注册到Selector
     * @return
     * @throws Exception
     */
    private ServerSocketChannel initAndRegister() throws Exception {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);

        // 启动主线程
        mainReactor.doStart();

        SelectionKey selectionKey = mainReactor.register(channel);
        handler.channelRegistered(channel);
        selectionKey.interestOps(SelectionKey.OP_ACCEPT);
        return channel;
    }
}
