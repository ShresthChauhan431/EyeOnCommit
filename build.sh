#!/bin/sh
rm -rf out && mkdir out && javac -d out $(find src/main -name "*.java") && echo "Build OK"
