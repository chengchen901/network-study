package com.study.network.netty.million;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author Hash
 * @date 2021年07月11日 21:44
 */
public class PongHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private static final ByteBuf PONG_BUF = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("pong".getBytes()));

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        String str = new String(data);
        if ("ping".equals(str)) {
            ctx.writeAndFlush(PONG_BUF.duplicate());
        }
    }
}
