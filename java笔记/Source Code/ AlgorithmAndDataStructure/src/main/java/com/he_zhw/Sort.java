package com.he_zhw;

import java.util.Arrays;

public class Sort {

    /**
     * 冒泡排序
     *
     * @param arr
     * @return
     */
    public int[] BubbleSort1(int[] arr) {
        int[] ints = Arrays.copyOf(arr, arr.length);
        for (int j = ints.length - 1; j > 0; j--) {
            boolean flag = false;
            for (int i = 0; i < j; i++) {
                if (ints[i] > ints[i + 1]) {
                    int tmp = ints[i];
                    ints[i] = ints[i + 1];
                    ints[i + 1] = tmp;
                    flag = true;
                }
            }
            if (!flag) {
                return ints;
            }
        }


        return ints;
    }

    /**
     * @param arr
     * @return
     */
    public int[] BubbleSortRecursion1(int[] arr) {
        int[] ints = Arrays.copyOf(arr, arr.length);

        Recursion(ints, ints.length - 1);
        return ints;
    }

    /**
     * @param arr
     * @param j
     * @return
     */
    private void Recursion(int[] arr, int j) {
        if (j == 1) {
            return;
        }
        int x = 1;//有序序列与无序序列的边界
        for (int i = 0; i < j; i++) {
            if (arr[i] > arr[i + 1]) {
                int tmp = arr[i];
                arr[i] = arr[i + 1];
                arr[i + 1] = tmp;
                x = i;
            }
        }
        Recursion(arr, x);
    }

    /**
     * 插入排序
     *
     * @param arr
     * @return
     */
    public int[] InsertionSort1(int[] arr) {
        int[] ints = Arrays.copyOf(arr, arr.length);
        for (int i = 1; i < ints.length; i++) {
/*            int temp = arr[i];
            for (int j = i - 1; j >= 0; j--) {
                if (ints[j] > temp) {
                    ints[j + 1] = ints[j];
                    ints[j] = temp;
                } else {
                    break;
                }
            }*/
            int tmp = arr[i];
            int j = i;
            while (j > 0 && tmp < arr[j - 1]) {
                ints[j] = ints[j - 1];
                j--;
            }
            if (j != i) {
                ints[j] = tmp;
            }
        }
        return ints;
    }


    /**
     * 插入排序(递归版本)
     *
     * @param arr
     * @return
     */
    public int[] InsertionSortRecursion(int[] arr) {
        int[] ints = Arrays.copyOf(arr, arr.length);
        InsertionRecursion(ints, 1);
        return ints;
    }

    private void InsertionRecursion(int[] arr, int low) {
        if (low == arr.length) {
            return;
        }
        int tmp = arr[low];
        int j = low;
        while (j > 0 && tmp < arr[j - 1]) {
            arr[j] = arr[j - 1];
            j--;
        }
        if (j != low) {
            arr[j] = tmp;
        }

        InsertionRecursion(arr, low + 1);
    }


    /**
     * 选择排序
     *
     * @param arr
     * @return
     */
    public int[] SelectSort(int[] arr) {
        int[] ints = Arrays.copyOf(arr, arr.length);
        for (int i = 0; i < ints.length - 1; i++) {
            int minIndex = i;//减少交换次数
            for (int j = i + 1; j < ints.length; j++) {
                if (ints[i] > ints[j]) {
//                    int tmp=ints[i];
//                    ints[i]=ints[j];
//                    ints[j]=tmp;
                    minIndex = j;
                }
            }
            if (minIndex != i) {
                int tmp = ints[i];
                ints[i] = ints[minIndex];
                ints[minIndex] = tmp;
            }
        }
        return ints;
    }

    public int[] BubbleSort(int[] arr) {
        int[] ints = Arrays.copyOf(arr, arr.length);
        for (int j = 0; j < ints.length - 1; j++) {
            boolean flag = false;
            for (int i = 0; i < ints.length - j - 1; i++) {
                if (arr[i] > ints[i + 1]) {
                    int temp = ints[i];
                    ints[i] = ints[i + 1];
                    ints[i + 1] = temp;
                    flag = true;
                }
            }
            if (!flag) {
                return ints;
            }
        }

        return ints;
    }

    public int[] BubbleSortRecursion(int[] arr) {
        int[] ints = Arrays.copyOf(arr, arr.length);
        BubbleSortRecursiontemp(ints, arr.length - 1);

        return ints;
    }

    private int[] BubbleSortRecursiontemp(int[] ints, int start) {

        if (start <= 0) {
            return ints;
        }

        for (int i = 0; i < start; i++) {
            if (ints[i] > ints[i + 1]) {
                int temp = ints[i];
                ints[i] = ints[i + 1];
                ints[i + 1] = temp;
            }
        }
        BubbleSortRecursiontemp(ints, start - 1);
        return ints;
    }

    public int[] InsertionSort(int[] arr) {
        int[] ints = Arrays.copyOf(arr, arr.length);

        for (int low = 1; low <= ints.length; low++) {
            int i = low;
            int t = ints[low];
            while (i > 0 && ints[i] < ints[i - 1]) {
                ints[i] = ints[i - 1];
                i--;
            }
            if (i != low) {
                ints[i] = t;
            }

        }
        return ints;
    }

}
