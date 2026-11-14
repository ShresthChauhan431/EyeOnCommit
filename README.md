# EyeOnCommit

Simple Java CLI that analyzes a Git repository's commit history using self-implemented
data-structure logic (HashMap counting, min-heap Top-K, pair counting, TreeMap timeline).

## Requirements
JDK 17+ and `git` on PATH. No other dependencies.

## Run
```
./build.sh
./run.sh /path/to/repo 5 1000      # repoPath, topK, commitLimit
./run-tests.sh                     # unit tests
```
(Windows: run the `javac` / `java` commands inside the .sh files.)

## Structure
```
model/   Commit record
git/     GitRepositoryReader   - runs `git log`, parses commits
engine/  FrequencyCounter      - file & contributor counts      O(F)
         TopK                  - min-heap ranking               O(n log k)
         CoChangeAnalyzer      - canonical file pairs, capped   O(sum m^2)
         ActivityAnalyzer      - commits per ISO week (TreeMap) O(C log W)
Main     CLI report
```
`engine/` does not depend on git or I/O, so it is testable on its own.

## Limits
- Merge commits are skipped; commits with >50 files are skipped for co-change.
- Counts are indicators, not measures of code quality or developer effort.
- Next steps: Maven + JUnit, date-range filter, Spring Boot REST API, frontend.
