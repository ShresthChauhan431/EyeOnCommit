package com.eyeoncommit.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class TopK {

    public static List<Map.Entry<String, Integer>> of(Map<String, Integer> frequency, int k) {
        if (k <= 0 || frequency.isEmpty()) {
            return List.of();
        }

        Comparator<Map.Entry<String, Integer>> byCountThenName = (first, second) -> {
            int diff = Integer.compare(first.getValue(), second.getValue());
            if (diff != 0) return diff;
            return second.getKey().compareTo(first.getKey());
        };

        PriorityQueue<Map.Entry<String, Integer>> minHeap = new PriorityQueue<>(byCountThenName);

        for (var entry : frequency.entrySet()) {
            minHeap.offer(entry);
            if (minHeap.size() > k) {
                minHeap.poll();
            }
        }

        List<Map.Entry<String, Integer>> result = new ArrayList<>(minHeap);
        result.sort(byCountThenName.reversed());
        return result;
    }
}
