package com.example.flightbooking;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.flightbooking.adapters.AdminBookingAdapter;
import com.example.flightbooking.models.Booking;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BookingManagementFragment extends Fragment {

    private FirebaseFirestore db;
    private final List<Booking> bookings = new ArrayList<>();
    private final List<String> docIds = new ArrayList<>();
    private AdminBookingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_management, container, false);
        db = FirebaseFirestore.getInstance();

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        RecyclerView rv = view.findViewById(R.id.rvAdminBookings);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminBookingAdapter(bookings, docIds);
        adapter.setOnBookingActionListener(new AdminBookingAdapter.OnBookingActionListener() {
            @Override
            public void onView(Booking booking, String docId) {
                BookingDetailsAdminFragment detailsFrag =
                        BookingDetailsAdminFragment.newInstance(booking, docId);
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailsFrag)
                        .addToBackStack(null)
                        .commit();
            }

            @Override
            public void onUpdateStatus(Booking booking, String docId, int position) {
                showUpdateStatusDialog(booking, docId, position);
            }
        });
        rv.setAdapter(adapter);

        fetchBookings();
        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchBookings() {
        db.collection("bookings").get()
                .addOnSuccessListener(query -> {
                    bookings.clear();
                    docIds.clear();

                    for (DocumentSnapshot doc : query.getDocuments()) {
                        Booking b = new Booking();

                        // --- Top-level flat fields ---
                        b.setBookingId(str(doc, "bookingId"));
                        b.setPassengerName(str(doc, "passengerName"));
                        b.setStatus(str(doc, "status"));
                        b.setTravelClass(str(doc, "travelClass"));
                        b.setPnrCode(str(doc, "pnrCode"));
                        b.setGate(str(doc, "gate"));
                        b.setSeat(str(doc, "seat"));
                        b.setDate(str(doc, "date"));
                        b.setFlightDate(str(doc, "flightDate"));

                        // Legacy flat fields (older bookings without nested flight)
                        b.setFromCode(str(doc, "fromCode"));
                        b.setToCode(str(doc, "toCode"));
                        b.setPrice(str(doc, "price"));
                        b.setAirlineName(str(doc, "airlineName"));
                        b.setFromTime(str(doc, "fromTime"));

                        // --- Nested flight map ---
                        Object flightObj = doc.get("flight");
                        if (flightObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> fm = (Map<String, Object>) flightObj;

                            com.example.flightbooking.models.Flight f =
                                    new com.example.flightbooking.models.Flight();
                            f.setFromCode(mapStr(fm, "fromCode"));
                            f.setToCode(mapStr(fm, "toCode"));
                            f.setAirlineName(mapStr(fm, "airlineName"));
                            f.setPrice(mapStr(fm, "price"));
                            f.setFromTime(mapStr(fm, "fromTime"));
                            f.setToTime(mapStr(fm, "toTime"));
                            f.setFlightNumber(mapStr(fm, "flightNumber"));
                            f.setAirlineLogo(mapStr(fm, "airlineLogo"));
                            b.setFlight(f);

                            // Also copy to flat legacy fields so adapter always has data
                            if (b.getFromCode() == null || b.getFromCode().isEmpty())
                                b.setFromCode(f.getFromCode());
                            if (b.getToCode() == null || b.getToCode().isEmpty())
                                b.setToCode(f.getToCode());
                            if (b.getAirlineName() == null || b.getAirlineName().isEmpty())
                                b.setAirlineName(f.getAirlineName());
                            if (b.getPrice() == null || b.getPrice().isEmpty())
                                b.setPrice(f.getPrice());
                        }

                        // Default bookingId to docId if missing
                        if (b.getBookingId() == null || b.getBookingId().isEmpty()) {
                            b.setBookingId(doc.getId());
                        }

                        bookings.add(b);
                        docIds.add(doc.getId());
                    }

                    adapter.notifyDataSetChanged();

                    if (bookings.isEmpty()) {
                        Toast.makeText(getContext(), "No bookings found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private String str(DocumentSnapshot doc, String key) {
        Object val = doc.get(key);
        return val != null ? val.toString() : "";
    }

    private String mapStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private void showUpdateStatusDialog(Booking booking, String docId, int position) {
        if (getContext() == null || docId == null || docId.isEmpty()) return;

        String[] statuses = {"Confirmed", "Pending", "Completed", "Cancelled"};
        new AlertDialog.Builder(getContext())
                .setTitle("Update Booking Status")
                .setItems(statuses, (dialog, which) -> {
                    String newStatus = statuses[which];
                    db.collection("bookings").document(docId)
                            .update("status", newStatus)
                            .addOnSuccessListener(aVoid -> {
                                booking.setStatus(newStatus);
                                adapter.notifyItemChanged(position);
                                Toast.makeText(getContext(),
                                        "Status - " + newStatus, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Update failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .show();
    }
}
