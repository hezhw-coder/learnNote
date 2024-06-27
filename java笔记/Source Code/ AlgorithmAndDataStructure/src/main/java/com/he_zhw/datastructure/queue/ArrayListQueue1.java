package com.he_zhw.datastructure.queue;

import java.util.Iterator;

public class ArrayListQueue1<E> implements Queue<E>, Iterable<E> {
    private E[] array = null;
    private int capacity = Integer.MAX_VALUE;
    private int head = 0;
    private int tail = 0;

    @SuppressWarnings("All")
    public ArrayListQueue1(int capacity) {
        array = (E[]) new Object[capacity + 1];
    }

    @Override
    public boolean offer(E value) {
        if (isFull()) {
            return false;
        }
        array[tail] = value;
        tail = (tail + 1) % array.length;
        return true;
    }

    @Override
    public E poll() {
        if (isEmpty()) {
            return null;
        }
        E e = array[head];
        head = (head + 1) % array.length;
        return e;
    }

    @Override
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return array[head];
    }

    @Override
    public boolean isEmpty() {
        return head == tail;
    }

    @Override
    public boolean isFull() {
        return (tail+1)%array.length==head;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int i=head;
            @Override
            public boolean hasNext() {
                return i!=tail;
            }

            @Override
            public E next() {
                E e = array[i];
                i++;
                return e;
            }
        };
    }
}
