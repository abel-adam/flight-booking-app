package com.example.flightbooking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.flightbooking.models.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.flightbooking.models.Booking;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FirebaseFirestore db;
    private TextView tvProfileName;
    private TextView tvMemberSince;
    private de.hdodenhof.circleimageview.CircleImageView ivProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            View guest = inflater.inflate(R.layout.fragment_profile_guest, container, false);
            guest.findViewById(R.id.btnGuestSignIn).setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), LoginActivity.class)));
            guest.findViewById(R.id.btnGuestRegister).setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), RegisterActivity.class)));
            return guest;
        }

        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        db = FirebaseFirestore.getInstance();

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvMemberSince = view.findViewById(R.id.tvMemberSince);
        ivProfile = view.findViewById(R.id.ivProfile);
        ImageButton btnEditProfile = view.findViewById(R.id.btnEditProfile);



        View.OnClickListener openEdit = v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ProfileEditFragment())
                .addToBackStack(null)
                .commit();

        btnEditProfile.setOnClickListener(openEdit);
        if (view.findViewById(R.id.btnPersonalSettings) != null) {
            view.findViewById(R.id.btnPersonalSettings).setOnClickListener(openEdit);
        }

        if (view.findViewById(R.id.cardBookings) != null) {
            view.findViewById(R.id.cardBookings).setOnClickListener(v -> {
                BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);
                if (nav != null) {
                    nav.setSelectedItemId(R.id.nav_bookings);
                }
            });
        }

        if (view.findViewById(R.id.btnLogoutAction) != null) {
            view.findViewById(R.id.btnLogoutAction).setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);
        if (nav != null && nav.getSelectedItemId() != R.id.nav_profile) {
            nav.setSelectedItemId(R.id.nav_profile);
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        loadProfileData();
        loadDashboardStats();
    }

    private void loadProfileData() {
        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        if (fu == null || tvProfileName == null || ivProfile == null) return;

        String display = fu.getDisplayName();
        if (display != null && !display.isEmpty()) {
            tvProfileName.setText(display);
        } else {
            tvProfileName.setText("John Doe");
        }

        String uid = fu.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    User u = doc.toObject(User.class);
                    if (u != null) {
                        if (u.getName() != null && !u.getName().isEmpty()) {
                            tvProfileName.setText(u.getName());
                        }
                        if (u.getPhotoUrl() != null && !u.getPhotoUrl().isEmpty()) {
                            Glide.with(this).load(u.getPhotoUrl()).into(ivProfile);
                        } else {
                            Glide.with(this).load(R.drawable.sample_user).into(ivProfile);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Glide.with(this).load(R.drawable.sample_user).into(ivProfile);
                    }
                });
    }

    private void loadDashboardStats() {
        FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
        if (fu == null || getView() == null) return;
        
        db.collection("bookings").whereEqualTo("userId", fu.getUid()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getView() == null) return;
                    
                    int totalFlights = 0;
                    int upcomingCount = 0;
                    Booking nextBooking = null;
                    
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    String todayStr = format.format(new Date());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Booking b = doc.toObject(Booking.class);
                        totalFlights++;
                        
                        String flightDate = b.getFlightDate();
                        if (flightDate == null && b.getFlight() != null) {
                            flightDate = b.getFlight().getDepartureDate();
                        }
                        
                        if (flightDate != null && flightDate.compareTo(todayStr) >= 0) {
                            upcomingCount++;
                            if (nextBooking == null) {
                                nextBooking = b;
                            } else {
                                String nDate = nextBooking.getFlightDate() != null ? nextBooking.getFlightDate() : 
                                        (nextBooking.getFlight() != null ? nextBooking.getFlight().getDepartureDate() : "");
                                if (flightDate.compareTo(nDate) < 0) {
                                    nextBooking = b;
                                }
                            }
                        }
                    }
                    
                    TextView tvTotalFlights = getView().findViewById(R.id.tvTotalFlights);
                    TextView tvUpcomingCount = getView().findViewById(R.id.tvUpcomingCount);
                    if (tvTotalFlights != null) tvTotalFlights.setText(String.valueOf(totalFlights));
                    if (tvUpcomingCount != null) tvUpcomingCount.setText(String.valueOf(upcomingCount));
                    
                    ViewGroup flContainer = getView().findViewById(R.id.flQuickGlanceContainer);
                    if (flContainer != null) {
                        flContainer.removeAllViews();
                        if (nextBooking != null) {
                            View ticketView = getLayoutInflater().inflate(R.layout.item_booking, flContainer, false);
                            populateTicketView(ticketView, nextBooking);
                            flContainer.addView(ticketView);
                        } else {
                            TextView empty = new TextView(getContext());
                            empty.setText("No upcoming trips");
                            empty.setGravity(android.view.Gravity.CENTER);
                            empty.setPadding(24, 24, 24, 24);
                            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
                            empty.setBackgroundResource(R.drawable.card_bg);
                            flContainer.addView(empty);
                        }
                    }
                });
    }

    private void populateTicketView(View view, Booking b) {
        TextView tvAirlineName = view.findViewById(R.id.tvAirlineName);
        TextView tvFlightNumberRow = view.findViewById(R.id.tvFlightNumberRow);
        TextView tvBookingId = view.findViewById(R.id.tvBookingId);
        TextView tvStatus = view.findViewById(R.id.tvStatus);
        TextView tvFromTime = view.findViewById(R.id.tvFromTime);
        TextView tvFromCode = view.findViewById(R.id.tvFromCode);
        TextView tvDate = view.findViewById(R.id.tvDate);
        TextView tvDuration = view.findViewById(R.id.tvDuration);
        TextView tvToTime = view.findViewById(R.id.tvToTime);
        TextView tvToCode = view.findViewById(R.id.tvToCode);
        TextView tvPassenger = view.findViewById(R.id.tvPassenger);
        TextView tvSeat = view.findViewById(R.id.tvSeat);
        TextView tvTravelClass = view.findViewById(R.id.tvTravelClass);
        TextView tvPrice = view.findViewById(R.id.tvPrice);
        ImageView ivAirlineLogo = view.findViewById(R.id.ivAirlineLogo);
        
        view.findViewById(R.id.btnBoardingPass).setVisibility(View.GONE);
        view.findViewById(R.id.btnViewTicket).setVisibility(View.GONE);

        if (b.getFlight() != null) {
            tvAirlineName.setText(b.getFlight().getAirlineName());
            tvFlightNumberRow.setText(b.getFlight().getFlightNumber());
            tvFromTime.setText(b.getFlight().getFromTime());
            tvFromCode.setText(b.getFlight().getFromCode());
            tvDate.setText(b.getFlight().getDepartureDate());
            tvDuration.setText(b.getFlight().getDuration());
            tvToTime.setText(b.getFlight().getToTime());
            tvToCode.setText(b.getFlight().getToCode());
            if (b.getFlight().getAirlineLogo() != null && !b.getFlight().getAirlineLogo().isEmpty()) {
                Glide.with(this).load(b.getFlight().getAirlineLogo()).into(ivAirlineLogo);
            }
        } else {
            tvAirlineName.setText(b.getAirlineName());
            tvFlightNumberRow.setText(b.getFlightNumber());
            tvFromTime.setText(b.getFromTime());
            tvFromCode.setText(b.getFromCode());
            tvDate.setText(b.getFlightDate() != null ? b.getFlightDate() : b.getDate());
            tvToTime.setText(b.getToTime());
            tvToCode.setText(b.getToCode());
        }

        tvBookingId.setText("ID: " + b.getBookingId());
        
        String status = b.getStatus() != null ? b.getStatus() : "Confirmed";
        tvStatus.setText(status);
        if (status.equalsIgnoreCase("Cancelled")) {
            tvStatus.setBackgroundResource(R.drawable.badge_status_cancelled);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red_500));
        } else if (status.equalsIgnoreCase("Completed")) {
            tvStatus.setBackgroundResource(R.drawable.badge_status_completed);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        } else {
            tvStatus.setBackgroundResource(R.drawable.badge_status_confirmed);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_et));
        }

        tvPassenger.setText(b.getPassengerName());
        tvSeat.setText(b.getSeat() != null ? b.getSeat() : "--");
        tvTravelClass.setText(b.getTravelClass() != null ? b.getTravelClass() : "Economy");
        tvPrice.setText(b.getPrice());
    }

}
