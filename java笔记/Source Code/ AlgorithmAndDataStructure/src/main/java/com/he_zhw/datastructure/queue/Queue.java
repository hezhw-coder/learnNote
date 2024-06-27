package com.he_zhw.datastructure.queue;

public interface Queue<E> {
    /**
     * 向队列尾部添加元素
     * @param value
     * @return
     */
    boolean offer(E value);

    /**
     * 返回头部元素,并移除
     * @return
     */
    E poll();

    /**
     * 返回头部元素
     * @return
     */
    E peek();

    /**
     * 判断队列是否为空
     * @return
     */
    boolean isEmpty();

    /**
     * 判断队列是否已满
     * @return
     */
    boolean isFull();
}
