package com.he_zhw;

import java.lang.annotation.Target;

public class Search {
    public int binarySearch1(int[] arr, int target) {
        int end = arr.length - 1;
        int start = 0;
        int count = 0;
        while (start <= end) {
            count++;
            int middle = (end + start) >>> 1;
            if (target == arr[middle]) {
                System.out.println("查找了" + count + "次");
                return middle;
            } else if (target > arr[middle]) {
                start = middle + 1;
            } else if (target < arr[middle]) {
                end = middle - 1;
            }
        }
        System.out.println("查找了" + count + "次");
        return -1;
    }

    public int binarySearchBalance1(int[] arr, int target) {

        int end = arr.length;
        int start = 0;
        int count = 0;
        int middle = 0;
        while (1 < end - start) {
            count++;
            middle = (end + start) >>> 1;

            if (target < arr[middle]) {
                end = middle;
            } else {
                start = middle;
            }
        }
        if (target == arr[middle]) {
            System.out.println("查找了" + count + "次");
            return middle;
        }
        System.out.println("查找了" + count + "次");
        return -1;
    }

    public int search1(int[] nums, int target) {
        int j = nums.length - 1;
        int i = 0;
        while (i <= j) {
            int m = (i + j) >>> 1;
            int num = nums[m];
            if (num == target) {
                return m;
            } else if (num > target) {
                j = m - 1;

            } else {
                i = m + 1;
            }
        }
        return -1;
    }

    public int search(int[] nums, int target) {
        return searchRecursion(nums, 0, nums.length - 1, target);
    }

    private int searchRecursion(int[] nums, int start, int end, int target) {
        if (start > end) {
            return -1;
        }
        int m = (start + end) >>> 1;
        if (nums[m] == target) {
            return m;
        } else if (nums[m] < target) {
            start = m + 1;
        } else {
            end = m - 1;
        }
        return searchRecursion(nums, start, end, target);
    }

}
