package com.devflow.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateFormatter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd. MMM");
    private static final DateTimeFormatter FULL_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private DateFormatter() {}

    public static String formatTime(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(TIME_FORMAT);
    }

    public static String formatRelative(LocalDateTime dt) {
        if (dt == null) return "";
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dt, now);
        if (minutes < 1) return "jetzt";
        if (minutes < 60) return minutes + " Min.";
        long hours = ChronoUnit.HOURS.between(dt, now);
        if (hours < 24) return hours + " Std.";
        long days = ChronoUnit.DAYS.between(dt, now);
        if (days < 7) return days + " T.";
        return dt.format(DATE_FORMAT);
    }

    public static String formatDate(LocalDateTime dt) {
        if (dt == null) return "";
        LocalDateTime now = LocalDateTime.now();
        if (dt.toLocalDate().equals(now.toLocalDate())) {
            return dt.format(TIME_FORMAT);
        }
        return dt.format(DATE_FORMAT);
    }

    public static String formatFull(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(FULL_FORMAT);
    }
}
