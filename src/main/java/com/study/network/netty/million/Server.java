package com.study.network.netty.million;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * @author Hash
 * @date 2021年07月11日 21:45
 */
public class Server {
    static final int BEGIN_PORT = 11000;
    static final int N_PORT = 100;

    public static void main(String[] args) {
        new Server().start(BEGIN_PORT, N_PORT);
    }

    public void start(int beginPort, int nPort) {
        System.out.println("启动服务....");

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);

        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline pipeline = ch.pipeline();
                //每个连接都有个ConnectionCountHandler对连接记数进行增加
                pipeline.addLast(new ConnectionCountHandler());
            }
        });

        //这里开启 10000到100099这100个端口
        for (int i = 0; i < nPort; i++) {
            int port = beginPort + i;
            bootstrap.bind(port).addListener((ChannelFutureListener) future -> {
                System.out.println("端口绑定成功: " + port);
            });
        }
        System.out.println("服务已启动!");
    }
}
