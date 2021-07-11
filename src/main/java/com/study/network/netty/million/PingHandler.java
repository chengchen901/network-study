package com.study.network.netty.million;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleUserEventChannelHandler;
import io.netty.handler.timeout.IdleStateEvent;

import static io.netty.handler.timeout.IdleState.ALL_IDLE;

/**
 * @author Hash
 * @date 2021年07月11日 21:41
 */
public class PingHandler extends SimpleUserEventChannelHandler<IdleStateEvent> {
    private static final ByteBuf PING_BUF = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("ping".getBytes()));

    private int count;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        String str = new String(data);
        if ("pong".equals(str)) {
            System.out.println(ctx + " ---- " + str);
            count--;
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    protected void eventReceived(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        if (evt.state() == ALL_IDLE) {
            if (count >= 3) {
                System.out.println("检测到客户端连接无响应，断开连接：" + ctx.channel());
                ctx.close();
                return;
            }

            count++;
            System.out.println(ctx.channel() + " ---- ping");
            ctx.writeAndFlush(PING_BUF.duplicate());
        }
        ctx.fireUserEventTriggered(evt);
    }
}
