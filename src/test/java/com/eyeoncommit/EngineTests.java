package com.eyeoncommit;

import com.eyeoncommit.engine.*;
import com.eyeoncommit.git.GitRepositoryReader;
import com.eyeoncommit.model.Commit;
import java.io.BufferedReader;
import java.io.StringReader;
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

    public static void main(String[] a) throws Exception {
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

        // GitRepositoryReader tests
        GitRepositoryReader reader = new GitRepositoryReader();
        String gitLogSample = String.join("\n",
                "@@hash1|1731500000|alice@example.com|Alice | Team Lead",
                "file1.txt",
                "file2.txt",
                "@@hash2|1731586400|bob@example.com|Bob",
                "file2.txt",
                "@@hash3|1731672800||",
                "file3.txt"
        );
        List<Commit> parsedCommits = reader.parse(new BufferedReader(new StringReader(gitLogSample)));
        check("parse count", parsedCommits.size() == 3);

        Commit c1 = parsedCommits.get(0);
        check("author with pipe preserved", "Alice | Team Lead".equals(c1.authorName()));
        check("author with pipe timestamp valid", c1.epochSec() == 1731500000L);
        check("commit 1 files count", c1.files().size() == 2 && c1.files().contains("file1.txt"));

        Commit c3 = parsedCommits.get(2);
        check("blank email falls back to unknown", "unknown".equals(c3.authorEmail()));
        check("blank name falls back to email", "unknown".equals(c3.authorName()));

        check("parse empty input", reader.parse(new BufferedReader(new StringReader(""))).isEmpty());

        // ActivityAnalyzer with pipe author commit
        var pipeAct = ActivityAnalyzer.weekly(List.of(c1));
        check("activity timeline includes commit with pipe in author", pipeAct.size() == 1);

        // Main repo name resolution
        String repoNameDot = Main.resolveRepoName(".");
        check("resolveRepoName for '.' is not '.'", !repoNameDot.equals(".") && !repoNameDot.isEmpty());
        String repoNameSlash = Main.resolveRepoName("./");
        check("resolveRepoName for './' is not '.'", !repoNameSlash.equals(".") && !repoNameSlash.isEmpty());

        // Live read of current repository
        List<Commit> repoCommits = reader.read(".", 5);
        check("read current git repository", !repoCommits.isEmpty());

        System.out.println(failed == 0 ? "\nAll tests passed." : "\n" + failed + " test(s) FAILED.");
        System.exit(failed == 0 ? 0 : 1);
    }
}
