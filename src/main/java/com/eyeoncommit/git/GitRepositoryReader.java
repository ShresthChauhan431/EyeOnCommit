package com.eyeoncommit.git;

import com.eyeoncommit.model.Commit;
import java.io.*;
import java.util.*;

/** Reads commit history by running `git log`. No external libraries needed. */
public class GitRepositoryReader {

    public List<Commit> read(String repoPath, int limit) throws IOException, InterruptedException {
        File dir = new File(repoPath);
        boolean isGit = new File(dir, ".git").exists()
                || (new File(dir, "HEAD").exists() && new File(dir, "objects").exists());
        if (!isGit)
            throw new IllegalArgumentException("Not a git repository: " + repoPath);

        Process p = new ProcessBuilder("git", "-C", repoPath, "log", "--name-only", "--no-merges",
                "-n", String.valueOf(limit), "--pretty=format:@@%H|%at|%ae|%an")
                .redirectErrorStream(true).start();

        List<Commit> commits;
        StringBuilder errBuf = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            commits = parse(br, errBuf);
        }
        int exitCode = p.waitFor();
        if (exitCode != 0 && commits.isEmpty()) {
            String err = errBuf.toString().trim();
            if (err.contains("does not have any commits") || err.contains("bad default revision")) {
                return Collections.emptyList();
            }
            throw new IllegalStateException("git log failed" + (err.isEmpty() ? "" : ": " + err));
        }
        return commits;
    }

    public List<Commit> parse(BufferedReader br) throws IOException {
        return parse(br, null);
    }

    public List<Commit> parse(BufferedReader br, StringBuilder headerCapture) throws IOException {
        List<Commit> commits = new ArrayList<>();
        String line, hash = null, email = null, name = null;
        long ts = -1;
        Set<String> files = null;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("@@")) {
                if (hash != null) commits.add(new Commit(hash, email, name, ts, files));
                String[] parts = line.substring(2).split("\\|", 4);
                hash = parts[0];
                ts = -1;
                if (parts.length > 1) {
                    try { ts = Long.parseLong(parts[1]); } catch (Exception ignored) {}
                }
                email = (parts.length > 2 && !parts[2].isBlank()) ? parts[2] : "unknown";
                name = (parts.length > 3 && !parts[3].isBlank()) ? parts[3] : email;
                files = new LinkedHashSet<>();
            } else if (hash != null) {
                if (!line.isBlank()) files.add(line.trim());
            } else if (headerCapture != null) {
                headerCapture.append(line).append(System.lineSeparator());
            }
        }
        if (hash != null) commits.add(new Commit(hash, email, name, ts, files));
        return commits;
    }
}
