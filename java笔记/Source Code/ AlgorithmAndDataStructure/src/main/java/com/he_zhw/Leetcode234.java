package com.he_zhw;

import com.he_zhw.datastructure.ListNode;

public class Leetcode234 {
    public boolean isPalindrome(ListNode head) {
        ListNode s = new ListNode(-1, head);
        ListNode p1 = s;
        ListNode p2 = s;
        int i = 0;
        while (p2 != null && p2.next != null) {//寻找中间节点
            p1 = p1.next;
            p2 = p2.next.next;
            i++;
        }
        if (p2 != null && p2.next == null) {//偶数链表
            p1 = p1.next;
        }
        ListNode last = reverseList(p1);
        ListNode p3 = s.next;
        while (i > 0) {
            if (p3.val != last.val) {
                return false;
            }
            p3 = p3.next;
            last = last.next;
            i--;
        }
        return true;
    }

    private ListNode reverseList(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode listNode = reverseList(head.next);
        head.next.next = head;
        head.next = null;
        return listNode;
    }


    public static void main(String[] args) {
        Leetcode234 leetcode234 = new Leetcode234();
        ListNode head = ListNode.of(1, 2, 2,1);
//        ListNode head = ListNode.of(1, 2, 3);
        System.out.println(head);
//        ListNode listNode = leetcode19.removeN(head, 1);
//        ListNode listNode = leetcode19.removeNthFromEnd(head, 5);
        boolean palindrome = leetcode234.isPalindrome(head);
        System.out.println(palindrome);
//        ListNode listNode = leetcode234.reverseList(head);
//        System.out.println(listNode);

    }
}
