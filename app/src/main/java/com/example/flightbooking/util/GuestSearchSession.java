package com.example.flightbooking.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists last flight search and a short list of recent searches so guests (and everyone)
 * keep their criteria across navigation and after signing in mid-flow.
 */
public final class GuestSearchSession {

    private static final String PREFS = "guest_search_session";
    private static final String K_FROM = "from";
    private static final String K_TO = "to";
    private static final String K_DATE = "date";
    private static final String K_RETURN = "return_date";
    private static final String K_PASSENGERS = "passengers";
    private static final String K_CLASS = "travel_class";
    private static final String K_ROUND_TRIP = "round_trip";
    private static final String K_MULTI = "multi_city";
    private static final String K_RECENT = "recent_json";
    private static final String K_MC2_FROM = "mc2_from";
    private static final String K_MC2_TO = "mc2_to";
    private static final String K_MC2_DATE = "mc2_date";

    private static final int MAX_RECENT = 5;

    private GuestSearchSession() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /**
     * Updates only origin/destination (minimal home search). Preserves dates, passengers, and class if already stored.
     */
    public static void saveHomeRoute(Context ctx, String from, String to) {
        SharedPreferences p = prefs(ctx);
        SharedPreferences.Editor ed = p.edit();
        ed.putString(K_FROM, from != null ? from : "");
        ed.putString(K_TO, to != null ? to : "");
        ed.putInt(K_PASSENGERS, 1);
        ed.putString(K_CLASS, "Economy");
        ed.putBoolean(K_ROUND_TRIP, false);
        ed.putBoolean(K_MULTI, false);
        ed.putString(K_DATE, "");
        ed.putString(K_RETURN, "");
        ed.apply();
        pushRecentSearch(ctx, from, to);
    }

    public static void saveCurrentSearch(Context ctx, String from, String to, String date,
                                         String returnDate, int passengers, String travelClass,
                                         boolean roundTrip, boolean multiCity) {
        android.content.SharedPreferences.Editor ed = prefs(ctx).edit()
                .putString(K_FROM, from != null ? from : "")
                .putString(K_TO, to != null ? to : "")
                .putString(K_DATE, date != null ? date : "")
                .putString(K_RETURN, returnDate != null ? returnDate : "")
                .putInt(K_PASSENGERS, passengers)
                .putString(K_CLASS, travelClass != null ? travelClass : "Economy")
                .putBoolean(K_ROUND_TRIP, roundTrip)
                .putBoolean(K_MULTI, multiCity);
        if (!multiCity) {
            ed.remove(K_MC2_FROM).remove(K_MC2_TO).remove(K_MC2_DATE);
        }
        ed.apply();
    }

    public static void saveMultiCityLeg2(Context ctx, String from, String to, String date) {
        prefs(ctx).edit()
                .putString(K_MC2_FROM, from != null ? from : "")
                .putString(K_MC2_TO, to != null ? to : "")
                .putString(K_MC2_DATE, date != null ? date : "")
                .apply();
    }

    @Nullable
    public static String getMultiLeg2From(Context ctx) {
        String s = prefs(ctx).getString(K_MC2_FROM, "");
        return s != null && !s.isEmpty() ? s : null;
    }

    @Nullable
    public static String getMultiLeg2To(Context ctx) {
        String s = prefs(ctx).getString(K_MC2_TO, "");
        return s != null && !s.isEmpty() ? s : null;
    }

    @Nullable
    public static String getMultiLeg2Date(Context ctx) {
        String s = prefs(ctx).getString(K_MC2_DATE, "");
        return s != null && !s.isEmpty() ? s : null;
    }

    public static void pushRecentSearch(Context ctx, String from, String to) {
        if (from == null || to == null || from.isEmpty() || to.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(prefs(ctx).getString(K_RECENT, "[]"));
            JSONArray next = new JSONArray();
            JSONObject head = new JSONObject();
            head.put("from", from);
            head.put("to", to);
            next.put(head);
            for (int i = 0; i < arr.length() && next.length() < MAX_RECENT; i++) {
                JSONObject o = arr.getJSONObject(i);
                String f = o.optString("from");
                String t = o.optString("to");
                if (from.equals(f) && to.equals(t)) continue;
                next.put(o);
            }
            prefs(ctx).edit().putString(K_RECENT, next.toString()).apply();
        } catch (JSONException ignored) {
            prefs(ctx).edit().putString(K_RECENT, "[]").apply();
        }
    }

    public static List<RecentRoute> loadRecentRoutes(Context ctx) {
        List<RecentRoute> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(ctx).getString(K_RECENT, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new RecentRoute(o.optString("from"), o.optString("to")));
            }
        } catch (JSONException ignored) { }
        return out;
    }

    public static boolean hasSavedSearch(Context ctx) {
        return prefs(ctx).contains(K_FROM);
    }

    public static void applyToForm(Context ctx, FormTarget target) {
        SharedPreferences p = prefs(ctx);
        if (!p.contains(K_FROM)) return;
        target.setFrom(p.getString(K_FROM, null));
        target.setTo(p.getString(K_TO, null));
        target.setDate(p.getString(K_DATE, null));
        target.setReturnDate(p.getString(K_RETURN, null));
        target.setPassengers(p.getInt(K_PASSENGERS, 0));
        target.setTravelClass(p.getString(K_CLASS, null));
        target.setRoundTrip(p.getBoolean(K_ROUND_TRIP, false));
        target.setMultiCity(p.getBoolean(K_MULTI, false));
    }

    public interface FormTarget {
        void setFrom(@Nullable String v);
        void setTo(@Nullable String v);
        void setDate(@Nullable String v);
        void setReturnDate(@Nullable String v);
        void setPassengers(int count);
        void setTravelClass(@Nullable String v);
        void setRoundTrip(boolean roundTrip);
        void setMultiCity(boolean multiCity);
    }

    public static final class RecentRoute {
        public final String from;
        public final String to;

        public RecentRoute(String from, String to) {
            this.from = from;
            this.to = to;
        }
    }
}
