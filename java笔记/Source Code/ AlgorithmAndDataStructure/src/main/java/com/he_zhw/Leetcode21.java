package com.he_zhw;

import com.he_zhw.datastructure.ListNode;

public class Leetcode21 {
    public ListNode mergeTwoLists1(ListNode list1, ListNode list2) {
        ListNode p1 = list1;
        ListNode p2 = list2;
        ListNode s = new ListNode(-1, null);
        ListNode p3 = s;
        while (p1 != null && p2 != null) {
            if (p1.val < p2.val) {
                p3.next = new ListNode(p1.val, null);
                p1 = p1.next;
            } else {
                p3.next = new ListNode(p2.val, null);
                p2 = p2.next;
            }
            p3 = p3.next;
        }
        if (p1 == null) {
            p3.next = p2;
        }
        if (p2 == null) {
            p3.next = p1;
        }
        return s.next;
    }

    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {

        if (list1 == null) {
            return list2;
        }
        if (list2 == null) {
            return list1;
        }
        if (list1.val < list2.val) {
            list1.next = mergeTwoLists(list1.next, list2);
            return list1;
        } else {
            list2.next = mergeTwoLists(list1, list2.next);
            return list2;
        }
    }


    public static void main(String[] args) {
        Leetcode21 leetcode21 = new Leetcode21();
        ListNode head1 = ListNode.of(1, 2, 4);
        ListNode head2 = ListNode.of(1, 3, 4);
//                ListNode head1 = null;
//        ListNode head2 = ListNode.of(0);
        System.out.println(head1);
        System.out.println(head2);
        ListNode listNode = leetcode21.mergeTwoLists(head1, head2);
        //ListNode listNode = leetcode206.removeLast(head);
        System.out.println(listNode);
    }
}
