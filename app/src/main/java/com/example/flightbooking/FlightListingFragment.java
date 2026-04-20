package com.example.flightbooking;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.flightbooking.adapters.FlightAdapter;
import com.example.flightbooking.models.Flight;
import com.example.flightbooking.util.AirportDisplayHelper;
import com.example.flightbooking.util.SavedTripsStore;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.firebase.firestore.ListenerRegistration;

public class FlightListingFragment extends Fragment {

    private static final String TAG = "FlightListingFragment";

    private enum SortMode { BEST_VALUE, PRICE, DURATION, DEPARTURE }

    private FirebaseFirestore db;
    private FlightAdapter adapter;
    private final List<Flight> allFlights = new ArrayList<>();
    private final List<Flight> flightList = new ArrayList<>();

    private SortMode sortMode = SortMode.BEST_VALUE;
    private boolean directOnly;
    private boolean refundableOnly;
    private boolean morningDepartureOnly;
    private int maxPriceCap; // 0 = none, else e.g. 600
    private TextView btnSortLabel;
    private TextView tvEmptyFlights;
    private RecyclerView rvFlights;
    private String searchFromIata = "";
    private String searchToIata = "";
    private String searchDate = "";
    private ProgressBar pbLoading;
    private boolean multiCityTrip;
    private ListenerRegistration flightsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flight_listing, container, false);
        try {
            db = FirebaseFirestore.getInstance();

            TextView tvRouteFrom = view.findViewById(R.id.tvRouteFrom);
            TextView tvRouteTo = view.findViewById(R.id.tvRouteTo);
            TextView tvHeaderSubtitle = view.findViewById(R.id.tvHeaderSubtitle);
            rvFlights = view.findViewById(R.id.rvFlights);
            tvEmptyFlights = view.findViewById(R.id.tvEmptyFlights);
            pbLoading = view.findViewById(R.id.pbLoading);
            btnSortLabel = view.findViewById(R.id.btnSort);
            
            View btnBack = view.findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
            }

            Context ctx = getContext();
            Bundle args = getArguments();
            if (ctx != null && args != null) {
                String from = args.getString("fromCity", "Addis Ababa (ADD)");
                String to = args.getString("toCity", "Dubai (DXB)");
                String date = args.getString("date", "15 Jun, 2024");
                int passengers = args.getInt("passengers", 1);
                String travelClass = args.getString("travelClass", "Economy");
                multiCityTrip = args.getBoolean("multiCity", false);
                
                searchFromIata = AirportDisplayHelper.extractIataCode(from).trim();
                searchToIata = AirportDisplayHelper.extractIataCode(to).trim();
                searchDate = date; 

                if (tvRouteFrom != null) tvRouteFrom.setText(from);
                if (tvRouteTo != null) tvRouteTo.setText(to);
                
                String passengerLabel = passengers + (passengers == 1 ? " Passenger" : " Passengers");
                String dateFormatted = formatLongFormDate(date);
                
                if (tvHeaderSubtitle != null) {
                    String sub = dateFormatted + " \u2022 " + passengerLabel + " \u2022 " + travelClass;
                    if (multiCityTrip) {
                        boolean isLeg2 = args.containsKey("leg1_flight");
                        sub = sub + (isLeg2 ? " - Leg 2" : " - Leg 1");
                    }
                    tvHeaderSubtitle.setText(sub);
                }
            }

            if (rvFlights != null) {
                rvFlights.setLayoutManager(new LinearLayoutManager(ctx));
                adapter = new FlightAdapter(flightList, flight -> BookingFlowDialogs.openBookingOrGate(FlightListingFragment.this, flight, () -> openBooking(flight)), this::onSaveTripClicked);
                rvFlights.setAdapter(adapter);
            }

            View btnFilters = view.findViewById(R.id.btnFilters);
            if (btnFilters != null) btnFilters.setOnClickListener(this::showFilterMenu);
            if (btnSortLabel != null) btnSortLabel.setOnClickListener(this::showSortMenu);
            
            updateSortLabel();
            fetchFlights();
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL INIT ERROR", e);
            if (getContext() != null) {
                new MaterialAlertDialogBuilder(getContext())
                    .setTitle("Component View Failure")
                    .setMessage("Details: " + e.getMessage())
                    .setPositiveButton("Back", (d, w) -> { if (isAdded()) getParentFragmentManager().popBackStack(); })
                    .show();
            }
        }
        return view;
    }

    private void openBooking(Flight flight) {
        if (flight == null || !isAdded()) return;

        // Redirect to login if user is not signed in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please sign in to book your flight", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            startActivity(intent);
            return;
        }



        try {
            Bundle args = new Bundle();
            args.putSerializable("selected_flight", flight);
            
            Fragment nextFragment;
            if (multiCityTrip) {
                FlightListingFragment leg2 = new FlightListingFragment();
                if (getArguments() != null) {
                    args.putString("fromCity", getArguments().getString("mc2From"));
                    args.putString("toCity", getArguments().getString("mc2To"));
                    args.putString("date", getArguments().getString("mc2Date"));
                    args.putInt("passengers", getArguments().getInt("passengers", 1));
                    args.putString("travelClass", getArguments().getString("travelClass"));
                }
                args.putBoolean("multiCity", false); 
                args.putSerializable("leg1_flight", flight);
                nextFragment = leg2;
            } else {
                Flight leg1Flight = getArguments() != null ? (Flight) getArguments().getSerializable("leg1_flight") : null;
                if (leg1Flight != null) args.putSerializable("leg1_flight", leg1Flight);
                nextFragment = new BookingFragment();
            }

            nextFragment.setArguments(args);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, nextFragment)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();

        } catch (Exception e) {
            Log.e(TAG, "Critical Navigation Error", e);
        }
    }

    private void onSaveTripClicked(Flight flight) {
        Context ctx = getContext();
        if (ctx == null) return;
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            new MaterialAlertDialogBuilder(ctx)
                    .setTitle(R.string.save_trip_login_title)
                    .setMessage(R.string.save_trip_login_message)
                    .setPositiveButton(R.string.booking_gate_login, (d, w) -> {
                        if (getContext() != null) {
                            getContext().startActivity(new Intent(getContext(), LoginActivity.class));
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }
        SavedTripsStore.saveFlight(ctx, u.getUid(), flight);
        Toast.makeText(ctx, R.string.save_trip_saved, Toast.LENGTH_SHORT).show();
    }

    private String formatLongFormDate(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            String t = raw.trim().replace("Sept", "Sep");
            SimpleDateFormat in = new SimpleDateFormat("d MMM, yyyy", Locale.US);
            Date d;
            try {
                d = in.parse(t);
            } catch (ParseException e) {
                in = new SimpleDateFormat("d MMM yyyy", Locale.US);
                d = in.parse(t.replace(",", ""));
            }
            if (d == null) return raw;
            return new SimpleDateFormat("d MMMM yyyy", Locale.US).format(d);
        } catch (Exception e) {
            return raw;
        }
    }

    private void updateSortLabel() {
        if (btnSortLabel == null) return;
        switch (sortMode) {
            case DURATION:
                btnSortLabel.setText(R.string.sort_duration_label);
                break;
            case DEPARTURE:
                btnSortLabel.setText(R.string.sort_time_label);
                break;
            case PRICE:
                btnSortLabel.setText(R.string.sort_price_label);
                break;
            case BEST_VALUE:
            default:
                btnSortLabel.setText(R.string.sort_best_value_label);
                break;
        }
    }

    private void showSortMenu(View anchor) {
        Context ctx = getContext();
        if (ctx == null) return;
        PopupMenu pm = new PopupMenu(ctx, anchor);
        pm.getMenu().add(0, 0, 0, "Best value (recommended)");
        pm.getMenu().add(0, 1, 0, "Price (low to high)");
        pm.getMenu().add(0, 2, 0, "Duration (shortest)");
        pm.getMenu().add(0, 3, 0, "Departure time");
        pm.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 0) {
                sortMode = SortMode.BEST_VALUE;
            } else if (id == 1) {
                sortMode = SortMode.PRICE;
            } else if (id == 2) {
                sortMode = SortMode.DURATION;
            } else if (id == 3) {
                sortMode = SortMode.DEPARTURE;
            }
            updateSortLabel();
            applyFilterAndSort();
            return true;
        });
        pm.show();
    }

    private void showFilterMenu(View anchor) {
        Context ctx = getContext();
        if (ctx == null) return;
        PopupMenu pm = new PopupMenu(ctx, anchor);
        pm.getMenu().add(0, 1, 0, directOnly ? "✓ Direct flights only" : "Direct flights only");
        pm.getMenu().add(0, 2, 0, refundableOnly ? "✓ Refundable only" : "Refundable only");
        pm.getMenu().add(0, 4, 0, morningDepartureOnly ? "✓ Morning departure (before noon)" : "Morning departure (before noon)");
        pm.getMenu().add(0, 10, 0, maxPriceCap == 600 ? "✓ Max price $600" : "Max price $600");
        pm.getMenu().add(0, 11, 0, maxPriceCap == 800 ? "✓ Max price $800" : "Max price $800");
        pm.getMenu().add(0, 3, 0, "Clear filters");
        pm.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                directOnly = !directOnly;
            } else if (id == 2) {
                refundableOnly = !refundableOnly;
            } else if (id == 4) {
                morningDepartureOnly = !morningDepartureOnly;
            } else if (id == 10) {
                maxPriceCap = maxPriceCap == 600 ? 0 : 600;
            } else if (id == 11) {
                maxPriceCap = maxPriceCap == 800 ? 0 : 800;
            } else if (id == 3) {
                directOnly = false;
                refundableOnly = false;
                morningDepartureOnly = false;
                maxPriceCap = 0;
            }
            applyFilterAndSort();
            return true;
        });
        pm.show();
    }

    private void fetchFlights() {
        if (pbLoading != null) pbLoading.setVisibility(View.VISIBLE);
        if (tvEmptyFlights != null) {
            tvEmptyFlights.setVisibility(View.GONE);
        }

        if (flightsListener != null) flightsListener.remove();

        flightsListener = db.collection("flights")
                .addSnapshotListener((value, error) -> {
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    
                    if (error != null) {
                        Log.e(TAG, "Snapshot error: " + error.getMessage());
                        return;
                    }

                    if (value != null && isAdded()) {
                        if (tvEmptyFlights != null) tvEmptyFlights.setVisibility(View.GONE);
                        allFlights.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Flight f = doc.toObject(Flight.class);
                            if (f != null) {
                                f.setId(doc.getId());
                                normalizeFlightData(f);
                                allFlights.add(f);
                            }
                        }
                        applyFilterAndSort();
                    }
                });
    }

    private void normalizeFlightData(Flight f) {
        // 1. Naming Consistency
        if (f.getAirlineName() != null && f.getAirlineName().equalsIgnoreCase("Ethioairline")) {
            f.setAirlineName("Ethioairlines");
        }

        // 2. Duration Math Fix
        if (f.getFromTime() != null && f.getToTime() != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
                Date start = sdf.parse(f.getFromTime().trim());
                Date end = sdf.parse(f.getToTime().trim());
                if (start != null && end != null) {
                    long diffMs = end.getTime() - start.getTime();
                    if (diffMs < 0) diffMs += 24 * 60 * 60 * 1000; // Handle overnight

                    long hours = diffMs / (60 * 60 * 1000);
                    long mins = (diffMs % (60 * 60 * 1000)) / (60 * 1000);
                    
                    String calcDuration = hours + "h " + mins + "m";
                    // Only update if it's wildly different or user requested fix
                    f.setDuration(calcDuration);
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (flightsListener != null) {
            flightsListener.remove();
        }
    }

    private boolean isActiveFlight(Flight f) {
        String s = f.getStatus();
        return s != null && "Active".equalsIgnoreCase(s.trim());
    }

    private boolean matchesSearchRoute(Flight f) {
        // Universal Normalization for Route
        String sFrom = AirportDisplayHelper.normalizeRouteInput(searchFromIata);
        String sTo = AirportDisplayHelper.normalizeRouteInput(searchToIata);
        
        String fFrom = AirportDisplayHelper.normalizeRouteInput(f.getFromCode());
        String fTo = AirportDisplayHelper.normalizeRouteInput(f.getToCode());

        // 1. Route Matching (Priority 1: Code/Normalization match)
        boolean fromMatch = sFrom.equals(fFrom) || fFrom.contains(sFrom) || sFrom.contains(fFrom);
        boolean toMatch = sTo.equals(fTo) || fTo.contains(sTo) || sTo.contains(fTo);

        // Priority 2: City Label fallback (Robust against partial inputs)
        String fullFrom = "";
        if (getArguments() != null) fullFrom = getArguments().getString("fromCity", "").toUpperCase(Locale.US);
        if (fullFrom.isEmpty()) fullFrom = AirportDisplayHelper.cityNameForCode(searchFromIata).toUpperCase(Locale.US);

        String fullTo = "";
        if (getArguments() != null) fullTo = getArguments().getString("toCity", "").toUpperCase(Locale.US);
        if (fullTo.isEmpty()) fullTo = AirportDisplayHelper.cityNameForCode(searchToIata).toUpperCase(Locale.US);

        if (!fromMatch) {
            String fFromName = AirportDisplayHelper.cityNameForCode(fFrom).toUpperCase(Locale.US);
            if (fullFrom.contains(fFromName) || fFromName.contains(fullFrom) || fullFrom.contains(fFrom)) fromMatch = true;
        }
        
        if (!toMatch) {
            String fToName = AirportDisplayHelper.cityNameForCode(fTo).toUpperCase(Locale.US);
            if (fullTo.contains(fToName) || fToName.contains(fullTo) || fullTo.contains(fTo)) toMatch = true;
        }

        // Special Guard: DUB (Dublin) vs DXB (Dubai)
        // If the user explicitly typed DUB for Dubai, we still want to keep them separate 
        // unless the city name search fallback below finds a reason to link them.
        if (sTo.equals("DXB") && fTo.equals("DUB") && !fullTo.contains("DUBLIN")) toMatch = false;
        if (sTo.equals("DUB") && fTo.equals("DXB") && !fullTo.contains("DUBAI")) toMatch = false;

        boolean routeMatch = fromMatch && toMatch;
        
        // 2. Date Matching (Robust resilience)
        boolean dateMatch = true;
        if (searchDate != null && !searchDate.isEmpty()) {
            String fd = f.getDepartureDate();
            if (fd != null && !fd.isEmpty()) {
               String s1 = AirportDisplayHelper.normalizeDate(searchDate);
               String s2 = AirportDisplayHelper.normalizeDate(fd);
               
               // Exact match or partial date match (e.g. 2026-04 matches 2026-04-19)
               if (!s1.equalsIgnoreCase(s2) && !fd.contains(s1) && !s1.contains(fd)) {
                   dateMatch = false;
               }
            }
        }
        
        return routeMatch && dateMatch;
    }

    private void applyFilterAndSort() {
        flightList.clear();
        int rawCount = allFlights.size();
        
        for (Flight f : allFlights) {
            if (!isActiveFlight(f)) continue;
            
            // Core matching
            if (!matchesSearchRoute(f)) continue;
            
            // Optional filters (only apply if specifically toggled)
            if (directOnly) {
                String d = f.getDirect();
                if (d == null || !d.toLowerCase(Locale.US).contains("direct")) continue;
            }
            if (refundableOnly && !f.getIsRefundable()) continue;
            if (morningDepartureOnly && !isMorningDeparture(f)) continue;
            if (maxPriceCap > 0 && priceAmount(f) > maxPriceCap) continue;
            
            flightList.add(f);
        }
        
        // Safety Fallback: If 0 matching flights but database is NOT empty
        if (flightList.isEmpty() && rawCount > 0) {
            boolean foundSomething = false;
            for (Flight f : allFlights) {
                if (isActiveFlight(f)) {
                    // Try matching route ignoring date as a backup check
                    String oldDate = searchDate;
                    searchDate = ""; 
                    if (matchesSearchRoute(f)) foundSomething = true;
                    searchDate = oldDate;
                }
            }
            if (foundSomething) {
                Log.d(TAG, "Matches found if date is ignored.");
            }
        }

        Comparator<Flight> cmp;
        switch (sortMode) {
            case DURATION:
                cmp = Comparator.comparingInt(this::durationMinutes);
                break;
            case DEPARTURE:
                cmp = (f1, f2) -> {
                    String t1 = f1.getFromTime();
                    String t2 = f2.getFromTime();
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return compareFlightTimes(t1, t2);
                };
                break;
            case PRICE:
                cmp = Comparator.comparingInt(this::priceAmount);
                break;
            case BEST_VALUE:
            default:
                // REAL-WORLD DEFAULT: Sort by Departure Time ASC first
                cmp = (f1, f2) -> {
                    int timeComp = compareFlightTimes(f1.getFromTime(), f2.getFromTime());
                    if (timeComp != 0) return timeComp;
                    return Integer.compare(priceAmount(f1), priceAmount(f2));
                };
                break;
        }
        flightList.sort(cmp);
        adapter.notifyDataSetChanged();

        boolean empty = flightList.isEmpty();
        if (tvEmptyFlights != null) {
            tvEmptyFlights.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (rvFlights != null) {
            rvFlights.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }
    private boolean isMorningDeparture(Flight f) {
        String t = f.getFromTime();
        if (t == null) return true;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
            Date d = sdf.parse(t.trim());
            if (d == null) return true;
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            return c.get(Calendar.HOUR_OF_DAY) < 12;
        } catch (ParseException e) {
            return true;
        }
    }

    private int priceAmount(Flight f) {
        String p = f.getPrice();
        if (p == null) return Integer.MAX_VALUE;
        try {
            return Integer.parseInt(p.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private int durationMinutes(Flight f) {
        String d = f.getDuration();
        if (d == null) return Integer.MAX_VALUE;
        int mins = 0;
        try {
            String[] parts = d.toLowerCase(Locale.US).replace(" ", "").split("h");
            if (parts.length > 0) {
                mins += Integer.parseInt(parts[0].replaceAll("[^0-9]", "")) * 60;
            }
            if (parts.length > 1) {
                mins += Integer.parseInt(parts[1].replaceAll("[^0-9]", ""));
            }
        } catch (Exception ignored) {
            return Integer.MAX_VALUE;
        }
        return mins;
    }

    private int compareFlightTimes(String t1, String t2) {
        if (t1 == null || t2 == null) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
            Date d1 = sdf.parse(t1.trim());
            Date d2 = sdf.parse(t2.trim());
            if (d1 == null || d2 == null) return 0;
            return d1.compareTo(d2);
        } catch (Exception e) {
            return t1.compareTo(t2);
        }
    }
}
