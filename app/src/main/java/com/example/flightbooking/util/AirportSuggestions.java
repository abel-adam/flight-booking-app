package com.example.flightbooking.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Static airport labels for autocomplete (IATA-focused). */
public final class AirportSuggestions {

    private static final String[] AIRPORTS = {
            // Local Ethiopia Flights
            "Addis Ababa (ADD)", "Bahir Dar (BJR)", "Dire Dawa (DIR)", "Lalibela (LLI)",
            "Gondar (GDQ)", "Axum (AXU)", "Mekelle (MQX)", "Jimma (JIM)", "Hawassa (AWA)", "Arba Minch (AMH)",
            
            // Middle East
            "Dubai (DXB)", "Jeddah (JED)", "Riyadh (RUH)", "Tel Aviv (TLV)", "Doha (DOH)", 
            "Abu Dhabi (AUH)", "Muscat (MCT)", "Kuwait City (KWI)", "Bahrain (BAH)", "Beirut (BEY)",
            
            // Europe
            "London (LHR)", "Paris (CDG)", "Frankfurt (FRA)", "Rome (FCO)", "Madrid (MAD)",
            "Athens (ATH)", "Vienna (VIE)", "Brussels (BRU)", "Stockholm (ARN)", "Oslo (OSL)", 
            "Geneva (GVA)", "Zurich (ZRH)", "Milan (MXP)", "Moscow (DME)", "Dublin (DUB)",
            
            // Africa
            "Nairobi (NBO)", "Johannesburg (JNB)", "Cairo (CAI)", "Lagos (LOS)", "Casablanca (CMN)",
            "Cape Town (CPT)", "Dakar (DSS)", "Abidjan (ABJ)", "Accra (ACC)", "Luanda (LAD)",
            "Kigali (KGL)", "Entebbe (EBB)", "Dar es Salaam (DAR)", "Harare (HRE)", "Lusaka (LUN)",
            
            // Americas 
            "New York (JFK)", "Washington DC (IAD)", "Chicago (ORD)", "Los Angeles (LAX)", 
            "Toronto (YYZ)", "São Paulo (GRU)", "Buenos Aires (EZE)",
            
            // Asia
            "Tokyo (NRT)", "Singapore (SIN)", "Istanbul (IST)", "Mumbai (BOM)", "Hong Kong (HKG)",
            "Beijing (PEK)", "Seoul (ICN)", "Bangkok (BKK)", "Kuala Lumpur (KUL)", "Delhi (DEL)",
            "Manila (MNL)", "Jakarta (CGK)", "Guangzhou (CAN)", "Shanghai (PVG)"
    };

    private AirportSuggestions() {}

    public static List<String> all() {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, AIRPORTS);
        return list;
    }

    /** Guess a sensible default "From" airport from device locale (no runtime permission). */
    public static String defaultFromForLocale(Locale locale) {
        if (locale == null) locale = Locale.getDefault();
        String country = locale.getCountry();
        if ("ET".equalsIgnoreCase(country)) return "Addis Ababa (ADD)";
        if ("AE".equalsIgnoreCase(country)) return "Dubai (DXB)";
        if ("US".equalsIgnoreCase(country)) return "New York (JFK)";
        if ("GB".equalsIgnoreCase(country)) return "London (LHR)";
        if ("KE".equalsIgnoreCase(country)) return "Nairobi (NBO)";
        if ("SA".equalsIgnoreCase(country)) return "Jeddah (JED)";
        return "Addis Ababa (ADD)";
    }
}
