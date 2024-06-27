package com.he_zhw;

import com.he_zhw.datastructure.ListNode;

public class Leetcode141 {
    public boolean hasCycle(ListNode head) {
        ListNode p1 = head;
        ListNode p2 = head;
        while (p2 != null && p2.next != null) {

            p1 = p1.next;
            p2 = p2.next.next;
            if (p1 == p2) {
                return true;
            }

        }
        return false;
    }

    public static void main(String[] args) {
        Leetcode141 leetcode141 = new Leetcode141();
        ListNode head = new ListNode(-4, null);
        ListNode head1 = new ListNode(0, head);
        ListNode head2 = new ListNode(2, head1);
        ListNode head3 = new ListNode(3, head2);
//        head.next = head1;
//        System.out.println(head);
        boolean listNode = leetcode141.hasCycle(head3);
        //ListNode listNode = leetcode206.removeLast(head);
        System.out.println(listNode);
    }
}
