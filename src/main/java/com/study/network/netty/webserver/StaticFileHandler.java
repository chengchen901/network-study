package com.study.network.netty.webserver;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * 处理静态资源的请求
 */
public class StaticFileHandler extends SimpleChannelInboundHandler<File> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, File file) throws Exception {

        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException ignore) {
            sendError(ctx, NOT_FOUND);
            return;
        }
        long fileLength = raf.length();

        // 创建一个响应对象
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        // 设置响应内容的长度
        HttpUtil.setContentLength(response, fileLength);
        // 设置响应的ContentType
        setContentTypeHeader(response, file);

        // 写入响应头部信息
        ctx.write(response);
        // 将文件内容写到客户端
        ctx.write(new DefaultFileRegion(raf.getChannel(), 0, fileLength), ctx.newProgressivePromise());
        // 结束给客户端写入
        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }

    /**
     * 根据文件后缀名设置ContentType
     *
     * @param response
     * @param file
     */
    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        if (file.getPath().endsWith(".css")) {
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/css");
        } else {
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
        }
    }

    /**
     * 发送出错的结果
     *
     * @param ctx
     * @param status
     */
    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        sendAndCleanupConnection(ctx, response, false);
    }

    /**
     * 发送响应结果
     *
     * @param ctx
     * @param response
     * @param keepAlive
     */
    private static void sendAndCleanupConnection(ChannelHandlerContext ctx, FullHttpResponse response, boolean keepAlive) {
        HttpUtil.setContentLength(response, response.content().readableBytes());
        ChannelFuture flushPromise = ctx.writeAndFlush(response);
    }

    /**
     * 异常处理
     *
     * @param ctx
     * @param cause
     */
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }
}
