package com.example.flightbooking;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.graphics.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;
import com.example.flightbooking.models.Flight;
import com.example.flightbooking.models.Booking;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardFragment extends Fragment {

    private FirebaseFirestore db;
    private List<Flight> allFlights = new ArrayList<>();
    private List<Booking> allBookings = new ArrayList<>();
    private int totalUsers = 0;
    
    private View statRevenue;
    private View statFlights;
    private View statBookings;
    private View statUsers;
    private LinearLayout llOccupancyAlerts;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        db = FirebaseFirestore.getInstance();
        
        statRevenue = view.findViewById(R.id.statRevenue);
        statFlights = view.findViewById(R.id.statFlights);
        statBookings = view.findViewById(R.id.statBookings);
        statUsers = view.findViewById(R.id.statUsers);
        llOccupancyAlerts = view.findViewById(R.id.llOccupancyAlerts);

        Spinner spinner = view.findViewById(R.id.spinnerTimeRange);
        String[] ranges = {"All Time", "Today", "This Week", "This Month"};
        Context context = getContext();
        if (context != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, ranges);
            spinner.setAdapter(adapter);
        }
        
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateDashboard(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Initial setup with "Loading..."
        setupStatCard(statRevenue, R.drawable.ic_ticket, "#F59E0B", "$...", "REVENUE");
        setupStatCard(statFlights, R.drawable.ic_plane, "#3B82F6", "...", "TOTAL FLIGHTS");
        setupStatCard(statBookings, R.drawable.ic_ticket, "#10B981", "...", "ACTIVE BOOKINGS");
        setupStatCard(statUsers, R.drawable.ic_user, "#8B5CF6", "...", "TOTAL USERS");
        
        fetchData();

        // 2. Setup Management Menus
        setupMenuItem(view.findViewById(R.id.menuManageFlights), R.drawable.ic_plane, "Flight Management");
        setupMenuItem(view.findViewById(R.id.menuManageBookings), R.drawable.ic_ticket, "Booking Management");
        setupMenuItem(view.findViewById(R.id.menuManageDestinations), R.drawable.ic_location_pin, "Destination Management");
        setupMenuItem(view.findViewById(R.id.menuManageUsers), R.drawable.ic_user, "User Management");

        // 3. Navigation
        view.findViewById(R.id.menuManageFlights).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new FlightManagementFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.menuManageBookings).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new BookingManagementFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.menuManageDestinations).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new DestinationManagementFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.menuManageUsers).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new UserManagementFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 4. Logout
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            android.content.Intent intent = new android.content.Intent(getActivity(), LoginActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    private void fetchData() {
        db.collection("users").get().addOnSuccessListener(shots -> {
            totalUsers = shots.size();
            updateDashboard(0);
        });
        
        db.collection("flights").addSnapshotListener((value, error) -> {
            if (value != null) {
                allFlights.clear();
                for (com.google.firebase.firestore.DocumentSnapshot d : value.getDocuments()) {
                    Flight f = d.toObject(Flight.class);
                    if (f != null) {
                        f.setId(d.getId());
                        allFlights.add(f);
                    }
                }
                updateDashboard(0);
            }
        });
        
        db.collection("bookings").addSnapshotListener((value, error) -> {
            if (value != null) {
                allBookings.clear();
                for (com.google.firebase.firestore.DocumentSnapshot d : value.getDocuments()) {
                    Booking b = d.toObject(Booking.class);
                    if (b != null) allBookings.add(b);
                }
                updateDashboard(0);
            }
        });
    }

    private void updateDashboard(int timeRangeIndex) {
        if (!isAdded()) return;
        
        int flightsCount = 0;
        int bookingsCount = 0;
        int revenue = 0;
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Calendar cal = Calendar.getInstance();
        String todayStr = sdf.format(cal.getTime());
        cal.add(Calendar.DAY_OF_YEAR, -7);
        String lastWeekStr = sdf.format(cal.getTime());
        cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String thisMonthStr = sdf.format(cal.getTime());
        
        List<Flight> lowOccupancy = new ArrayList<>();
        
        // Filter Flights
        for (Flight f : allFlights) {
            String fd = f.getDepartureDate();
            boolean match = filterDate(fd, timeRangeIndex, todayStr, lastWeekStr, thisMonthStr);
            if (match) {
                flightsCount++;
                int cap = f.getTotalCapacity() > 0 ? f.getTotalCapacity() : 150;
                double occ = (double) f.getBookedSeats() / cap;
                if (occ < 0.1 && fd != null && fd.compareTo(todayStr) >= 0) {
                    lowOccupancy.add(f);
                }
            }
        }
        
        // Filter Bookings
        for (Booking b : allBookings) {
            String bd = b.getDate();
            if (bd == null && b.getFlightDate() != null) bd = b.getFlightDate();
            boolean match = filterDate(bd, timeRangeIndex, todayStr, lastWeekStr, thisMonthStr);
            if (match) {
                bookingsCount++;
                String p = b.getPrice();
                if (p != null) {
                    try {
                        int val = Integer.parseInt(p.replaceAll("[^0-9]", ""));
                        revenue += val;
                    } catch (Exception e) {}
                }
            }
        }
        
        setupStatCard(statRevenue, R.drawable.ic_ticket, "#F59E0B", "$" + revenue, "REVENUE");
        setupStatCard(statFlights, R.drawable.ic_plane, "#3B82F6", String.valueOf(flightsCount), "TOTAL FLIGHTS");
        setupStatCard(statBookings, R.drawable.ic_ticket, "#10B981", String.valueOf(bookingsCount), "ACTIVE BOOKINGS");
        setupStatCard(statUsers, R.drawable.ic_user, "#8B5CF6", String.valueOf(totalUsers), "TOTAL USERS");
        
        populateOccupancyAlerts(lowOccupancy);
    }
    
    private boolean filterDate(String date, int timeRangeIndex, String today, String lastWeek, String thisMonth) {
        if (date == null) return timeRangeIndex == 0;
        switch (timeRangeIndex) {
            case 1: // Today
                return date.equals(today);
            case 2: // This Week
                return date.compareTo(lastWeek) >= 0 && date.compareTo(today) <= 0;
            case 3: // This Month
                return date.compareTo(thisMonth) >= 0;
            case 0: // All Time
            default:
                return true;
        }
    }
    
    private void populateOccupancyAlerts(List<Flight> lowOccupancy) {
        if (llOccupancyAlerts == null || getContext() == null) return;
        llOccupancyAlerts.removeAllViews();
        
        if (lowOccupancy.isEmpty()) {
            TextView tv = new TextView(getContext());
            tv.setText("All upcoming flights have healthy occupancy.");
            tv.setTextColor(Color.parseColor("#6B7280"));
            llOccupancyAlerts.addView(tv);
            return;
        }
        
        for (Flight f : lowOccupancy) {
            TextView tv = new TextView(getContext());
            tv.setText("⚠️ " + f.getFlightNumber() + " (" + f.getFromCode() + " -> " + f.getToCode() + ")\n   Occupancy: " + f.getBookedSeats() + "/" + f.getTotalCapacity());
            tv.setTextColor(Color.parseColor("#EF4444"));
            tv.setBackgroundResource(R.drawable.edittext_bg);
            tv.setPadding(32, 24, 32, 24);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            tv.setLayoutParams(lp);
            llOccupancyAlerts.addView(tv);
        }
    }

    private void setupStatCard(View view, int iconRes, String colorHex, String value, String label) {
        View container = view.findViewById(R.id.flIconContainer);
        ImageView icon = view.findViewById(R.id.ivStatIcon);
        TextView tvValue = view.findViewById(R.id.tvStatValue);
        TextView tvLabel = view.findViewById(R.id.tvStatLabel);

        // Create a dynamic circle background with the specified color
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        shape.setColor(android.graphics.Color.parseColor(colorHex));
        if (container != null) {
            container.setBackground(shape);
        }

        if (icon != null) icon.setImageResource(iconRes);
        if (tvValue != null) tvValue.setText(value);
        if (tvLabel != null) tvLabel.setText(label);
    }

    private void setupMenuItem(View view, int iconRes, String label) {
        ImageView icon = view.findViewById(R.id.ivMenuIcon);
        TextView text = view.findViewById(R.id.tvMenuLabel);
        if (icon != null) icon.setImageResource(iconRes);
        if (text != null) text.setText(label);
    }
}
