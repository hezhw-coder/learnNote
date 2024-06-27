package com.he_zhw.datastructure.queue;

import org.junit.Test;

public class ArrayListQueueTest {
    @Test
    public void test1() {
        ArrayListQueue1<Integer> integers = new ArrayListQueue1<Integer>(6);
        integers.offer(1);
        integers.offer(2);
        integers.offer(3);
        integers.offer(4);
        integers.offer(5);
        integers.offer(6);
//        Integer peek = integers.peek();
//        System.out.println(peek);
//        integers.poll();
//        integers.poll();
//        System.out.println(poll);
        for (Integer obj : integers) {
            System.out.print(" "+obj);
        }
    }
}