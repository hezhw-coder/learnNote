package com.he_zhw.datastructure.queue;

import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class LinkedListQueueTest {
    @Test
    public void Test1() {
        LinkedListQueue<Integer> integers = new LinkedListQueue<>();
        integers.offer(1);
        integers.offer(2);
        integers.offer(3);
        integers.offer(4);
        Integer peek = integers.peek();
        System.out.println(peek);
        Integer poll = integers.poll();
        System.out.println(poll);
        for (Integer obj : integers) {
            System.out.println(obj);
        }
    }
}
