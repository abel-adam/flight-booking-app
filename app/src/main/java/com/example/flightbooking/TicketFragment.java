package com.example.flightbooking;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.flightbooking.models.Booking;
import com.google.firebase.firestore.FirebaseFirestore;

public class TicketFragment extends Fragment {

    private static final String TAG = "TicketFragment";
    private FirebaseFirestore db;
    private Booking currentBooking;
    
    private TextView tvTicketFromCode, tvTicketToCode, tvTicketFromCity, tvTicketToCity;
    private TextView tvTicketPassengerName, tvTicketFlightDate, tvTicketGate, tvTicketSeat;
    private TextView tvTicketDepartureTime, tvTicketPnrCode, tvClassBadge;
    private TextView tvTicketBoardingTime, tvTicketTerminal;
    private android.widget.ImageView ivTicketQrCode;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ticket, container, false);
        db = FirebaseFirestore.getInstance();

        // 1. Initialize Views
        initViews(view);

        // 2. Fetch Booking Data
        if (getArguments() != null) {
            String bookingId = getArguments().getString("bookingId");
            if (bookingId != null) {
                fetchBookingData(bookingId);
            }
        }

        // 3. Download PDF Button
        view.findViewById(R.id.btnDownloadPdf).setOnClickListener(v -> {
            if (currentBooking != null) {
                PdfGenerator.generateTicketPdf(requireContext(), currentBooking);
            } else {
                Toast.makeText(getContext(), "Ticket data not loaded yet", Toast.LENGTH_SHORT).show();
            }
        });

        // 4. Back to Home Button
        view.findViewById(R.id.btnBackToHome).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        });

        return view;
    }

    private void initViews(View view) {
        tvTicketFromCode = view.findViewById(R.id.tvTicketFromCode);
        tvTicketToCode = view.findViewById(R.id.tvTicketToCode);
        tvTicketFromCity = view.findViewById(R.id.tvTicketFromCity);
        tvTicketToCity = view.findViewById(R.id.tvTicketToCity);
        tvTicketPassengerName = view.findViewById(R.id.tvTicketPassengerName);
        tvTicketFlightDate = view.findViewById(R.id.tvTicketFlightDate);
        tvTicketGate = view.findViewById(R.id.tvTicketGate);
        tvTicketSeat = view.findViewById(R.id.tvTicketSeat);
        tvTicketDepartureTime = view.findViewById(R.id.tvTicketDepartureTime);
        tvTicketPnrCode = view.findViewById(R.id.tvTicketPnrCode);
        tvClassBadge = view.findViewById(R.id.tvClassBadge);
        tvTicketBoardingTime = view.findViewById(R.id.tvTicketBoardingTime);
        tvTicketTerminal = view.findViewById(R.id.tvTicketTerminal);
        ivTicketQrCode = view.findViewById(R.id.ivTicketQrCode);
    }

    private void fetchBookingData(String bookingId) {
        db.collection("bookings").document(bookingId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                currentBooking = documentSnapshot.toObject(Booking.class);
                if (currentBooking != null) {
                    bindTicketData();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching booking", e);
                Toast.makeText(getContext(), "Failed to load ticket", Toast.LENGTH_SHORT).show();
            });
    }

    private void bindTicketData() {
        tvTicketPassengerName.setText(currentBooking.getPassengerName());
        tvTicketFlightDate.setText(currentBooking.getFlightDate());
        tvTicketGate.setText(currentBooking.getGate() != null ? currentBooking.getGate() : "A-1");
        tvTicketSeat.setText(currentBooking.getSeat() != null ? currentBooking.getSeat() : "12A");
        tvTicketPnrCode.setText(currentBooking.getPnrCode());
        tvClassBadge.setText(currentBooking.getTravelClass());
        
        if (currentBooking.getFlight() != null) {
            com.example.flightbooking.models.Flight f = currentBooking.getFlight();
            tvTicketFromCode.setText(f.getFromCode());
            tvTicketToCode.setText(f.getToCode());
            tvTicketDepartureTime.setText(f.getFromTime());
            
            tvTicketFromCity.setText(com.example.flightbooking.util.AirportDisplayHelper.cityNameForCode(f.getFromCode())); 
            tvTicketToCity.setText(com.example.flightbooking.util.AirportDisplayHelper.cityNameForCode(f.getToCode()));

            String bt = f.getBoardingTime();
            if (bt == null || bt.isEmpty()) {
                bt = "45 mins before departure";
            }
            tvTicketBoardingTime.setText(bt);

            String term = f.getTerminalFrom();
            if (term == null || term.isEmpty()) term = "Terminal 2";
            tvTicketTerminal.setText(term);
        }

        // Generate QR Code
        android.graphics.Bitmap qr = com.example.flightbooking.util.QrBitmapUtil.encodeQr(currentBooking.getPnrCode(), 400);
        if (qr != null && ivTicketQrCode != null) {
            ivTicketQrCode.setImageBitmap(qr);
        }
    }
}
