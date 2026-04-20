package com.example.flightbooking;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.flightbooking.models.Booking;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class PaymentFragment extends Fragment {

    private static final String TAG = "PaymentFragment";

    // Payment mode
    private boolean isLocalPassenger = true;

    // Firestore
    private FirebaseFirestore db;

    // Visa/Card views
    private TextInputEditText etCardNumber, etExpiry, etCVV, etCardHolderName;
    private TextView tvCardDisplayNumber, tvCardDisplayHolder, tvCardDisplayExpiry;

    // Telebirr views
    private TextInputEditText etTelebirrPhone, etTelebirrPin;
    private TextInputLayout tilTelebirrPhone, tilTelebirrPin;
    private TextView tvTelebirrDisplayPhone;

    // Summary views
    private TextView tvTicketPrice, tvTaxFees, tvTotalAmount, tvPaymentMethodBadge;

    // Containers
    private LinearLayout llTelebirrContainer, llVisaContainer;

    // Card type toggle
    private MaterialButton btnSelectVisa, btnSelectMastercard;

    // Pricing
    private double totalPrice = 0;

    // Loading dialog
    private android.app.AlertDialog loadingDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_payment, container, false);
        db = FirebaseFirestore.getInstance();

        // Determine passenger type from arguments
        String nationality = "";
        if (getArguments() != null) {
            nationality = getArguments().getString("nationality", "Ethiopian (Local)");
        }
        isLocalPassenger = !("International".equals(nationality));

        // Bind views
        bindViews(view);

        // Show correct payment method UI
        if (isLocalPassenger) {
            showTelebirrUI();
        } else {
            showVisaUI();
        }

        // Wire up back button
        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        // Wire up pay button
        view.findViewById(R.id.btnPayNow).setOnClickListener(v -> processPayment());

        // Prefill card holder name
        if (getArguments() != null && etCardHolderName != null) {
            etCardHolderName.setText(getArguments().getString("passengerName", ""));
        }

        setupRealTimeMockup();
        displayPricingSummary();
        setupCardTypeToggle();

        return view;
    }

    private void bindViews(View view) {
        llTelebirrContainer = view.findViewById(R.id.llTelebirrContainer);
        llVisaContainer = view.findViewById(R.id.llVisaContainer);

        // Telebirr
        etTelebirrPhone = view.findViewById(R.id.etTelebirrPhone);
        etTelebirrPin = view.findViewById(R.id.etTelebirrPin);
        tilTelebirrPhone = view.findViewById(R.id.tilTelebirrPhone);
        tilTelebirrPin = view.findViewById(R.id.tilTelebirrPin);
        tvTelebirrDisplayPhone = view.findViewById(R.id.tvTelebirrDisplayPhone);

        // Visa
        etCardNumber = view.findViewById(R.id.etCardNumber);
        etExpiry = view.findViewById(R.id.etExpiry);
        etCVV = view.findViewById(R.id.etCVV);
        etCardHolderName = view.findViewById(R.id.etCardHolderName);
        tvCardDisplayNumber = view.findViewById(R.id.tvCardDisplayNumber);
        tvCardDisplayHolder = view.findViewById(R.id.tvCardDisplayHolder);
        tvCardDisplayExpiry = view.findViewById(R.id.tvCardDisplayExpiry);

        // Card type buttons
        btnSelectVisa = view.findViewById(R.id.btnSelectVisa);
        btnSelectMastercard = view.findViewById(R.id.btnSelectMastercard);

        // Summary
        tvTicketPrice = view.findViewById(R.id.tvTicketPrice);
        tvTaxFees = view.findViewById(R.id.tvTaxFees);
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount);
        tvPaymentMethodBadge = view.findViewById(R.id.tvPaymentMethodBadge);
    }

    private void showTelebirrUI() {
        llTelebirrContainer.setVisibility(View.VISIBLE);
        llVisaContainer.setVisibility(View.GONE);
        if (tvPaymentMethodBadge != null) {
            tvPaymentMethodBadge.setText("🇪🇹 Telebirr");
        }
        // Live phone preview
        if (etTelebirrPhone != null) {
            etTelebirrPhone.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tvTelebirrDisplayPhone != null) {
                        String val = s.toString().trim();
                        tvTelebirrDisplayPhone.setText(val.isEmpty() ? "+251 *** *** ***" : val);
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void showVisaUI() {
        llTelebirrContainer.setVisibility(View.GONE);
        llVisaContainer.setVisibility(View.VISIBLE);
        if (tvPaymentMethodBadge != null) {
            tvPaymentMethodBadge.setText("💳 Visa / Mastercard");
        }
    }

    private void setupCardTypeToggle() {
        if (btnSelectVisa == null || btnSelectMastercard == null) return;
        btnSelectVisa.setOnClickListener(v -> {
            btnSelectVisa.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)));
            btnSelectVisa.setTextColor(android.graphics.Color.WHITE);
            btnSelectMastercard.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFF1F5F9));
            if (getContext() != null) {
                btnSelectMastercard.setTextColor(
                        androidx.core.content.ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
            if (tvPaymentMethodBadge != null) tvPaymentMethodBadge.setText("💳 Visa");
        });
        btnSelectMastercard.setOnClickListener(v -> {
            btnSelectMastercard.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary)));
            btnSelectMastercard.setTextColor(android.graphics.Color.WHITE);
            btnSelectVisa.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFF1F5F9));
            if (getContext() != null) {
                btnSelectVisa.setTextColor(
                        androidx.core.content.ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
            if (tvPaymentMethodBadge != null) tvPaymentMethodBadge.setText("💳 Mastercard");
        });
    }

    private void setupRealTimeMockup() {
        if (etCardNumber != null) {
            etCardNumber.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tvCardDisplayNumber == null) return;
                    String raw = s.toString().replaceAll(" ", "");
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < raw.length(); i++) {
                        if (i > 0 && i % 4 == 0) formatted.append("  ");
                        formatted.append(raw.charAt(i));
                    }
                    tvCardDisplayNumber.setText(formatted.length() > 0
                            ? formatted.toString() : "****  ****  ****  ****");
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (etCardHolderName != null) {
            etCardHolderName.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tvCardDisplayHolder != null) {
                        tvCardDisplayHolder.setText(s.length() > 0
                                ? s.toString().toUpperCase() : "YOUR NAME");
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
        if (etExpiry != null) {
            etExpiry.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (tvCardDisplayExpiry != null) {
                        tvCardDisplayExpiry.setText(s.length() > 0 ? s.toString() : "MM/YY");
                    }
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void displayPricingSummary() {
        if (getArguments() == null) return;
        String basePriceStr = getArguments().getString("final_price", "0");

        double basePrice = 0;
        try {
            basePrice = Double.parseDouble(basePriceStr.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            Log.e(TAG, "Price parse error: " + basePriceStr, e);
        }

        double tax = 45.00;
        totalPrice = basePrice + tax;

        tvTicketPrice.setText(String.format(Locale.US, "$%.2f", basePrice));
        tvTaxFees.setText(String.format(Locale.US, "$%.2f", tax));
        tvTotalAmount.setText(String.format(Locale.US, "$%.2f", totalPrice));
    }

    private void processPayment() {
        if (isLocalPassenger) {
            processTelebirrPayment();
        } else {
            processVisaPayment();
        }
    }

    private void processTelebirrPayment() {
        if (etTelebirrPhone == null || etTelebirrPin == null) return;

        String phone = etTelebirrPhone.getText() != null
                ? etTelebirrPhone.getText().toString().trim() : "";
        String pin = etTelebirrPin.getText() != null
                ? etTelebirrPin.getText().toString().trim() : "";

        boolean valid = true;
        if (phone.length() < 10) {
            tilTelebirrPhone.setError("Enter a valid phone number");
            valid = false;
        } else {
            tilTelebirrPhone.setError(null);
        }
        if (pin.length() < 4) {
            tilTelebirrPin.setError("PIN must be 4 digits");
            valid = false;
        } else {
            tilTelebirrPin.setError(null);
        }
        if (!valid) return;

        // Logic to handle payment processing

        showLoadingDialog("Processing Telebirr payment…");
        // Simulate network delay then confirm
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            hideLoadingDialog();
            saveBookingToFirestore("Telebirr");
        }, 2000);
    }

    private void processVisaPayment() {
        if (etCardNumber == null || etExpiry == null || etCVV == null || etCardHolderName == null) return;

        String cardNum = etCardNumber.getText() != null
                ? etCardNumber.getText().toString().replaceAll("\\s+", "").trim() : "";
        String expiry = etExpiry.getText() != null
                ? etExpiry.getText().toString().trim() : "";
        String cvv = etCVV.getText() != null
                ? etCVV.getText().toString().trim() : "";
        String holder = etCardHolderName.getText() != null
                ? etCardHolderName.getText().toString().trim() : "";

        boolean isValid = true;

        if (cardNum.length() < 16) {
            etCardNumber.setError("Invalid card number (16 digits required)");
            isValid = false;
        } else {
            etCardNumber.setError(null);
        }

        if (expiry.length() != 5 || !expiry.contains("/")) {
            etExpiry.setError("Invalid expiry (MM/YY)");
            isValid = false;
        } else {
            try {
                String[] parts = expiry.split("/");
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);
                if (month < 1 || month > 12) {
                    etExpiry.setError("Invalid month (01-12)");
                    isValid = false;
                } else {
                    etExpiry.setError(null);
                }
            } catch (Exception e) {
                etExpiry.setError("Invalid format");
                isValid = false;
            }
        }

        if (cvv.length() < 3) {
            etCVV.setError("Invalid CVV (3 digits)");
            isValid = false;
        } else {
            etCVV.setError(null);
        }

        if (holder.length() < 3) {
            etCardHolderName.setError("Valid card holder name required");
            isValid = false;
        } else {
            etCardHolderName.setError(null);
        }

        if (!isValid) return;

        showLoadingDialog("Authorising Visa payment…");
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            hideLoadingDialog();
            saveBookingToFirestore("Visa/Mastercard");
        }, 2000);
    }

    private void saveBookingToFirestore(String paymentMethod) {
        if (getArguments() == null) return;

        String pnrCode = generateRandomString(6);
        String seat = (new Random().nextInt(30) + 1) + "" + (char) ('A' + new Random().nextInt(6));
        String[] gates = {"A-1", "B-12", "C-5", "D-22"};
        String gate = gates[new Random().nextInt(gates.length)];

        String passengerName = getArguments().getString("passengerName", "Passenger");
        String dob = getArguments().getString("dob", "");
        String email = getArguments().getString("email", "");
        String phone = getArguments().getString("phone", "");
        String gender = getArguments().getString("gender", "");
        String travelClass = getArguments().getString("travelClass", "Economy");
        String nationality = getArguments().getString("nationality", "");
        String passportNumber = getArguments().getString("passportNumber", "");
        String passportExpiry = getArguments().getString("passportExpiry", "");
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        com.example.flightbooking.models.Flight flight =
                (com.example.flightbooking.models.Flight) getArguments().getSerializable("selected_flight");

        Booking booking = new Booking();
        FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
        if (authUser != null) booking.setUserId(authUser.getUid());

        booking.setPassengerName(passengerName);
        booking.setDob(dob);
        booking.setEmailAddress(email);
        booking.setPhoneNumber(phone);
        booking.setGender(gender);
        booking.setTravelClass(travelClass);
        booking.setNationality(nationality);
        booking.setPassportNumber(passportNumber);
        booking.setPassportExpiry(passportExpiry);
        booking.setStatus("Confirmed");
        booking.setDate(todayDate);
        booking.setTotalAmount(String.format(Locale.US, "$%.2f", totalPrice));

        if (flight != null) {
            booking.setFlight(flight);
            booking.setFlightDate(flight.getDepartureDate() != null
                    ? flight.getDepartureDate() : todayDate);
        } else {
            booking.setFlightDate(todayDate);
        }
        booking.setGate(gate);
        booking.setSeat(seat);
        booking.setPnrCode(pnrCode);

        db.collection("bookings").add(booking)
                .addOnSuccessListener(documentReference -> {
                    String bookingId = documentReference.getId();
                    documentReference.update("bookingId", bookingId);

                    if (flight != null && flight.getId() != null) {
                        db.collection("flights").document(flight.getId())
                                .update("bookedSeats",
                                        com.google.firebase.firestore.FieldValue.increment(1));
                    }

                    Log.d(TAG, "[" + paymentMethod + "] Booking saved: " + bookingId);
                    showSuccessToast(paymentMethod);
                    navigateToTicket(bookingId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Booking save failed", e);
                    if (getContext() != null) {
                        Toast.makeText(getContext(),
                                "Payment failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showSuccessToast(String method) {
        if (getContext() == null) return;
        Toast.makeText(getContext(),
                "✅ " + method + " payment successful!", Toast.LENGTH_SHORT).show();
    }

    private void navigateToTicket(String bookingId) {
        if (!isAdded() || getParentFragmentManager() == null) return;
        TicketFragment ticketFragment = new TicketFragment();
        Bundle args = new Bundle();
        args.putString("bookingId", bookingId);
        ticketFragment.setArguments(args);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, ticketFragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    private void showLoadingDialog(String message) {
        if (getContext() == null) return;
        try {
            View dialogView = LayoutInflater.from(getContext())
                    .inflate(R.layout.dialog_loading, null);
            loadingDialog = new android.app.AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .setCancelable(false)
                    .create();
            if (loadingDialog.getWindow() != null) {
                loadingDialog.getWindow()
                        .setBackgroundDrawableResource(android.R.color.transparent);
            }
            loadingDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Loading dialog error", e);
        }
    }

    private void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
