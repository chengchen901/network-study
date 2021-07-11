package com.study.network.nio.webroot;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * Reactor线程
 */
public abstract class ReactorThread extends Thread {

    Selector selector;

    /**
     * Selector监听到有事件后,调用这个方法
     */
    public abstract void handler(SelectableChannel channel, SelectionKey key) throws Exception;

    ReactorThread() throws IOException {
        selector = Selector.open();
    }

    volatile boolean running = false;

    @Override
    public void run() {
        // 轮询Selector事件
        while (running) {
            try {
                selector.select(1000);

                // 获取查询结果
                Set<SelectionKey> selected = selector.selectedKeys();
                // 遍历查询结果
                Iterator<SelectionKey> iter = selected.iterator();
                while (iter.hasNext()) {
                    // 被封装的查询结果
                    SelectionKey key = iter.next();
                    iter.remove();
                    int readyOps = key.readyOps();
                    // 关注 Read 和 Accept两个事件
                    if ((readyOps & (SelectionKey.OP_READ | SelectionKey.OP_ACCEPT)) != 0 || readyOps == 0) {
                        try {
                            SelectableChannel channel = (SelectableChannel) key.attachment();
                            channel.configureBlocking(false);
                            handler(channel, key);
                            if (!channel.isOpen()) {
                                key.cancel(); // 如果关闭了,就取消这个KEY的订阅
                            }
                        } catch (Exception ex) {
                            key.cancel(); // 如果有异常,就取消这个KEY的订阅
                        }
                    }
                }
                selector.selectNow();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    SelectionKey register(SelectableChannel channel) throws Exception {
        return channel.register(selector, 0, channel);
    }

    /**
     * 线程未启动就启动线程
     */
    void doStart() {
        if (!running) {
            running = true;
            start();
        }
    }

}
