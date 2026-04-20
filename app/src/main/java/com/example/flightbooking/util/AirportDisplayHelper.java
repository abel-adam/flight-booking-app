package com.example.flightbooking.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class AirportDisplayHelper {

    private static final Map<String, String> IATA_TO_CITY = new HashMap<>();

    static {
        IATA_TO_CITY.put("ADD", "Addis Ababa");
        IATA_TO_CITY.put("DXB", "Dubai");
        IATA_TO_CITY.put("JFK", "New York");
        IATA_TO_CITY.put("LHR", "London");
        IATA_TO_CITY.put("NBO", "Nairobi");
        IATA_TO_CITY.put("JED", "Jeddah");
        IATA_TO_CITY.put("CDG", "Paris");
        IATA_TO_CITY.put("NRT", "Tokyo");
        IATA_TO_CITY.put("SIN", "Singapore");
        IATA_TO_CITY.put("JNB", "Johannesburg");
        IATA_TO_CITY.put("CAI", "Cairo");
        IATA_TO_CITY.put("FRA", "Frankfurt");
        IATA_TO_CITY.put("IST", "Istanbul");
        IATA_TO_CITY.put("BOM", "Mumbai");
        IATA_TO_CITY.put("HKG", "Hong Kong");
        IATA_TO_CITY.put("BJR", "Bahir Dar");
        IATA_TO_CITY.put("DIR", "Dire Dawa");
        IATA_TO_CITY.put("LLI", "Lalibela");
        IATA_TO_CITY.put("GDQ", "Gondar");
        IATA_TO_CITY.put("AXU", "Axum");
        IATA_TO_CITY.put("MQX", "Mekelle");
        IATA_TO_CITY.put("JIM", "Jimma");
        IATA_TO_CITY.put("AWA", "Hawassa");
        IATA_TO_CITY.put("AMH", "Arba Minch");
        IATA_TO_CITY.put("RUH", "Riyadh");
        IATA_TO_CITY.put("TLV", "Tel Aviv");
        IATA_TO_CITY.put("DOH", "Doha");
        IATA_TO_CITY.put("AUH", "Abu Dhabi");
        IATA_TO_CITY.put("MCT", "Muscat");
        IATA_TO_CITY.put("KWI", "Kuwait City");
        IATA_TO_CITY.put("BAH", "Bahrain");
        IATA_TO_CITY.put("BEY", "Beirut");
        IATA_TO_CITY.put("FCO", "Rome");
        IATA_TO_CITY.put("MAD", "Madrid");
        IATA_TO_CITY.put("ATH", "Athens");
        IATA_TO_CITY.put("VIE", "Vienna");
        IATA_TO_CITY.put("BRU", "Brussels");
        IATA_TO_CITY.put("ARN", "Stockholm");
        IATA_TO_CITY.put("OSL", "Oslo");
        IATA_TO_CITY.put("GVA", "Geneva");
        IATA_TO_CITY.put("ZRH", "Zurich");
        IATA_TO_CITY.put("MXP", "Milan");
        IATA_TO_CITY.put("DME", "Moscow");
        IATA_TO_CITY.put("LOS", "Lagos");
        IATA_TO_CITY.put("CMN", "Casablanca");
        IATA_TO_CITY.put("CPT", "Cape Town");
        IATA_TO_CITY.put("DSS", "Dakar");
        IATA_TO_CITY.put("ABJ", "Abidjan");
        IATA_TO_CITY.put("ACC", "Accra");
        IATA_TO_CITY.put("LAD", "Luanda");
        IATA_TO_CITY.put("KGL", "Kigali");
        IATA_TO_CITY.put("EBB", "Entebbe");
        IATA_TO_CITY.put("DAR", "Dar es Salaam");
        IATA_TO_CITY.put("HRE", "Harare");
        IATA_TO_CITY.put("LUN", "Lusaka");
        IATA_TO_CITY.put("IAD", "Washington DC");
        IATA_TO_CITY.put("ORD", "Chicago");
        IATA_TO_CITY.put("LAX", "Los Angeles");
        IATA_TO_CITY.put("YYZ", "Toronto");
        IATA_TO_CITY.put("GRU", "São Paulo");
        IATA_TO_CITY.put("EZE", "Buenos Aires");
        IATA_TO_CITY.put("PEK", "Beijing");
        IATA_TO_CITY.put("ICN", "Seoul");
        IATA_TO_CITY.put("BKK", "Bangkok");
        IATA_TO_CITY.put("KUL", "Kuala Lumpur");
        IATA_TO_CITY.put("DEL", "Delhi");
        IATA_TO_CITY.put("MNL", "Manila");
        IATA_TO_CITY.put("CGK", "Jakarta");
        IATA_TO_CITY.put("CAN", "Guangzhou");
        IATA_TO_CITY.put("PVG", "Shanghai");
        IATA_TO_CITY.put("DUB", "Dublin");
    }

    private AirportDisplayHelper() {}

    /** "Addis Ababa (ADD)" -> "Addis Ababa" */
    public static String extractCityLabel(String field) {
        if (field == null) return "";
        int open = field.indexOf('(');
        if (open > 0) {
            return field.substring(0, open).trim();
        }
        return field.trim();
    }

    /** "Addis Ababa (ADD)" -> "ADD", "Dubai" -> "DXB" (via lookup) */
    public static String extractIataCode(String field) {
        if (field == null) return "";
        int open = field.indexOf('(');
        int close = field.indexOf(')');
        if (open >= 0 && close > open) {
            return field.substring(open + 1, close).trim().toUpperCase(Locale.US);
        }
        
        String t = field.trim().toUpperCase(Locale.US);
        if (t.length() == 3) return t;

        // Fallback: If they typed "Dubai", look up "DXB"
        for (Map.Entry<String, String> entry : IATA_TO_CITY.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(t)) {
                return entry.getKey();
            }
        }
        return t;
    }

    public static String cityNameForCode(String iataCode) {
        if (iataCode == null || iataCode.isEmpty()) return "";
        String c = iataCode.trim().toUpperCase(Locale.US);
        String name = IATA_TO_CITY.get(c);
        return name != null ? name : iataCode;
    }

    /** "Addis Ababa (ADD)" -> "addisababaadd", "Dubai" -> "dubai" */
    public static String normalizeRouteInput(String input) {
        if (input == null) return "";
        String s = input.toUpperCase(Locale.US)
                .replaceAll("\\(", "")
                .replaceAll("\\)", "")
                .replaceAll("\\s+", "")
                .trim();
        
        return s;
    }

    /** "16 Apr, 2026" or "2026-04-16" -> "2026-04-16" */
    public static String normalizeDate(String input) {
        if (input == null || input.isEmpty()) return "";
        String t = input.trim();
        
        // Handle variations like "18 April 2026"
        String cleaned = t.replace(",", "").replace("/", "-")
                         .replace("Sept", "Sep").replaceAll("\\s+", " ");
        
        // 1. Try ISO YYYY-MM-DD
        if (cleaned.matches("\\d{4}-\\d{2}-\\d{2}")) return cleaned;

        // 2. Try Display formats
        try {
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
            
            String[] formats = {
                "d MMM yyyy", "dd MMM yyyy", "MMMM d yyyy", "d MMMM yyyy",
                "yyyy-MM-dd", "dd-MM-yyyy", "MM-dd-yyyy", "d MMM, yyyy", "dd MMM, yyyy",
                "MMM d yyyy", "MMM dd yyyy"
            };
            for (String f : formats) {
                try {
                    java.text.SimpleDateFormat in = new java.text.SimpleDateFormat(f, Locale.US);
                    in.setLenient(false);
                    return out.format(in.parse(cleaned));
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        
        return cleaned;
    }
}
