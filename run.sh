#!/bin/sh
# Usage: ./run.sh <repoPath> [topK] [commitLimit]
[ -d out ] || ./build.sh
java -cp out com.eyeoncommit.Main "$@"
