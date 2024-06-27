package com.he_zhw;

import com.he_zhw.datastructure.ListNode;

public class Leetcode206 {


    public ListNode reverseList1(ListNode head) {

        ListNode n1 = null;
        ListNode p = head;
        while (p != null) {
            n1 = new ListNode(p.val, n1);
            p = p.next;
        }

        return n1;
    }

    public ListNode reverseList2(ListNode head) {
        if (head==null||head.next == null) {
            return head;
        }
        ListNode listNode = reverseList(head.next);
        head.next.next=head;
        head.next = null;
        return listNode;
    }

    public ListNode reverseList(ListNode head){

        if (head==null||head.next == null) {
            return head;
        }
        ListNode listNode = reverseList(head.next);
        head.next.next=head;
        head.next = null;
        return listNode;
    }


    public static void main(String[] args) {
        Leetcode206 leetcode206 = new Leetcode206();
        ListNode head = ListNode.of(1, 2, 3, 4, 5);
        System.out.println(head);
        ListNode listNode = leetcode206.reverseList(head);
        //ListNode listNode = leetcode206.removeLast(head);
        System.out.println(listNode);
    }
}
