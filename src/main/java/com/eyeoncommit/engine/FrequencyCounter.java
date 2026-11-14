package com.eyeoncommit.engine;

import com.eyeoncommit.model.Commit;
import java.util.*;

/** Frequency counting with HashMap. Time O(total file changes). */
public class FrequencyCounter {

    /** file -> number of commits that changed it. */
    public static Map<String, Integer> fileFrequency(List<Commit> commits) {
        Map<String, Integer> m = new HashMap<>();
        for (Commit c : commits) for (String f : c.files()) m.merge(f, 1, Integer::sum);
        return m;
    }

    /** author email -> number of commits (same email = same contributor). */
    public static Map<String, Integer> contributorFrequency(List<Commit> commits) {
        Map<String, Integer> m = new HashMap<>();
        for (Commit c : commits) m.merge(c.authorEmail(), 1, Integer::sum);
        return m;
    }
}
