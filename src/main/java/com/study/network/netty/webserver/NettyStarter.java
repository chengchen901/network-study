package com.study.network.netty.webserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * 启动类
 */
public class NettyStarter {
    public static void main(String[] args) {
        // 老板线程组，处理客户端连接
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        // 工人线程组，处理客户端的请求
        NioEventLoopGroup workGroup = new NioEventLoopGroup();

        // 创建启动器，并配置
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workGroup)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new WebServerInit());

        try {
            // 绑定端口
            Channel channel = bootstrap.bind(8080).sync().channel();
            channel.closeFuture().addListeners(future -> {
                bossGroup.shutdownGracefully();
                workGroup.shutdownGracefully();
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
