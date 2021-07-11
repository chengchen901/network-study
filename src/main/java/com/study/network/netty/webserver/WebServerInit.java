package com.study.network.netty.webserver;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;

/**
 * 客户端连接后初始化
 */
public class WebServerInit extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // 添加Http编码解码器
        pipeline.addLast(new HttpServerCodec());
        // 添加请求分发处理器
        pipeline.addLast(new DispatcherHandler());
        // 添加静态资源处理器
        pipeline.addLast(new StaticFileHandler());
        // 添加Action处理器
        pipeline.addLast(new ActionHandler());
    }
}
