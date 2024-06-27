package com.he_zhw.datastructure.queue;


import com.he_zhw.datastructure.QueueListNode;

import java.util.Iterator;

public class LinkedListQueue<E> implements Queue<E>, Iterable<E> {

    private QueueListNode<E> head = new QueueListNode(null, null);
    private QueueListNode<E> tail = head;

    public LinkedListQueue() {

        this.tail.next = head;
    }

    /**
     * 向队列尾部添加
     *
     * @param value
     * @return
     */
    @Override
    public boolean offer(E value) {
        QueueListNode<E> eQueueListNode = new QueueListNode<>(value, head);
        this.tail.next=eQueueListNode;
        this.tail=eQueueListNode;
        return true;
    }

    /**
     * 返回头部元素,并移除
     *
     * @return
     */
    @Override
    public E poll() {
        if (isEmpty()) {
            return null;
        }
        QueueListNode<E> first = head.next;
        head.next=first.next;
        if (first==tail){
            tail=head;
        }
        return first.val;
    }

    /**
     * 返回头部元素
     *
     * @return
     */
    @Override
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return head.next.val;
    }

    /**
     * 判断队列是否为空
     *
     * @return
     */
    @Override
    public boolean isEmpty() {
        return tail == head;
    }

    /**
     * 判断队列是否已满
     *
     * @return
     */
    @Override
    public boolean isFull() {
        return false;
    }


    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            QueueListNode<E> next = head.next;
            @Override
            public boolean hasNext() {
                return next!=head;
            }

            @Override
            public E next() {
                E val = next.val;
                next=next.next;
                return val;
            }
        };
    }
}
