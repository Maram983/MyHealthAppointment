package com.myhealth.appointment.data;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class AppUtils {
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.ENGLISH);
    private static final DateTimeFormatter DISPLAY_TIME =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private AppUtils() {
    }

    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    public static String formatDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return "";
        }
        try {
            return LocalDate.parse(isoDate.substring(0, 10)).format(DISPLAY_DATE);
        } catch (Exception ignored) {
            return isoDate;
        }
    }

    public static String formatTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) {
            return "";
        }
        try {
            String trimmed = isoTime.length() >= 8 ? isoTime.substring(0, 8) : isoTime;
            return LocalTime.parse(trimmed).format(DISPLAY_TIME);
        } catch (Exception ignored) {
            return isoTime;
        }
    }

    public static String greetingForNow() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) {
            return "GOOD MORNING";
        }
        if (hour < 17) {
            return "GOOD AFTERNOON";
        }
        return "GOOD EVENING";
    }

    public static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "DR";
        }
        String[] parts = name.replace("Dr.", "").trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        builder.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) {
            builder.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }
        return builder.toString();
    }

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String trimmed = email.trim();
        return trimmed.contains("@") && trimmed.contains(".") && trimmed.length() > 5;
    }
}
