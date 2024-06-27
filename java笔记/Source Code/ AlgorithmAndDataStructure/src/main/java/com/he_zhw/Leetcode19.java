package com.he_zhw;

import com.he_zhw.datastructure.ListNode;

/**
 * 删除链表的倒数第 N 个结点
 */
public class Leetcode19 {
    public ListNode removeNthFromEnd1(ListNode head, int n) {
        ListNode s = new ListNode(-1, head);
        ListNode p1 = s;
        ListNode p2 = s;

        for (int i = 1; i <= n + 1; i++) {
            p1 = p1.next;
        }
        while (true) {
            if (p1 == null) {
                p2.next = p2.next.next;
                break;
            }
            p1 = p1.next;
            p2 = p2.next;

        }
        return s.next;
    }

    public ListNode removeNthFromEnd(ListNode head, int n) {
/*        if (head==null){
            return null;
        }

        head.next= removeNthFromEnd(head.next, n);

        return head;*/
        ListNode p=new ListNode(-1,head);
        removeN(p,n);
        return p.next;
    }

    private int removeN(ListNode head, int n) {
        if (head == null) {
            return 0;
        }
        int i = removeN(head.next, n);
        if (i==n){
            head.next=head.next.next;

        }
        return i+1;
    }

    public static void main(String[] args) {
        Leetcode19 leetcode19 = new Leetcode19();
        ListNode head = ListNode.of(1, 2, 3, 4, 5);
        System.out.println(head);
//        ListNode listNode = leetcode19.removeN(head, 1);
        ListNode listNode = leetcode19.removeNthFromEnd(head, 5);
        //ListNode listNode = leetcode206.removeLast(head);
        System.out.println(listNode);
    }
}
