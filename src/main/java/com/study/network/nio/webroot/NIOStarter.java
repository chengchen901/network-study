package com.study.network.nio.webroot;

import java.io.IOException;
import java.nio.channels.SelectableChannel;

/**
 * 启动类
 */
public class NIOStarter {
    public static void main(String[] args) {
        try {
            // 创建启动器
            ServerBootstrap bootstrap = new ServerBootstrap();
            // 配置启动器
            bootstrap.handler(new ChannelHandlerAdapter() {
                @Override
                public void channelRegistered(SelectableChannel channel) throws Exception {
                    System.out.println("服务已注册:" + channel);
                }
            }).childHandler(new RequestHandler());

            // 绑定端口
            bootstrap.bind(8080);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
