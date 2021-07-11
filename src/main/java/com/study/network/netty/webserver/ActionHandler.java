package com.study.network.netty.webserver;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Action处理器，处理所有的action请求，只处理指定的action，其它的都返回404
 */
public class ActionHandler extends SimpleChannelInboundHandler<HttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
        // 获取不带参数的url
        String url = new QueryStringDecoder(request.uri()).rawPath();
        if ("/login".equals(url)) {
            String result = doLogin(request);

            // 将结果封装到ByteBuf
            ByteBuf buf = Unpooled.wrappedBuffer(result.getBytes());

            // 创建响应对象
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, buf);
            // 设置响应内容长度
            HttpUtil.setContentLength(response, buf.writerIndex());
            // 设置响应的ContentTYpe
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

            // 将响应写给客户端
            ctx.writeAndFlush(response);
        } else {
            sendError(ctx, NOT_FOUND);
        }
    }

    /**
     * 登陆
     * @param request
     * @return
     */
    private String doLogin(HttpRequest request) {
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
        Map<String, List<String>> params = queryStringDecoder.parameters();
        String userName = params.get("userName").get(0);
        String password = params.get("password").get(0);

        if ("admin".equals(userName) && "111111".equals(password)) {
            return "登陆成功！";
        }
        return "用户名或秘密错误！";
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
}
