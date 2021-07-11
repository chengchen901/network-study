package com.study.network.netty.buf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Hash
 * @date 2021年07月04日 14:45
 */
public class DirectByteBufDemo {
    public static void main(String[] args) {
//        directByteBufTest();
        poolTest();
    }

    public static void directByteBufTest() {
        //  +-------------------+------------------+------------------+
        //  | discardable bytes |  readable bytes  |  writable bytes  |
        //  |                   |     (CONTENT)    |                  |
        //  +-------------------+------------------+------------------+
        //  |                   |                  |                  |
        //  0      <=       readerIndex   <=   writerIndex    <=    capacity

        // 1.创建一个非池化的ByteBuf，大小为10个字节
        ByteBuf buf = Unpooled.directBuffer(10);
        System.out.println("原始ByteBuf为====================>" + buf.toString());
        // System.out.println("1.ByteBuf中的内容为===============>" + Arrays.toString(buf.array()) + "\n");

        // 2.写入一段内容
        byte[] bytes = {1, 2, 3, 4, 5};
        buf.writeBytes(bytes);
        System.out.println("写入的bytes为====================>" + Arrays.toString(bytes));
        System.out.println("写入一段内容后ByteBuf为===========>" + buf.toString());
        //System.out.println("2.ByteBuf中的内容为===============>" + Arrays.toString(buf.array()) + "\n");

        // 3.读取一段内容
        byte b1 = buf.readByte();
        byte b2 = buf.readByte();
        System.out.println("读取的bytes为====================>" + Arrays.toString(new byte[]{b1, b2}));
        System.out.println("读取一段内容后ByteBuf为===========>" + buf.toString());
        //System.out.println("3.ByteBuf中的内容为===============>" + Arrays.toString(buf.array()) + "\n");

        // 4.将读取的内容丢弃
        buf.discardReadBytes();
        System.out.println("将读取的内容丢弃后ByteBuf为========>" + buf.toString());
        //System.out.println("4.ByteBuf中的内容为===============>" + Arrays.toString(buf.array()) + "\n");

        // 5.清空读写指针
        buf.clear();
        System.out.println("将读写指针清空后ByteBuf为==========>" + buf.toString());
        //System.out.println("5.ByteBuf中的内容为===============>" + Arrays.toString(buf.array()) + "\n");

        // 6.再次写入一段内容，比第一段内容少
        byte[] bytes2 = {1, 2, 3};
        buf.writeBytes(bytes2);
        System.out.println("写入的bytes为====================>" + Arrays.toString(bytes2));
        System.out.println("写入一段内容后ByteBuf为===========>" + buf.toString());
        // System.out.println("6.ByteBuf中的内容为===============>" + Arrays.toString(buf.array()) + "\n");

        // 7.将ByteBuf清零
        buf.setZero(0, buf.capacity());
        System.out.println("将内容清零后ByteBuf为==============>" + buf.toString());
        // System.out.println("7.ByteBuf中的内容为================>" + Arrays.toString(buf.array()) + "\n");

        // 8.再次写入一段超过容量的内容
        byte[] bytes3 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        buf.writeBytes(bytes3);
        System.out.println("写入的bytes为====================>" + Arrays.toString(bytes3));
        System.out.println("写入一段内容后ByteBuf为===========>" + buf.toString());
        System.out.println("8.ByteBuf中的内容为===============>" + Arrays.toString(buf.array()) + "\n");
        //  随机访问索引 getByte
        //  顺序读 read*
        //  顺序写 write*
        //  清除已读内容 discardReadBytes
        //  清除缓冲区 clear
        //  搜索操作
        //  标记和重置
        //  完整代码示例：参考
        // 搜索操作 读取指定位置 buf.getByte(1);
        //
    }

    public static void poolTest() {

        System.out.println("测试buf回收============================================");
        ByteBufAllocator allocator = ByteBufAllocator.DEFAULT;
        // tiny
        ByteBuf buf1 = allocator.directBuffer(495);// 分配的内存最大长度为496
        System.out.printf("buf1: 0x%X%n", buf1.memoryAddress());
        buf1.release();// 此时会被回收到tiny的512b格子中

        ByteBuf buf2 = allocator.directBuffer(495);// 从tiny的512b格子去取
        System.out.printf("buf2: 0x%X%n", buf2.memoryAddress());
        buf2.release();

        // small
        ByteBuf buf3 = allocator.directBuffer(497);// 分配的内存最大长度为512
        System.out.printf("buf3: 0x%X%n", buf3.memoryAddress());
        buf3.release();// 此时会被回收到small的512b格子中

        ByteBuf buf4 = allocator.directBuffer(497);// 从small的512b格子中去取
        System.out.printf("buf4: 0x%X%n", buf4.memoryAddress());
        for (int i = 0; i < 1000; i++) {//写入1000字节数据后，会动态扩容到1024
            buf4.writeByte(i);
        }
        buf4.release();// 此时回收会到small的1kb格子

        ByteBuf buf5 = allocator.directBuffer(1024 + 1);// 分配的内存最大长度为2048
        System.out.printf("buf5: 0x%X%n", buf5.memoryAddress());
        buf5.release();// 此时回收会到small的2kb格子

        ByteBuf buf6 = allocator.directBuffer(1024 + 1);// 从small的2kb格子中去取
        System.out.printf("buf6: 0x%X%n", buf6.memoryAddress());
        buf6.release();

        // normal
        ByteBuf buf7 = allocator.directBuffer(4 * 1024 + 1);// 分配的内存最大长度为8KB
        System.out.printf("buf7: 0x%X%n", buf7.memoryAddress());
        buf7.release();// 此时会回收到normal的8KB格子

        ByteBuf buf8 = allocator.directBuffer(8 * 1024 + 1);// 分配的内存最大长度为16KB
        System.out.printf("buf8: 0x%X%n", buf8.memoryAddress());
        buf8.release();// 此时会回收到normal的16KB格子

        ByteBuf buf9 = allocator.directBuffer(16 * 1024 + 1);// 分配的内存最大长度为32KB
        System.out.printf("buf9: 0x%X%n", buf9.memoryAddress());
        buf9.release();// 此时会回收到normal的32KB格子

        ByteBuf buf10 = allocator.directBuffer(32 * 1024 + 1);// 分配的内存最大长度为64KB
        System.out.printf("buf10: 0x%X%n", buf10.memoryAddress());
        buf10.release();// 此时会直接释放内存

        System.out.println("测试buf回收区域容量============================================");
        List<ByteBuf> bufs = new ArrayList<>();
        for (int i = 0; i < 600; i++) {
            bufs.add(allocator.directBuffer(4));
        }
        for (int i = 0; i < bufs.size(); i++) {
            bufs.get(i).release();
        }

        ByteBuf buf11 = allocator.directBuffer(10);
        ByteBuf sliceBuf = buf11.retainedSlice(3, 1);
        sliceBuf.writerIndex(0);
        sliceBuf.writeByte(2);
        System.out.printf("sliceBuf: %s%n", sliceBuf);
        System.out.printf("buf11: %s%n", buf11);
        System.out.println(buf11.getByte(3));
        buf11.release();
        sliceBuf.release();
    }
}
