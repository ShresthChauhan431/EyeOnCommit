package com.eyeoncommit.git;

import com.eyeoncommit.model.Commit;
import java.io.*;
import java.util.*;

/** Reads commit history by running `git log`. No external libraries needed. */
public class GitRepositoryReader {

    public List<Commit> read(String repoPath, int limit) throws IOException, InterruptedException {
        if (!new File(repoPath, ".git").exists())
            throw new IllegalArgumentException("Not a git repository: " + repoPath);

        Process p = new ProcessBuilder("git", "-C", repoPath, "log", "--name-only", "--no-merges",
                "-n", String.valueOf(limit), "--pretty=format:@@%H|%ae|%an|%at")
                .redirectErrorStream(true).start();

        List<Commit> commits = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line, hash = null, email = null, name = null;
            long ts = -1;
            Set<String> files = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("@@")) {
                    if (hash != null) commits.add(new Commit(hash, email, name, ts, files));
                    String[] parts = line.substring(2).split("\\|", 4);
                    hash = parts[0];
                    email = parts[1].isBlank() ? "unknown" : parts[1];
                    name = parts[2].isBlank() ? email : parts[2];
                    try { ts = Long.parseLong(parts[3]); } catch (Exception e) { ts = -1; }
                    files = new LinkedHashSet<>();
                } else if (!line.isBlank() && files != null) {
                    files.add(line.trim());
                }
            }
            if (hash != null) commits.add(new Commit(hash, email, name, ts, files));
        }
        if (p.waitFor() != 0 && commits.isEmpty())
            throw new IllegalStateException("git log failed (empty repository?)");
        return commits;
    }
}
