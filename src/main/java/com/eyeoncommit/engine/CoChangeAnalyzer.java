package com.eyeoncommit.engine;

import com.eyeoncommit.model.Commit;
import java.util.*;

/** Counts unordered file pairs changed in the same commit. O(m^2) per commit, capped by maxFiles. */
public class CoChangeAnalyzer {

    public static Map<String, Integer> pairs(List<Commit> commits, int maxFilesPerCommit) {
        Map<String, Integer> pairs = new HashMap<>();
        for (Commit c : commits) {
            int n = c.files().size();
            if (n < 2 || n > maxFilesPerCommit) continue;
            List<String> f = new ArrayList<>(c.files());
            Collections.sort(f); // canonical order: A+B == B+A
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++)
                    pairs.merge(f.get(i) + "  +  " + f.get(j), 1, Integer::sum);
        }
        return pairs;
    }
}
