package com.eyeoncommit.engine;

import com.eyeoncommit.model.Commit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrequencyCounter {

    public static Map<String, Integer> fileFrequency(List<Commit> commits) {
        Map<String, Integer> counts = new HashMap<>();
        for (Commit commit : commits) {
            for (String file : commit.files()) {
                counts.merge(file, 1, Integer::sum);
            }
        }
        return counts;
    }

    public static Map<String, Integer> contributorFrequency(List<Commit> commits) {
        Map<String, Integer> counts = new HashMap<>();
        for (Commit commit : commits) {
            counts.merge(commit.authorEmail(), 1, Integer::sum);
        }
        return counts;
    }
}
