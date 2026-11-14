package com.eyeoncommit.engine;

import java.util.*;

/** Top-K using a min-heap. Time O(n log k), space O(k). Ties broken alphabetically. */
public class TopK {

    public static List<Map.Entry<String, Integer>> of(Map<String, Integer> freq, int k) {
        if (k <= 0 || freq.isEmpty()) return List.of();
        Comparator<Map.Entry<String, Integer>> rank = (a, b) -> {
            int c = Integer.compare(a.getValue(), b.getValue());
            return c != 0 ? c : b.getKey().compareTo(a.getKey()); // smaller = worse
        };
        PriorityQueue<Map.Entry<String, Integer>> heap = new PriorityQueue<>(rank);
        for (var e : freq.entrySet()) {
            heap.offer(e);
            if (heap.size() > k) heap.poll();
        }
        List<Map.Entry<String, Integer>> out = new ArrayList<>(heap);
        out.sort(rank.reversed());
        return out;
    }
}
