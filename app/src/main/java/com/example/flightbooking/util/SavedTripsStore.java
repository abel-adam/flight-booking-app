package com.example.flightbooking.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import com.example.flightbooking.models.Flight;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Local \"saved trips\" for signed-in users (route summaries). */
public final class SavedTripsStore {

    private static final String PREFS = "saved_trips_store";
    private static final int MAX = 8;

    private SavedTripsStore() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(@Nullable String uid) {
        return "list_" + (uid != null ? uid : "guest");
    }

    public static void saveFlight(Context ctx, @Nullable String uid, Flight flight) {
        if (uid == null || flight == null) return;
        String summary = summaryLine(flight);
        try {
            JSONArray arr = new JSONArray(prefs(ctx).getString(key(uid), "[]"));
            JSONArray next = new JSONArray();
            JSONObject head = new JSONObject();
            head.put("line", summary);
            head.put("id", flight.getFlightNumber() != null ? flight.getFlightNumber() : "");
            next.put(head);
            for (int i = 0; i < arr.length() && next.length() < MAX; i++) {
                JSONObject o = arr.getJSONObject(i);
                if (summary.equals(o.optString("line"))) continue;
                next.put(o);
            }
            prefs(ctx).edit().putString(key(uid), next.toString()).apply();
        } catch (JSONException ignored) { }
    }

    public static List<String> loadLines(Context ctx, @Nullable String uid) {
        List<String> out = new ArrayList<>();
        if (uid == null) return out;
        try {
            JSONArray arr = new JSONArray(prefs(ctx).getString(key(uid), "[]"));
            for (int i = 0; i < arr.length(); i++) {
                out.add(arr.getJSONObject(i).optString("line"));
            }
        } catch (JSONException ignored) { }
        return out;
    }

    private static String summaryLine(Flight f) {
        String a = f.getFromCode() != null ? f.getFromCode() : "";
        String b = f.getToCode() != null ? f.getToCode() : "";
        String n = f.getFlightNumber() != null ? f.getFlightNumber() : "";
        return a + " - " + b + " · " + n;
    }
}
