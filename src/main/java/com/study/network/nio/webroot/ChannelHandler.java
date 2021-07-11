package com.study.network.nio.webroot;

import java.nio.channels.SelectableChannel;

/**
 * 事件处理器
 */
public interface ChannelHandler {
    /**
     * Channel注册到selector后被调用
     * @param channel
     * @throws Exception
     */
    void channelRegistered(SelectableChannel channel) throws Exception;

    /**
     * Channel激活后被调用
     * @param channel
     * @throws Exception
     */
    void channelActive(SelectableChannel channel) throws Exception;

    /**
     * 有数据可读时被调用
     * @param channel
     * @throws Exception
     */
    void channelRead(SelectableChannel channel) throws Exception;
}
