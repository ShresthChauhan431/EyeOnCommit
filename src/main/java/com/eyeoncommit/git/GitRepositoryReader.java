package com.eyeoncommit.git;

import com.eyeoncommit.model.Commit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GitRepositoryReader {

    public List<Commit> read(String repoPath, int limit) throws IOException, InterruptedException {
        File repoDir = new File(repoPath);

        boolean looksLikeGitRepo = new File(repoDir, ".git").exists()
                || (new File(repoDir, "HEAD").exists() && new File(repoDir, "objects").exists());

        if (!looksLikeGitRepo) {
            throw new IllegalArgumentException("Not a git repository: " + repoPath);
        }

        Process gitProcess = new ProcessBuilder(
                "git", "-C", repoPath, "log",
                "--name-only", "--no-merges",
                "-n", String.valueOf(limit),
                "--pretty=format:@@%H|%at|%ae|%an"
        ).redirectErrorStream(true).start();

        List<Commit> commits;
        StringBuilder errorOutput = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(gitProcess.getInputStream()))) {
            commits = parseLogOutput(reader, errorOutput);
        }

        int exitCode = gitProcess.waitFor();
        if (exitCode != 0 && commits.isEmpty()) {
            String errorText = errorOutput.toString().trim();
            if (errorText.contains("does not have any commits") || errorText.contains("bad default revision")) {
                return Collections.emptyList();
            }
            throw new IllegalStateException("git log failed" + (errorText.isEmpty() ? "" : ": " + errorText));
        }

        return commits;
    }

    public List<Commit> parseLogOutput(BufferedReader reader) throws IOException {
        return parseLogOutput(reader, null);
    }

    public List<Commit> parseLogOutput(BufferedReader reader, StringBuilder errorCapture) throws IOException {
        List<Commit> commits = new ArrayList<>();

        String line;
        String currentHash = null;
        String currentEmail = null;
        String currentName = null;
        long currentTimestamp = -1;
        Set<String> currentFiles = null;

        while ((line = reader.readLine()) != null) {
            if (line.startsWith("@@")) {
                if (currentHash != null) {
                    commits.add(new Commit(currentHash, currentEmail, currentName, currentTimestamp, currentFiles));
                }

                String[] parts = line.substring(2).split("\\|", 4);
                currentHash = parts[0];

                currentTimestamp = -1;
                if (parts.length > 1) {
                    try {
                        currentTimestamp = Long.parseLong(parts[1]);
                    } catch (NumberFormatException ignored) {
                    }
                }

                currentEmail = (parts.length > 2 && !parts[2].isBlank()) ? parts[2] : "unknown";
                currentName = (parts.length > 3 && !parts[3].isBlank()) ? parts[3] : currentEmail;
                currentFiles = new LinkedHashSet<>();

            } else if (currentHash != null) {
                if (!line.isBlank()) {
                    currentFiles.add(line.trim());
                }
            } else if (errorCapture != null) {
                errorCapture.append(line).append(System.lineSeparator());
            }
        }

        if (currentHash != null) {
            commits.add(new Commit(currentHash, currentEmail, currentName, currentTimestamp, currentFiles));
        }

        return commits;
    }
}
