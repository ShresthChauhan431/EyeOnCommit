package com.eyeoncommit;

import com.eyeoncommit.engine.*;
import com.eyeoncommit.model.Commit;
import java.util.*;

/** Dependency-free tests. Run: ./run-tests.sh  (exit code 1 on failure). */
public class EngineTests {
    static int failed = 0;

    static Commit c(String email, long ts, String... files) {
        return new Commit("h", email, email, ts, new LinkedHashSet<>(Arrays.asList(files)));
    }
    static void check(String name, boolean ok) {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failed++;
    }

    public static void main(String[] a) {
        var commits = List.of(c("a", 1, "X", "Y"), c("b", 2, "X"), c("a", 3, "X", "Z"));
        var ff = FrequencyCounter.fileFrequency(commits);
        check("file in multiple commits", ff.get("X") == 3);
        check("file appears once", ff.get("Y") == 1);
        check("duplicate in one commit counts once",
                FrequencyCounter.fileFrequency(List.of(c("a", 1, "X", "X"))).get("X") == 1);

        check("same contributor counted", FrequencyCounter.contributorFrequency(commits).get("a") == 2);
        check("empty list", FrequencyCounter.fileFrequency(List.of()).isEmpty());

        Map<String, Integer> m = Map.of("a", 5, "b", 5, "c", 9, "d", 1);
        var top2 = TopK.of(m, 2);
        check("top-k picks highest", top2.get(0).getKey().equals("c"));
        check("top-k tie -> alphabetical", top2.get(1).getKey().equals("a"));
        check("k larger than size", TopK.of(m, 10).size() == 4);
        check("k zero", TopK.of(m, 0).isEmpty());
        check("k negative", TopK.of(m, -3).isEmpty());

        check("0 files -> no pairs", CoChangeAnalyzer.pairs(List.of(c("a", 1)), 50).isEmpty());
        check("1 file -> no pairs", CoChangeAnalyzer.pairs(List.of(c("a", 1, "A")), 50).isEmpty());
        check("2 files -> 1 pair", CoChangeAnalyzer.pairs(List.of(c("a", 1, "A", "B")), 50).size() == 1);
        check("3 files -> 3 pairs", CoChangeAnalyzer.pairs(List.of(c("a", 1, "A", "B", "C")), 50).size() == 3);
        var canon = CoChangeAnalyzer.pairs(List.of(c("a", 1, "B", "A"), c("a", 2, "A", "B")), 50);
        check("pair order canonical", canon.size() == 1 && canon.values().iterator().next() == 2);
        check("large commit skipped", CoChangeAnalyzer.pairs(List.of(c("a", 1, "A", "B", "C")), 2).isEmpty());

        long day = 86400;
        var act = ActivityAnalyzer.weekly(List.of(c("a", 1731500000L), c("a", 1731500000L + 100),
                c("a", 1731500000L + 14 * day), c("a", -1), c("a", 0)));
        check("same day grouped + invalid skipped + different weeks",
                act.size() == 2 && act.firstEntry().getValue() == 2);

        System.out.println(failed == 0 ? "\nAll tests passed." : "\n" + failed + " test(s) FAILED.");
        System.exit(failed == 0 ? 0 : 1);
    }
}
