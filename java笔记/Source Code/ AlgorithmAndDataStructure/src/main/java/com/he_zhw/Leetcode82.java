package com.he_zhw;

import com.he_zhw.datastructure.ListNode;

public class Leetcode82 {
    public ListNode deleteDuplicates1(ListNode head) {
        ListNode s = new ListNode(-1, head);
        ListNode p1 = s;
        ListNode p2 = p1.next;
        ListNode p3 = p2.next;
        while (p2 != null && p3 != null) {
            if (p2.val == p3.val) {
                while ((p3 = p3.next) != null && p2.val == p3.val) {

                }
                p1.next = p3;
            } else {
                p1 = p1.next;
                p2 = p2.next;
                p3 = p3.next;
            }
        }
        return s.next;
    }

    public ListNode deleteDuplicates(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        if (head.val == head.next.val) {
            ListNode x = head.next.next;
            while (x != null && head.val == x.val) {
                x=x.next;
            }
            return deleteDuplicates(x);
        } else {
            head.next = deleteDuplicates(head.next);
        }

        return head;

    }

    public static void main(String[] args) {
        Leetcode82 leetcode82 = new Leetcode82();
        ListNode head = ListNode.of(1, 1, 1, 2, 3);
        System.out.println(head);
        ListNode listNode = leetcode82.deleteDuplicates(head);
        //ListNode listNode = leetcode206.removeLast(head);
        System.out.println(listNode);
    }
}
