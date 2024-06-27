package com.he_zhw.datastructure;

public class QueueListNode<E> {
    public E val;
    public QueueListNode<E> next;

    public QueueListNode(E val, QueueListNode<E> next) {
        this.val = val;
        this.next = next;
    }
}
