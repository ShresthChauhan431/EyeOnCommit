package com.eyeoncommit.engine;

import com.eyeoncommit.model.Commit;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.TreeMap;

public class ActivityAnalyzer {

    public static TreeMap<String, Integer> weekly(List<Commit> commits) {
        TreeMap<String, Integer> weeklyActivity = new TreeMap<>();

        for (Commit commit : commits) {
            if (commit.epochSec() <= 0) {
                continue;
            }

            LocalDate date = Instant.ofEpochSecond(commit.epochSec())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();

            int weekYear = date.get(IsoFields.WEEK_BASED_YEAR);
            int weekNum = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            String weekLabel = weekYear + "-W" + String.format("%02d", weekNum);

            weeklyActivity.merge(weekLabel, 1, Integer::sum);
        }

        return weeklyActivity;
    }
}
