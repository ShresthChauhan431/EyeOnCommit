package com.eyeoncommit;

import com.eyeoncommit.engine.ActivityAnalyzer;
import com.eyeoncommit.engine.CoChangeAnalyzer;
import com.eyeoncommit.engine.FrequencyCounter;
import com.eyeoncommit.engine.TopK;
import com.eyeoncommit.git.GitRepositoryReader;
import com.eyeoncommit.model.Commit;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class EngineTests {

    static int failures = 0;

    static Commit makeCommit(String email, long timestamp, String... files) {
        return new Commit("abc123", email, email, timestamp,
                new LinkedHashSet<>(Arrays.asList(files)));
    }

    static void verify(String testName, boolean passed) {
        System.out.println((passed ? "PASS " : "FAIL ") + testName);
        if (!passed) failures++;
    }

    public static void main(String[] args) throws Exception {
        testFrequencyCounter();
        testTopK();
        testCoChangeAnalyzer();
        testActivityAnalyzer();
        testGitLogParsing();
        testRepoNameResolution();
        testLiveRead();

        System.out.println(failures == 0
                ? "\nAll tests passed."
                : "\n" + failures + " test(s) FAILED.");
        System.exit(failures == 0 ? 0 : 1);
    }

    static void testFrequencyCounter() {
        var sampleCommits = List.of(
                makeCommit("alice@test.com", 1, "README.md", "Main.java"),
                makeCommit("bob@test.com", 2, "README.md"),
                makeCommit("alice@test.com", 3, "README.md", "build.sh")
        );

        var fileCounts = FrequencyCounter.fileFrequency(sampleCommits);
        verify("file changed in multiple commits gets correct count", fileCounts.get("README.md") == 3);
        verify("file changed once has count of 1", fileCounts.get("Main.java") == 1);

        var duplicateFile = FrequencyCounter.fileFrequency(
                List.of(makeCommit("alice@test.com", 1, "X.java", "X.java")));
        verify("duplicate file in same commit only counted once", duplicateFile.get("X.java") == 1);

        var authorCounts = FrequencyCounter.contributorFrequency(sampleCommits);
        verify("contributor with two commits counted twice", authorCounts.get("alice@test.com") == 2);

        verify("empty commit list gives empty map", FrequencyCounter.fileFrequency(List.of()).isEmpty());
    }

    static void testTopK() {
        Map<String, Integer> scores = Map.of("alpha", 5, "beta", 5, "gamma", 9, "delta", 1);

        var top2 = TopK.of(scores, 2);
        verify("highest value comes first", top2.get(0).getKey().equals("gamma"));
        verify("ties broken alphabetically", top2.get(1).getKey().equals("alpha"));

        verify("k bigger than map returns everything", TopK.of(scores, 10).size() == 4);
        verify("k of zero returns empty", TopK.of(scores, 0).isEmpty());
        verify("negative k returns empty", TopK.of(scores, -3).isEmpty());
    }

    static void testCoChangeAnalyzer() {
        verify("commit with no files produces no pairs",
                CoChangeAnalyzer.pairs(List.of(makeCommit("x", 1)), 50).isEmpty());

        verify("commit with one file produces no pairs",
                CoChangeAnalyzer.pairs(List.of(makeCommit("x", 1, "A.java")), 50).isEmpty());

        verify("two files make one pair",
                CoChangeAnalyzer.pairs(List.of(makeCommit("x", 1, "A.java", "B.java")), 50).size() == 1);

        verify("three files make three pairs",
                CoChangeAnalyzer.pairs(List.of(makeCommit("x", 1, "A", "B", "C")), 50).size() == 3);

        var bothOrders = CoChangeAnalyzer.pairs(
                List.of(makeCommit("x", 1, "B", "A"), makeCommit("x", 2, "A", "B")), 50);
        verify("pair counted regardless of file order",
                bothOrders.size() == 1 && bothOrders.values().iterator().next() == 2);

        verify("commit exceeding max files gets skipped",
                CoChangeAnalyzer.pairs(List.of(makeCommit("x", 1, "A", "B", "C")), 2).isEmpty());
    }

    static void testActivityAnalyzer() {
        long secondsInDay = 86400;
        long baseTimestamp = 1731500000L;

        var timeline = ActivityAnalyzer.weekly(List.of(
                makeCommit("dev", baseTimestamp),
                makeCommit("dev", baseTimestamp + 100),
                makeCommit("dev", baseTimestamp + 14 * secondsInDay),
                makeCommit("dev", -1),
                makeCommit("dev", 0)
        ));

        verify("two weeks detected and invalid timestamps skipped",
                timeline.size() == 2 && timeline.firstEntry().getValue() == 2);
    }

    static void testGitLogParsing() throws Exception {
        GitRepositoryReader reader = new GitRepositoryReader();

        String fakeGitLog = String.join("\n",
                "@@aaa111|1731500000|alice@dev.io|Alice | Team Lead",
                "src/Main.java",
                "README.md",
                "@@bbb222|1731586400|bob@dev.io|Bob",
                "README.md",
                "@@ccc333|1731672800||",
                "config.yml"
        );

        List<Commit> parsed = reader.parseLogOutput(new BufferedReader(new StringReader(fakeGitLog)));
        verify("parsed three commits from log", parsed.size() == 3);

        Commit first = parsed.get(0);
        verify("author name with pipe character preserved", "Alice | Team Lead".equals(first.authorName()));
        verify("timestamp parsed correctly", first.epochSec() == 1731500000L);
        verify("files attached to correct commit", first.files().size() == 2 && first.files().contains("src/Main.java"));

        Commit third = parsed.get(2);
        verify("missing email defaults to unknown", "unknown".equals(third.authorEmail()));
        verify("missing name falls back to email", "unknown".equals(third.authorName()));

        verify("empty input returns empty list",
                reader.parseLogOutput(new BufferedReader(new StringReader(""))).isEmpty());

        var weeklyFromParsed = ActivityAnalyzer.weekly(List.of(first));
        verify("parsed commit works with activity analyzer", weeklyFromParsed.size() == 1);
    }

    static void testRepoNameResolution() {
        String dotResult = Main.figureOutRepoName(".");
        verify("dot path resolves to actual folder name", !dotResult.equals(".") && !dotResult.isEmpty());

        String slashResult = Main.figureOutRepoName("./");
        verify("dot-slash path resolves to actual folder name", !slashResult.equals(".") && !slashResult.isEmpty());
    }

    static void testLiveRead() throws Exception {
        GitRepositoryReader reader = new GitRepositoryReader();
        List<Commit> liveCommits = reader.read(".", 5);
        verify("can read commits from current git repo", !liveCommits.isEmpty());
    }
}
