package com.study.network.netty.pipeline;

/**
 * 链表形式调用，netty就是类似的这种形式
 *
 * @author Hash
 * @date 2021年07月03日 10:18
 */
public class PipelineDemo {

    public static void main(String[] args) {
        final PipelineDemo pipelineDemo = new PipelineDemo();
        pipelineDemo.addLast(new Handler1());
        pipelineDemo.addLast(new Handler2());
        pipelineDemo.addLast(new Handler2());
        pipelineDemo.addLast(new Handler1());

        pipelineDemo.requestProcess("火车呜呜呜~~");
    }

    /**
     * 初始化的时候造一个head，作为责任链的开始，但是并没有具体的处理
     */
    public HandlerChainContext head = new HandlerChainContext(new AbstractHandler() {
        @Override
        void doHandler(HandlerChainContext handlerChainContext, Object arg0) {
            handlerChainContext.runNext(arg0);
        }
    });

    public void requestProcess(Object arg0) {
        this.head.handler(arg0);
    }

    public void addLast(AbstractHandler handler) {
        HandlerChainContext context = head;
        while (context.next != null) {
            context = context.next;
        }
        context.next = new HandlerChainContext(handler);
    }
}

/**
 * handler上下文，主要负责维护链和链的执行
 */
class HandlerChainContext {

    /**
     * 下一个节点
     */
    HandlerChainContext next;

    /**
     * 当前节点处理器
     */
    AbstractHandler handler;

    public HandlerChainContext(AbstractHandler handler) {
        this.handler = handler;
    }

    void handler(Object arg0) {
        this.handler.doHandler(this, arg0);
    }

    void runNext(Object arg0) {
        if (this.next != null) {
            next.handler(arg0);
        }
    }
}

/**
 * 处理器抽象类
 */
abstract class AbstractHandler {

    /**
     * 处理器，用于实现具体的处理逻辑和是否处理下一个handler
     *
     * @param handlerChainContext
     * @param arg0
     * @author Hash
     * @date 2021/7/3 10:22
     */
    abstract void doHandler(HandlerChainContext handlerChainContext, Object arg0);
}

/**
 * 处理器具体实现类
 */
class Handler1 extends AbstractHandler {

    @Override
    void doHandler(HandlerChainContext handlerChainContext, Object arg0) {
        arg0 = arg0.toString() + "..handler1的小尾巴.....";
        System.out.println("我是Handler1的实例，我在处理：" + arg0);
        // 继续执行下一个
        handlerChainContext.runNext(arg0);
    }
}

/**
 * 处理器具体实现类
 */
class Handler2 extends AbstractHandler {

    @Override
    void doHandler(HandlerChainContext handlerChainContext, Object arg0) {
        arg0 = arg0.toString() + "..handler2的小尾巴.....";
        System.out.println("我是Handler2的实例，我在处理：" + arg0);
        // 继续执行下一个
        handlerChainContext.runNext(arg0);
    }
}