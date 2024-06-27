package com.he_zhw;

import org.junit.Test;

public class SearchTest {
    @Test
    public void binarySearchTest() {
        Search search = new Search();
        int[] arr = {-1, 0, 3, 5, 9, 12};
        int ret = search.search(arr, 0);
        System.out.println("索引位置" + ret);
    }
}