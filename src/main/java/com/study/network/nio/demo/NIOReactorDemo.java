package com.study.network.nio.demo;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NIO reactor线程模型的示例
 *
 * @author Hash
 * @date 2021年06月19日 17:10
 */
public class NIOReactorDemo {

    public static void main(String[] args) throws IOException {
        final NIOReactorDemo nioReactorDemo = new NIOReactorDemo();
        nioReactorDemo.newGroup();
        nioReactorDemo.initAndRegister();
        nioReactorDemo.bind();
    }

    private ServerSocketChannel serverSocketChannel;
    /**
     * 负责处理IO读和写
     */
    private SubReactor[] subReactors = new SubReactor[16];

    /**
     * 负责接收客户端的连接
     */
    private MainReactor mainReactor;

    /**
     * 处理业务操作的线程
     */
    private ExecutorService workPool = Executors.newCachedThreadPool();

    AtomicInteger incr = new AtomicInteger(0);

    /**
     * 初始化mainReactor和subReactor
     *
     * @author Hash
     * @date 2021/6/19 17:51
     */
    public void newGroup() throws IOException {
        mainReactor = new MainReactor();
        for (int i = 0; i < subReactors.length; i++) {
            subReactors[i] = new SubReactor();
        }
    }

    /**
     * 初始化服务端channel并注册到mainReactor中，并且启动mainReactor
     *
     * @author Hash
     * @date 2021/6/19 17:52
     */
    public void initAndRegister() throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);

        mainReactor.register(serverSocketChannel);
        mainReactor.start();
    }

    public void bind() throws IOException {
        serverSocketChannel.bind(new InetSocketAddress("localhost", 8080));
        System.out.println("启动成功");
    }

    /**
     * MainReactor 负责接收客户端连接
     *
     * @author Hash
     * @date 2021/6/19 17:44
     */
    class MainReactor extends Thread {

        private Selector selector;

        public MainReactor() throws IOException {
            this.selector = Selector.open();
        }

        @Override
        public void run() {
            while (true) {
                try {
                    selector.select();
                    final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    final Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        final SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isAcceptable()) {
                            final SocketChannel socketChannel = ((ServerSocketChannel) (key.channel())).accept();
                            // 将客户端连接给到acceptor
                            new Acceptor(socketChannel);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 将服务端channel注册到selector中，注册OP_ACCEPT事件
         *
         * @param serverSocketChannel
         * @author Hash
         * @date 2021/6/19 17:56
         */
        public void register(ServerSocketChannel serverSocketChannel) throws ClosedChannelException {
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        }
    }

    /**
     * 将客户端的连接分配到一个subReactor线程中，并启动subReactor线程
     *
     * @author Hash
     * @date 2021/6/19 17:46
     */
    class Acceptor {

        public Acceptor(SocketChannel socketChannel) throws IOException {
            socketChannel.configureBlocking(false);
            int index = incr.getAndIncrement() % subReactors.length;
            final SubReactor subReactor = subReactors[index];
            subReactor.register(socketChannel);
            subReactor.start();

            System.out.println("收到连接：" + socketChannel);
        }
    }

    /**
     * 负责从客户端读数据后交给工作线程去处理，和给客户端写数据
     *
     * @author Hash
     * @date 2021/6/19 17:47
     */
    class SubReactor extends Thread {

        private Selector selector;

        private volatile boolean running = false;

        public SubReactor() throws IOException {
            this.selector = Selector.open();
        }

        @Override
        public synchronized void start() {
            if (!running) {
                running = true;
                super.start();
            }
        }

        @Override
        public void run() {
            while (running) {
                try {
                    selector.select();
                    final Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    final Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        final SelectionKey key = iterator.next();
                        iterator.remove();

                        if (key.isReadable()) {
                            SocketChannel socketChannel = null;
                            try {
                                socketChannel = (SocketChannel) key.channel();
                                new Handler(socketChannel);
                            } catch (Exception e) {
                                e.printStackTrace();
                                key.cancel();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * 将客户端channel注册到selector中，注册OP_READ事件
         *
         * @param socketChannel
         * @author Hash
         * @date 2021/6/19 18:12
         */
        public void register(SocketChannel socketChannel) throws ClosedChannelException {
            socketChannel.register(selector, SelectionKey.OP_READ);
        }
    }

    /**
     * @author Hash
     * @date 2021/6/19 17:48
     */
    class Handler {

        public Handler(SocketChannel socketChannel) throws IOException {
            final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
            while (socketChannel.isOpen() && socketChannel.read(byteBuffer) != -1) {
                // 长连接情况下,需要手动判断数据有没有读取结束 (此处做一个简单的判断: 超过0字节就认为请求结束了)
                if (byteBuffer.position() > 0) {
                    break;
                }
            }

            if (byteBuffer.position() == 0) {
                return;
            }
            byteBuffer.flip();
            byte[] content = new byte[byteBuffer.remaining()];
            byteBuffer.get(content);
            System.out.println(new String(content));
            System.out.println("收到数据,来自：" + socketChannel.getRemoteAddress());
            // TODO 业务操作 数据库 接口调用等等
            workPool.execute(() -> {
                // 处理业务
            });

            // 响应结果 200
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Length: 11\r\n\r\n" +
                    "Hello World";
            ByteBuffer buffer = ByteBuffer.wrap(response.getBytes());
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
        }
    }
}
