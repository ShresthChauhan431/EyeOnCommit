package com.eyeoncommit.model;

import java.util.Set;

public record Commit(String hash, String authorEmail, String authorName,
                     long epochSec, Set<String> files) {}
