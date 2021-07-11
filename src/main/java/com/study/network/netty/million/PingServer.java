package com.study.network.netty.million;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * @author Hash
 * @date 2021年07月11日 21:43
 */
public class PingServer {
    static final int BEGIN_PORT = 8080;
    static final int N_PORT = 100;

    public static void main(String[] args) {
        new PingServer().start(BEGIN_PORT, N_PORT);
    }

    public void start(int beginPort, int nPort) {
        System.out.println("启动服务....");

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.handler(new LoggingHandler(LogLevel.INFO));
        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addFirst(new IdleStateHandler(0, 0, 1, TimeUnit.SECONDS));
                pipeline.addLast(new PingHandler());
                //每个连接都有个ConnectionCountHandler对连接记数进行增加
                pipeline.addLast(new ConnectionCountHandler());
            }
        });

        bootstrap.bind(beginPort).addListener((ChannelFutureListener) future -> {
            System.out.println("端口绑定成功: " + beginPort);
        });
        System.out.println("服务已启动!");
    }
}
