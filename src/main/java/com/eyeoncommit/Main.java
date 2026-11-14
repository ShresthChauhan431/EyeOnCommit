package com.eyeoncommit;

import com.eyeoncommit.engine.ActivityAnalyzer;
import com.eyeoncommit.engine.CoChangeAnalyzer;
import com.eyeoncommit.engine.FrequencyCounter;
import com.eyeoncommit.engine.TopK;
import com.eyeoncommit.git.GitRepositoryReader;
import com.eyeoncommit.model.Commit;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final int MAX_FILES_FOR_COCHANGE = 50;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: eyeoncommit <repoPath> [topK] [commitLimit]");
            return;
        }

        String repoPath = args[0];
        int topK = 5;
        int commitLimit = 1000;

        try {
            if (args.length > 1) topK = Integer.parseInt(args[1]);
            if (args.length > 2) commitLimit = Integer.parseInt(args[2]);
        } catch (NumberFormatException ex) {
            System.err.println("Error: topK and commitLimit need to be numbers.");
            return;
        }

        if (topK <= 0) {
            System.err.println("Error: topK has to be at least 1.");
            return;
        }
        if (commitLimit <= 0) {
            System.err.println("Error: commitLimit has to be at least 1.");
            return;
        }

        List<Commit> commits;
        try {
            commits = new GitRepositoryReader().read(repoPath, commitLimit);
        } catch (Exception ex) {
            System.err.println("Error: " + ex.getMessage());
            return;
        }

        if (commits.isEmpty()) {
            System.err.println("No commits found.");
            return;
        }

        Map<String, Integer> fileCounts = FrequencyCounter.fileFrequency(commits);
        Map<String, Integer> authorCounts = FrequencyCounter.contributorFrequency(commits);

        Map<String, String> emailNameLookup = new HashMap<>();
        for (Commit commit : commits) {
            emailNameLookup.putIfAbsent(commit.authorEmail(), commit.authorName());
        }

        Map<String, Integer> contributorDisplay = new HashMap<>();
        authorCounts.forEach((email, count) -> {
            String displayName = emailNameLookup.get(email) + " <" + email + ">";
            contributorDisplay.put(displayName, count);
        });

        int totalCommits = commits.size();
        String repoName = figureOutRepoName(repoPath);

        System.out.println("EyeOnCommit Repository Report");
        System.out.println("-----------------------------");
        System.out.println("Repository: " + repoName);
        System.out.println("Commits analyzed: " + totalCommits + " (limit " + commitLimit + ")");
        System.out.println("Unique contributors: " + authorCounts.size());
        System.out.println("Unique files changed: " + fileCounts.size());

        printSection("Top " + topK + " Frequently Changed Files",
                TopK.of(fileCounts, topK), totalCommits);

        printSection("Top " + topK + " Contributors",
                TopK.of(contributorDisplay, topK), totalCommits);

        Map<String, Integer> cochangePairs = CoChangeAnalyzer.pairs(commits, MAX_FILES_FOR_COCHANGE);
        printSection("Top " + topK + " Co-Changed File Pairs",
                TopK.of(cochangePairs, topK), totalCommits);

        System.out.println("\nCommits per Week");
        ActivityAnalyzer.weekly(commits).forEach((week, count) -> {
            String bar = "#".repeat(Math.min(count, 40));
            System.out.printf("%s  %s %d%n", week, bar, count);
        });

        System.out.println("\nNote: counts are indicators, not measures of quality or effort.");
    }

    static String figureOutRepoName(String repoPath) {
        try {
            File resolved = new File(repoPath).getCanonicalFile();
            String name = resolved.getName();
            return name.isEmpty() ? resolved.getPath() : name;
        } catch (IOException ex) {
            File fallback = new File(repoPath).getAbsoluteFile();
            String name = fallback.getName();
            return name.isEmpty() ? fallback.getPath() : name;
        }
    }

    private static void printSection(String heading, List<Map.Entry<String, Integer>> ranked, int total) {
        System.out.println("\n" + heading);
        int position = 1;
        for (var entry : ranked) {
            double percent = 100.0 * entry.getValue() / total;
            System.out.printf("%2d. %-50s %4d commits (%.1f%%)%n",
                    position++, entry.getKey(), entry.getValue(), percent);
        }
    }
}
