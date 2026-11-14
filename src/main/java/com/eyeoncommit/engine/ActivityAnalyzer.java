package com.eyeoncommit.engine;

import com.eyeoncommit.model.Commit;
import java.time.*;
import java.time.temporal.IsoFields;
import java.util.*;

/** Commits per ISO week. TreeMap keeps weeks sorted. Invalid timestamps are skipped. */
public class ActivityAnalyzer {

    public static TreeMap<String, Integer> weekly(List<Commit> commits) {
        TreeMap<String, Integer> weeks = new TreeMap<>();
        for (Commit c : commits) {
            if (c.epochSec() <= 0) continue;
            LocalDate d = Instant.ofEpochSecond(c.epochSec()).atZone(ZoneOffset.UTC).toLocalDate();
            String key = d.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                    + String.format("%02d", d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            weeks.merge(key, 1, Integer::sum);
        }
        return weeks;
    }
}
