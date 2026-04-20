package com.example.flightbooking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.flightbooking.models.Booking;
import com.google.firebase.firestore.FirebaseFirestore;

public class BookingDetailsAdminFragment extends Fragment {

    private static final String ARG_BOOKING_ID = "docId";
    private static final String ARG_PASSENGER  = "passenger";
    private static final String ARG_FROM        = "from";
    private static final String ARG_TO          = "to";
    private static final String ARG_FROM_CITY   = "fromCity";
    private static final String ARG_TO_CITY     = "toCity";
    private static final String ARG_AIRLINE     = "airline";
    private static final String ARG_STATUS      = "status";
    private static final String ARG_CLASS       = "travelClass";
    private static final String ARG_PRICE       = "price";
    private static final String ARG_PNR         = "pnr";
    private static final String ARG_GATE        = "gate";
    private static final String ARG_SEAT        = "seat";
    private static final String ARG_FLIGHT_DATE = "flightDate";
    private static final String ARG_DEPARTURE   = "departure";

    public static BookingDetailsAdminFragment newInstance(Booking booking, String docId) {
        BookingDetailsAdminFragment frag = new BookingDetailsAdminFragment();
        Bundle args = new Bundle();

        args.putString(ARG_BOOKING_ID, docId);
        args.putString(ARG_PASSENGER, booking.getPassengerName());
        args.putString(ARG_STATUS, booking.getStatus());
        args.putString(ARG_CLASS, booking.getTravelClass());
        args.putString(ARG_PNR, booking.getPnrCode());
        args.putString(ARG_GATE, booking.getGate());
        args.putString(ARG_SEAT, booking.getSeat());
        args.putString(ARG_FLIGHT_DATE, booking.getFlightDate());

        if (booking.getFlight() != null) {
            args.putString(ARG_FROM, booking.getFlight().getFromCode());
            args.putString(ARG_TO, booking.getFlight().getToCode());
            args.putString(ARG_AIRLINE, booking.getFlight().getAirlineName());
            args.putString(ARG_PRICE, booking.getFlight().getPrice());
            args.putString(ARG_DEPARTURE, booking.getFlight().getFromTime());
        } else {
            args.putString(ARG_FROM, booking.getFromCode());
            args.putString(ARG_TO, booking.getToCode());
            args.putString(ARG_AIRLINE, booking.getAirlineName());
            args.putString(ARG_PRICE, booking.getPrice());
            args.putString(ARG_DEPARTURE, booking.getFromTime());
        }
        // City names from code (simplified mapping)
        args.putString(ARG_FROM_CITY, cityFor(args.getString(ARG_FROM)));
        args.putString(ARG_TO_CITY,   cityFor(args.getString(ARG_TO)));

        frag.setArguments(args);
        return frag;
    }

    private static String cityFor(String code) {
        if (code == null) return "";
        switch (code.toUpperCase()) {
            case "JFK": return "New York";
            case "LHR": return "London";
            case "CDG": return "Paris";
            case "DXB": return "Dubai";
            case "HND": return "Tokyo";
            case "LAX": return "Los Angeles";
            case "ORD": return "Chicago";
            case "SYD": return "Sydney";
            default: return code;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_details_admin, container, false);
        Bundle args = getArguments();
        if (args == null) return view;

        String docId      = args.getString(ARG_BOOKING_ID, "");
        String passenger  = args.getString(ARG_PASSENGER, "—");
        String fromCode   = args.getString(ARG_FROM, "—");
        String toCode     = args.getString(ARG_TO, "—");
        String fromCity   = args.getString(ARG_FROM_CITY, "");
        String toCity     = args.getString(ARG_TO_CITY, "");
        String airline    = args.getString(ARG_AIRLINE, "—");
        String status     = args.getString(ARG_STATUS, "—");
        String travelClass= args.getString(ARG_CLASS, "Economy");
        String price      = args.getString(ARG_PRICE, "—");
        String pnr        = args.getString(ARG_PNR, "—");
        String gate       = args.getString(ARG_GATE, "—");
        String seat       = args.getString(ARG_SEAT, "—");
        String flightDate = args.getString(ARG_FLIGHT_DATE, "—");
        String departure  = args.getString(ARG_DEPARTURE, "—");

        // Bind views
        setText(view, R.id.tvAirlineName, airline);
        setText(view, R.id.tvStatusBadge, status);
        setText(view, R.id.tvFromCode, fromCode);
        setText(view, R.id.tvFromCity, fromCity);
        setText(view, R.id.tvToCode, toCode);
        setText(view, R.id.tvToCity, toCity);
        setText(view, R.id.tvPassenger, passenger);
        setText(view, R.id.tvPnr, pnr);
        setText(view, R.id.tvClass, travelClass);
        String priceDisplay = (price != null && price.startsWith("$")) ? price : "$" + (price != null ? price : "—");
        setText(view, R.id.tvPrice, priceDisplay);

        // Back
        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        // View Ticket - open E-Ticket fragment
        Button btnViewTicket = view.findViewById(R.id.btnViewTicket);
        btnViewTicket.setOnClickListener(v -> {
            Bundle ticketArgs = new Bundle();
            ticketArgs.putString("passengerName", passenger);
            ticketArgs.putString("airlineName", airline);
            ticketArgs.putString("fromCode", fromCode);
            ticketArgs.putString("toCode", toCode);
            ticketArgs.putString("travelClass", travelClass);
            ticketArgs.putString("pnrCode", pnr);
            ticketArgs.putString("gate", gate);
            ticketArgs.putString("seat", seat);
            ticketArgs.putString("flightDate", flightDate);
            ticketArgs.putString("departure", departure);
            ticketArgs.putString("price", priceDisplay);

            TicketFragment ticketFrag = new TicketFragment();
            ticketFrag.setArguments(ticketArgs);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, ticketFrag)
                    .addToBackStack(null)
                    .commit();
        });

        // Cancel Booking
        Button btnCancel = view.findViewById(R.id.btnCancelBooking);
        if ("Completed".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
            btnCancel.setVisibility(View.GONE);
        } else {
            btnCancel.setVisibility(View.VISIBLE);
            btnCancel.setOnClickListener(v -> {
                if (docId.isEmpty()) {
                    Toast.makeText(getContext(), "Cannot cancel: missing booking ID", Toast.LENGTH_SHORT).show();
                    return;
                }
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Cancel Booking")
                        .setMessage("Are you sure you want to cancel this booking?")
                        .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                            FirebaseFirestore.getInstance()
                                    .collection("bookings")
                                    .document(docId)
                                    .update("status", "Cancelled")
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Booking cancelled", Toast.LENGTH_SHORT).show();
                                        getParentFragmentManager().popBackStack();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(),
                                                    "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("No", null)
                        .show();
            });
        }

        return view;
    }

    private void setText(View parent, int id, String text) {
        TextView tv = parent.findViewById(id);
        if (tv != null) tv.setText(text != null ? text : "—");
    }
}
