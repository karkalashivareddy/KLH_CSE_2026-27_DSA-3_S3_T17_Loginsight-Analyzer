package com.loginsight.analytics;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import com.loginsight.model.LogEvent;

/**
 * Activity heatmap: hour-of-day (24 columns) × day-of-week (7 rows). Uses calendar weekday/hour in
 * UTC. Zero cells are still emitted so the client can render a full grid.
 */
public final class HeatmapAnalyzer {

    public static final String[] DAY_LABELS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    public record Heatmap(String[] days, int columns, long[][] cells) {
    }

    public Heatmap hourByWeekday(List<LogEvent> events) {
        long[][] cells = new long[7][24];
        for (LogEvent event : events) {
            if (event.getTimestamp() == null) {
                continue;
            }
            Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.setTimeInMillis(event.getTimestamp().toEpochMilli());
            int weekday = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7; // Mon=0 … Sun=6
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            cells[weekday][hour]++;
        }
        return new Heatmap(DAY_LABELS, 24, cells);
    }
}