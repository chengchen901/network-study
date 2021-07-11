package com.study.network.netty.webserver;

import com.study.network.nio.webroot.ReactorThread;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 处理请求分发
 */
public class DispatcherHandler extends SimpleChannelInboundHandler<HttpObject> {

    // 静态资源根目录
    private static final Path BASE_PATH;

    static {
        Path path;
        try {
            path = Paths.get(ReactorThread.class.getResource("/webroot").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            path = Paths.get(ReactorThread.class.getResource("/webroot").getFile());
        }
        BASE_PATH = path;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            String url = request.uri();
            // url是/就转到/index.html
            if ("/".equals(url)) {
                url = "/index.html";
            }

            Path path = Paths.get(BASE_PATH.toString(), new QueryStringDecoder(url).rawPath());
            if (Files.exists(path)) {// 静态资源存在就读静态资源
                ctx.fireChannelRead(path.toFile());
            } else {// 把请求地址当action处理
                ctx.fireChannelRead(request);
            }
        }
    }
}
