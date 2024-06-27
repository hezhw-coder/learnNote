package com.he_zhw;

import com.he_zhw.datastructure.ListNode;

public class Leetcode83 {
    public ListNode deleteDuplicates1(ListNode head) {
        ListNode s = new ListNode(-1, head);
        ListNode p1 = s.next;
        while (p1 != null && p1.next != null) {
            if (p1.val == p1.next.val) {
                p1.next = p1.next.next;
            } else {
                p1 = p1.next;
            }
        }
        return s.next;
    }

    public ListNode deleteDuplicates(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }

        ListNode listNode = deleteDuplicates(head.next);
        if (head.val == listNode.val) {
            return listNode;
        } else {
            head.next = listNode;
            return head;
        }
    }

    public static void main(String[] args) {
        Leetcode83 leetcode83 = new Leetcode83();
        ListNode head = ListNode.of(1, 1, 2, 3, 3);
        System.out.println(head);
        ListNode listNode = leetcode83.deleteDuplicates(head);
        //ListNode listNode = leetcode206.removeLast(head);
        System.out.println(listNode);
    }
}
