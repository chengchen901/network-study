package com.study.network.netty.buf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

/**
 * @author Hash
 * @date 2021年07月04日 21:00
 */
public class ZeroCopyDemo {
    public static void main(String[] args) {
//        wrapTest();
//        sliceTest();
        compositeTest();
    }

    public static void wrapTest() {
        // 将数组包裹在buf中，保存数组的引用
        byte[] arr = {1, 2, 3, 4, 5};
        ByteBuf byteBuf = Unpooled.wrappedBuffer(arr);
        System.out.println(byteBuf.getByte(4));
        arr[4] = 6;
        System.out.println(byteBuf.getByte(4));
    }

    public static void sliceTest() {
        // 拆分buf
        ByteBuf buffer1 = Unpooled.wrappedBuffer("hello".getBytes());
        ByteBuf newBuffer = buffer1.slice(1, 2);// 返回的buf引用了buffer1的内容
        newBuffer.unwrap();// 新buf中原buf的引用
        System.out.println(newBuffer.toString());
    }

    public static void compositeTest() {
        // 将多个buf合并为一个
        ByteBuf buffer1 = Unpooled.buffer(3);
        buffer1.writeByte(1);
        buffer1.writeByte(2);
        ByteBuf buffer2 = Unpooled.buffer(3);
        buffer2.writeByte(4);
        CompositeByteBuf compositeByteBuf = Unpooled.compositeBuffer();
        CompositeByteBuf newBuffer = compositeByteBuf.addComponents(true, buffer1, buffer2);
        System.out.println(newBuffer);
    }
}
