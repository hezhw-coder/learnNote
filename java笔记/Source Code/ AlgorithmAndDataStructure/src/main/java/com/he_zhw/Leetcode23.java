package com.he_zhw;

import com.he_zhw.datastructure.ListNode;

public class Leetcode23 {
    public ListNode mergeKLists(ListNode[] lists) {

        if (lists.length==0) {
            return null;
        }

        return spiltLists(lists,0,lists.length-1);
    }

    private ListNode mergeTwoLists(ListNode list1, ListNode list2) {

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

    private ListNode spiltLists(ListNode[] lists, int i, int j) {
        int middle = (i + j) >>> 1;
        if (j == i) {
            return lists[i];
        }
        ListNode listNode = spiltLists(lists, i, middle);
        ListNode listNode1 = spiltLists(lists, middle + 1, j);
        return mergeTwoLists(listNode,listNode1);
    }

    public static void main(String[] args) {
        Leetcode23 leetcode23 = new Leetcode23();
        ListNode head1 = ListNode.of(1, 4, 5);
        ListNode head2 = ListNode.of(1, 3, 4);
        ListNode head3 = ListNode.of(2, 6);
        System.out.println(head1);
        System.out.println(head2);
        System.out.println(head3);
        ListNode[] lists = {head1, head2, head3};
//        ListNode listNode = leetcode23.removeN(head, 1);
        ListNode listNode = leetcode23.mergeKLists(lists);
//        int listNode = leetcode23.spiltLists(lists, 0, lists.length-1);
        //ListNode listNode = leetcode206.removeLast(head);
        System.out.println(listNode);
    }
}
