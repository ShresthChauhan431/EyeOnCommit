package com.eyeoncommit.engine;

import com.eyeoncommit.model.Commit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CoChangeAnalyzer {

    public static Map<String, Integer> pairs(List<Commit> commits, int maxFilesPerCommit) {
        Map<String, Integer> pairCounts = new HashMap<>();

        for (Commit commit : commits) {
            int fileCount = commit.files().size();
            if (fileCount < 2 || fileCount > maxFilesPerCommit) {
                continue;
            }

            List<String> sorted = new ArrayList<>(commit.files());
            Collections.sort(sorted);

            for (int i = 0; i < sorted.size(); i++) {
                for (int j = i + 1; j < sorted.size(); j++) {
                    String pairKey = sorted.get(i) + "  +  " + sorted.get(j);
                    pairCounts.merge(pairKey, 1, Integer::sum);
                }
            }
        }

        return pairCounts;
    }
}
