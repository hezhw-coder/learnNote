package com.he_zhw;

import com.he_zhw.datastructure.ListNode;

/**
 * 根据值删除链表中的值
 */
public class Leetcode203 {

    public ListNode removeElements1(ListNode head, int val) {
        if (head == null) {
            return head;
        }
        ListNode p = new ListNode(666, head);
/*        while (true) {
            if (p == null || p.next == null) {
                break;
            }
            if (p.next.val == val) {
                boolean flag=false;
                if (p.next==head){
                    flag=true;
                }
                p.next = p.next.next;
                if (flag){
                    head=p.next;
                    continue;
                }
            }
            p = p.next;


        }*/
        ListNode p1 = p;
        ListNode p2 = p1.next;
        while (true) {
            if (p2 == null) {
                break;
            }
            if (p2.val == val) {
                p1.next = p2.next;
                p2 = p1.next;
            } else {
                p1 = p1.next;
                p2 = p2.next;
            }
        }
        return p.next;
    }

    /**
     * 递归方式
     *
     * @param head
     * @param val
     * @return
     */
    public ListNode removeElements(ListNode head, int val) {
        if (head == null) {
            return null;
        }
/*        ListNode listNode = removeElements(head.next, val);
        if (listNode.val == val) {
            head.next=listNode.next;

        }
        return head;*/
        if (head.val == val) {
            return removeElements(head.next, val);
        }
        else
        {
            head.next=removeElements(head.next, val);
            return head;
        }

    }

    public ListNode removeElements8(ListNode head){
        if (head==null) {
            return null;
        }
        head.next=removeElements8(head.next);
        return head;
    }

    public static void main(String[] args) {
        Leetcode203 leetcode206 = new Leetcode203();
        ListNode head = ListNode.of(1, 2, 6, 3, 4, 5, 6);
//        ListNode head = ListNode.of(7,7,7,7);
        System.out.println(head);
//        ListNode listNode = leetcode206.removeElements(head, 6);
        ListNode listNode = leetcode206.removeElements8(head);
        System.out.println(listNode);
    }
}
