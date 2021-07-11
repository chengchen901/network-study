package com.study.network.nio.webroot;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * 请求处理器
 */
public class RequestHandler extends ChannelHandlerAdapter {

    // webroot根目录
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
    public void channelActive(SelectableChannel channel) throws Exception {
        System.out.println(Thread.currentThread().getName() + "收到新连接 : " + ((SocketChannel) channel).getRemoteAddress());
    }

    @Override
    public void channelRead(SelectableChannel channel) throws Exception {
        SocketChannel socketChannel = (SocketChannel) channel;
        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        while (socketChannel.isOpen() && socketChannel.read(requestBuffer) != -1) {
            // 长连接情况下,需要手动判断数据有没有读取结束 (此处做一个简单的判断: 超过0字节就认为请求结束了)
            if (requestBuffer.position() > 0) {
                break;
            }
        }
        if (requestBuffer.position() == 0) {
            return; // 如果没数据了, 则不继续后面的处理
        }
        requestBuffer.flip();
        byte[] content = new byte[requestBuffer.limit()];
        requestBuffer.get(content);
//        System.out.println(new String(content));
        System.out.println(Thread.currentThread().getName() + "收到数据,来自：" + socketChannel.getRemoteAddress());
        HttpRequest httpRequest = HttpUtil.resolver(content);


        dispatch(httpRequest, socketChannel);
    }

    /**
     * 请求分配
     *
     * @param request
     * @param channel
     * @throws IOException
     */
    private void dispatch(HttpRequest request, SocketChannel channel) throws IOException {
        // 从请求中获取Url
        String url = request.getUrl();
        if ("/".equals(url)) {
            url = "/index.html";
        }

        Path path = Paths.get(BASE_PATH.toString(), url);
        if (Files.exists(path)) {
            // 文件存在就去请求文件
            dispatchToFile(channel, path);
        } else {
            try {
                // 请求action
                dispatchToAction(request, channel);
            } catch (FileNotFoundException e) {
                // action不存在就返回404错误
                String result = e.getMessage();

                response(channel, 404, result);
            }
        }
    }

    /**
     * 处理action请求
     *
     * @param request
     * @param channel
     * @throws IOException
     */
    private void dispatchToAction(HttpRequest request, SocketChannel channel) throws IOException {
        String result;
        if ("/login".equals(request.getUrl())) {
            result = doLogin(request);
        } else {
            throw new FileNotFoundException("访问的地址不存在！");
        }

        response(channel, 200, result);
    }

    /**
     * 响应请求
     *
     * @param channel
     * @param code
     * @param data
     * @throws IOException
     */
    private void response(SocketChannel channel, int code, String data) throws IOException {
        // 响应内容写入ByteBuffer
        ByteBuffer body = ByteBuffer.wrap(data.getBytes("GBK"));
        ByteBuffer head = ByteBuffer.wrap(HttpUtil.createResponseHead(code, body.capacity()).getBytes());

        while (head.hasRemaining()) {
            channel.write(head);
        }

        while (body.hasRemaining()) {
            channel.write(body);
        }
    }

    /**
     * 处理登陆业务
     *
     * @param request
     * @return
     */
    private String doLogin(HttpRequest request) {
        Map<String, String> params = request.getParams();
        String userName = params.get("userName");
        String password = params.get("password");
        if ("admin".equals(userName) && "111111".equals(password)) {
            return "登陆成功！";
        }
        return "用户名或秘密错误！";
    }

    /**
     * 处理文件请求
     *
     * @param channel
     * @param path
     */
    private void dispatchToFile(SocketChannel channel, Path path) {
        FileChannel fileChannel = null;
        try {
            // 打开一个只读的文件通道
            fileChannel = FileChannel.open(path, StandardOpenOption.READ);

            // 根据文件长度创建响应头信息
            String headStr = HttpUtil.createResponseHead(200, fileChannel.size());
            // 将响应头写到ByteBuffer
            ByteBuffer headBuf = ByteBuffer.wrap(headStr.getBytes());

            // 写响应头
            while (headBuf.hasRemaining()) {
                channel.write(headBuf);
            }

            // 将文件内容传输到客户端channel
            fileChannel.transferTo(0, fileChannel.size(), channel);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileChannel != null) {
                try {// 关闭文件通道
                    fileChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
