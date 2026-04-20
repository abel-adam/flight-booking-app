package com.example.flightbooking.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BookingUiUtils {

    private BookingUiUtils() {}

    /** Formats yyyy-MM-dd or returns original if parse fails */
    public static String formatDateHuman(String raw) {
        if (raw == null || raw.isEmpty()) return "—";
        try {
            if (raw.contains(",")) {
                return raw;
            }
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date d = in.parse(raw.trim());
            if (d == null) return raw;
            return new SimpleDateFormat("d MMM yyyy", Locale.US).format(d);
        } catch (ParseException e) {
            return raw;
        }
    }

    public static String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "Pending";
        String s = status.trim();
        if (s.equalsIgnoreCase("confirm") || s.equalsIgnoreCase("confirmed")) return "Confirmed";
        if (s.equalsIgnoreCase("pending")) return "Pending";
        if (s.equalsIgnoreCase("cancelled") || s.equalsIgnoreCase("canceled")) return "Cancelled";
        if (s.equalsIgnoreCase("completed")) return "Completed";
        return s;
    }
}
