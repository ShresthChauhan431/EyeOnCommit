#!/bin/sh
rm -rf out-test && mkdir out-test && javac -d out-test $(find src -name "*.java") && java -cp out-test com.eyeoncommit.EngineTests
