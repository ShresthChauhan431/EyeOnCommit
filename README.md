# EyeOnCommit

A small CLI tool I built to analyze Git commit history. Point it at any repo and it tells you which files change the most, who's committing the most, which files tend to get changed together, and how active the repo has been week by week.

I wanted something lightweight that doesn't need Maven or Gradle — just `javac` and `git` on your PATH and you're good to go.

## Getting Started

You'll need JDK 17+ and git installed.

```
./build.sh
./run.sh /path/to/your/repo 5 1000
```

The arguments after the repo path are optional — first one is how many results to show (default 5), second is max commits to look at (default 1000).

To run the tests:
```
./run-tests.sh
```

On Windows you can just grab the `javac` and `java` commands from inside the shell scripts and run them directly.

## What It Does

- **Frequently changed files** — finds the files that show up in the most commits, which usually points to hotspots
- **Top contributors** — ranks authors by commit count
- **Co-changed file pairs** — figures out which files tend to get modified together (good for spotting tight coupling)
- **Weekly activity** — shows a simple timeline of commits per week

## How It's Organized

```
src/main/java/com/eyeoncommit/
├── model/
│   └── Commit.java            the commit data record
├── git/
│   └── GitRepositoryReader    shells out to git log and parses the output
├── engine/
│   ├── FrequencyCounter       counts how often each file/author appears
│   ├── TopK                   picks the top K entries using a min-heap
│   ├── CoChangeAnalyzer       finds file pairs that change together
│   └── ActivityAnalyzer       groups commits by ISO week
└── Main.java                  CLI entry point, prints the report
```

The engine package has no dependency on git or I/O so it's easy to test in isolation.

## Things to Know

- Merge commits are ignored since they don't represent actual file changes
- Commits touching more than 50 files get skipped for the co-change analysis (otherwise huge bulk commits mess up the pair counts)
- The numbers are just indicators — don't read too much into them about code quality or how hard someone is working
