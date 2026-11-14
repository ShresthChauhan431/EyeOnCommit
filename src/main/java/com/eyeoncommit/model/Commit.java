package com.eyeoncommit.model;

import java.util.Set;

/** One analyzed commit. files is a Set so duplicates count once. */
public record Commit(String hash, String authorEmail, String authorName,
                     long epochSec, Set<String> files) {}
