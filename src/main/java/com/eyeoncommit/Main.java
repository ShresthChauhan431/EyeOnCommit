package com.eyeoncommit;

import com.eyeoncommit.engine.*;
import com.eyeoncommit.git.GitRepositoryReader;
import com.eyeoncommit.model.Commit;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** Usage: java -cp out com.eyeoncommit.Main <repoPath> [topK=5] [commitLimit=1000] */
public class Main {
    static final int MAX_FILES_PER_COMMIT = 50;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -cp out com.eyeoncommit.Main <repoPath> [topK] [commitLimit]");
            return;
        }

        int k = 5;
        int limit = 1000;
        try {
            if (args.length > 1) k = Integer.parseInt(args[1]);
            if (args.length > 2) limit = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("Error: topK and commitLimit must be integers.");
            return;
        }

        if (k <= 0) {
            System.err.println("Error: topK must be greater than 0.");
            return;
        }
        if (limit <= 0) {
            System.err.println("Error: commitLimit must be greater than 0.");
            return;
        }

        List<Commit> commits;
        try {
            commits = new GitRepositoryReader().read(args[0], limit);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return;
        }
        if (commits.isEmpty()) { System.err.println("No commits found."); return; }

        Map<String, Integer> fileFreq = FrequencyCounter.fileFrequency(commits);
        Map<String, Integer> authorFreq = FrequencyCounter.contributorFrequency(commits);

        Map<String, String> emailToName = new HashMap<>();
        for (Commit c : commits) emailToName.putIfAbsent(c.authorEmail(), c.authorName());
        Map<String, Integer> byName = new HashMap<>();
        authorFreq.forEach((email, n) -> byName.put(emailToName.get(email) + " <" + email + ">", n));

        int total = commits.size();
        String repoName = resolveRepoName(args[0]);

        System.out.println("EyeOnCommit Repository Report");
        System.out.println("-----------------------------");
        System.out.println("Repository: " + repoName);
        System.out.println("Commits analyzed: " + total + " (limit " + limit + ")");
        System.out.println("Unique contributors: " + authorFreq.size());
        System.out.println("Unique files changed: " + fileFreq.size());

        print("Top " + k + " Frequently Changed Files", TopK.of(fileFreq, k), total);
        print("Top " + k + " Contributors", TopK.of(byName, k), total);
        print("Top " + k + " Co-Changed File Pairs",
                TopK.of(CoChangeAnalyzer.pairs(commits, MAX_FILES_PER_COMMIT), k), total);

        System.out.println("\nCommits per Week");
        ActivityAnalyzer.weekly(commits).forEach((w, n) ->
                System.out.printf("%s  %s %d%n", w, "#".repeat(Math.min(n, 40)), n));

        System.out.println("\nNote: counts are indicators, not measures of quality or effort.");
    }

    static String resolveRepoName(String repoPath) {
        try {
            File canonical = new File(repoPath).getCanonicalFile();
            String name = canonical.getName();
            return name.isEmpty() ? canonical.getPath() : name;
        } catch (IOException e) {
            File abs = new File(repoPath).getAbsoluteFile();
            String name = abs.getName();
            return name.isEmpty() ? abs.getPath() : name;
        }
    }

    static void print(String title, List<Map.Entry<String, Integer>> list, int total) {
        System.out.println("\n" + title);
        int i = 1;
        for (var e : list)
            System.out.printf("%2d. %-50s %4d commits (%.1f%%)%n", i++, e.getKey(), e.getValue(),
                    100.0 * e.getValue() / total);
    }
}
