package com.study.network.netty.million;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author Hash
 * @date 2021年07月11日 21:40
 */
public class Client {
    //服务端的IP
    private static final String SERVER_HOST = "localhost";

    static final int BEGIN_PORT = 11000;
    static final int N_PORT = 100;

    public static void main(String[] args) {
        new Client().start(BEGIN_PORT, N_PORT);
    }

    public void start(final int beginPort, int nPort) {
        System.out.println("客户端启动....");
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast(new PongHandler());
            }
        });

        int index = 0;
        int port;

        String serverHost = System.getProperty("server.host", SERVER_HOST);
        //从10000的端口开始，按端口递增的方式进行连接
        while (!Thread.interrupted()) {
            port = beginPort + index;
            try {
                ChannelFuture channelFuture = bootstrap.connect(serverHost, port);
                channelFuture.addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        System.out.println("连接失败，退出!");
                        System.exit(0);
                    }
                });
                channelFuture.get();
            } catch (Exception e) {
            }

            if (++index == nPort) {
                index = 0;
            }
        }
    }

}
