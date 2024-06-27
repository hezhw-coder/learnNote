package com.he_zhw;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class SortTest {
    @Test
    public void BubbleSortTest() {
        Sort sort = new Sort();
        int[] arr={5,4,6,1,91,64};
//        int[] ints = sort.BubbleSort(arr);
//        int[] ints = sort.BubbleSortRecursion(arr);
        int[] ints = sort.InsertionSort(arr);
//        int[] ints = sort.InsertionSortRecursion(arr);
//        int[] ints = sort.SelectSort(arr);
        System.out.println(Arrays.toString(ints));
    }

}