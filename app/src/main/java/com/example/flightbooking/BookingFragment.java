package com.example.flightbooking;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.flightbooking.models.Flight;
import com.example.flightbooking.models.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class BookingFragment extends Fragment {

    private TextInputLayout tilName, tilDOB, tilGender, tilEmail, tilPhone, tilTravelClass;
    private EditText etPassengerName, etDOB, etEmail, etPhone;
    private AutoCompleteTextView actvGender, actvTravelClass, actvCountryCode, actvNationality;
    private TextView tvSummaryFlightNumber, tvSummaryRoute, tvSummaryDate;
    private Flight selectedFlight;
    private CheckBox cbCreateAccountAfter, cbAgreePolicy;
    private MaterialSwitch swUseSavedProfile;
    private MaterialButton btnContinueToPayment;
    private ProgressBar pbSubmitting;
    private TextInputLayout tilPassportNumber, tilPassportExpiry, tilNationality;
    private EditText etPassportNumber, etPassportExpiry;
    private LinearLayout llPassportContainer;
    private LinearLayout llPolicyBody;
    private ImageView ivPolicyChevron;
    private boolean policyExpanded = false;
    private FirebaseFirestore db;
    private User savedUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking, container, false);
        try {
            db = FirebaseFirestore.getInstance();

            if (getArguments() != null) {
                selectedFlight = (Flight) getArguments().getSerializable("selected_flight");
            }

            if (selectedFlight == null) {
                Toast.makeText(getContext(), "Flight data is missing. Please try again.", Toast.LENGTH_SHORT).show();
                return view; 
            }

            initViews(view);
            fillFlightSummary();
            setupDobPicker();
            setupDropdowns();
            setupValidationListeners();

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                fetchUserData(currentUser.getUid());
            }

            if (swUseSavedProfile != null) {
                swUseSavedProfile.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked && savedUser != null) {
                        if (etPassengerName != null) etPassengerName.setText(savedUser.getName());
                        if (etEmail != null) etEmail.setText(savedUser.getEmail());
                    } else if (!isChecked) {
                        if (etPassengerName != null) etPassengerName.setText("");
                        if (etEmail != null) etEmail.setText("");
                    }
                });
            }

            View btnBack = view.findViewById(R.id.btnBack);
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
            }

            // Collapsible policy toggle
            View rlPolicyHeader = view.findViewById(R.id.rlPolicyHeader);
            llPolicyBody = view.findViewById(R.id.llPolicyBody);
            ivPolicyChevron = view.findViewById(R.id.ivPolicyChevron);
            if (rlPolicyHeader != null) {
                rlPolicyHeader.setOnClickListener(v -> {
                    policyExpanded = !policyExpanded;
                    if (llPolicyBody != null) {
                        llPolicyBody.setVisibility(policyExpanded ? View.VISIBLE : View.GONE);
                    }
                    if (ivPolicyChevron != null) {
                        ivPolicyChevron.animate()
                                .rotation(policyExpanded ? 90 : -90)
                                .setDuration(250)
                                .start();
                    }
                });
            }

            if (btnContinueToPayment != null) {
                btnContinueToPayment.setOnClickListener(v -> proceedToPayment());
            }
        } catch (Exception e) {
            Log.e("BookingFragment", "CRITICAL NAVIGATION ERROR", e);
            if (getContext() != null) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                    .setTitle("Component Load Error")
                    .setMessage("Sorry, the booking screen encountered an error: " + e.getMessage() + "\nPlease try again.")
                    .setPositiveButton("Go Back", (d, w) -> { if (isAdded()) getParentFragmentManager().popBackStack(); })
                    .setCancelable(false)
                    .show();
            }
        }

        return view;
    }

    private void initViews(View view) {
        if (view == null) return;
        tilName = view.findViewById(R.id.tilName);
        tilDOB = view.findViewById(R.id.tilDOB);
        tilGender = view.findViewById(R.id.tilGender);
        tilEmail = view.findViewById(R.id.tilEmail);
        tilPhone = view.findViewById(R.id.tilPhone);
        tilTravelClass = view.findViewById(R.id.tilTravelClass);

        etPassengerName = view.findViewById(R.id.etPassengerName);
        etDOB = view.findViewById(R.id.etDOB);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        actvGender = view.findViewById(R.id.actvGender);
        actvTravelClass = view.findViewById(R.id.actvTravelClass);
        actvCountryCode = view.findViewById(R.id.actvCountryCode);
        actvNationality = view.findViewById(R.id.actvNationality);
        
        tilNationality = view.findViewById(R.id.tilNationality);
        tilPassportNumber = view.findViewById(R.id.tilPassportNumber);
        tilPassportExpiry = view.findViewById(R.id.tilPassportExpiry);
        
        etPassportNumber = view.findViewById(R.id.etPassportNumber);
        etPassportExpiry = view.findViewById(R.id.etPassportExpiry);
        
        llPassportContainer = view.findViewById(R.id.llPassportContainer);
        
        cbAgreePolicy = view.findViewById(R.id.cbAgreePolicy);
        
         btnContinueToPayment = view.findViewById(R.id.btnContinueToPayment);
        pbSubmitting = view.findViewById(R.id.pbSubmitting);

        tvSummaryFlightNumber = view.findViewById(R.id.tvSummaryFlightNumber);
        tvSummaryRoute = view.findViewById(R.id.tvSummaryRoute);
        tvSummaryDate = view.findViewById(R.id.tvSummaryDate);
        
        // Ensure button is active from the start
        if (btnContinueToPayment != null) {
            btnContinueToPayment.setEnabled(true);
            btnContinueToPayment.setAlpha(1.0f);
        }
    }

    private void setupDropdowns() {
        Context ctx = getContext();
        if (ctx == null) {
            Log.e("BookingFragment", "setupDropdowns: Context is null.");
            return;
        }
        
        if (actvGender == null || actvTravelClass == null) {
            Log.e("BookingFragment", "setupDropdowns: Dropdown views are null.");
            return;
        }

        String[] genders = {"Male", "Female", "Prefer not to say"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1, genders);
        actvGender.setAdapter(genderAdapter);

        String ep = (selectedFlight != null && selectedFlight.getPrice() != null) ? selectedFlight.getPrice() : "$0";
        String bp = (selectedFlight != null && selectedFlight.getBusinessPrice() != null) ? selectedFlight.getBusinessPrice() : ep;
        String[] classes = { "Economy Class (" + ep + ")", "Business Class (" + bp + ")" };
        ArrayAdapter<String> classAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1, classes);
        actvTravelClass.setAdapter(classAdapter);
        actvTravelClass.setText(classes[0], false);

        // Country Codes
        String[] countryCodes = {
            "+251 (ETH)", "+971 (UAE)", "+1 (USA)", "+44 (UK)", 
            "+254 (KEN)", "+252 (SOM)", "+253 (DJI)", "+249 (SUD)",
            "+256 (UGA)", "+255 (TAN)", "+27 (RSA)", "+234 (NGA)",
            "+20 (EGY)", "+966 (KSA)", "+91 (IND)", "+86 (CHN)"
        };
        ArrayAdapter<String> codeAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1, countryCodes);
        if (actvCountryCode != null) {
            actvCountryCode.setAdapter(codeAdapter);
            actvCountryCode.setText(countryCodes[0], false); // Default to Ethiopia
        }

        // Nationality
        String[] nationalities = {"Ethiopian (Local)", "International"};
        ArrayAdapter<String> nationalityAdapter = new ArrayAdapter<>(ctx, android.R.layout.simple_list_item_1, nationalities);
        if (actvNationality != null) {
            actvNationality.setAdapter(nationalityAdapter);
            actvNationality.setText(nationalities[0], false);
            actvNationality.setOnItemClickListener((parent, view, position, id) -> {
                String nat = nationalities[position];
                if ("International".equals(nat)) {
                    llPassportContainer.setVisibility(View.VISIBLE);
                } else {
                    llPassportContainer.setVisibility(View.GONE);
                }
            });
        }
    }

    private void setupValidationListeners() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { validateForm(); }
        };

        etPassengerName.addTextChangedListener(watcher);
        etDOB.addTextChangedListener(watcher);
        etEmail.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);
        actvGender.addTextChangedListener(watcher);
        actvTravelClass.addTextChangedListener(watcher);
    }

    private void validateForm() {
        // No longer disabling the button in real-time. 
        // We will perform full validation when the user clicks 'Continue'.
        // This makes the UI feel more responsive.
    }

    private boolean validateField(TextInputLayout til, boolean isValid) {
        if (til == null) return isValid;
        if (!isValid) {
            til.setEndIconDrawable(null);
        } else {
            til.setEndIconDrawable(R.drawable.ic_check_circle);
            Context ctx = getContext();
            if (ctx != null) {
                til.setEndIconTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.primary)));
            }
        }
        return isValid;
    }

    private void proceedToPayment() {
        if (selectedFlight == null) {
            Toast.makeText(getContext(), "Selected flight is unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Perform Active Validation
        boolean nameValid = etPassengerName.getText().toString().trim().length() >= 3;
        boolean dobValid = !etDOB.getText().toString().trim().isEmpty();
        boolean emailValid = Patterns.EMAIL_ADDRESS.matcher(etEmail.getText().toString().trim()).matches();
        boolean phoneValid = etPhone.getText().toString().trim().length() >= 9;
        boolean genderValid = !actvGender.getText().toString().isEmpty();
        boolean classValid = !actvTravelClass.getText().toString().isEmpty();
        boolean natValid = !actvNationality.getText().toString().isEmpty();

        boolean passportValid = true;
        if ("International".equals(actvNationality.getText().toString())) {
            String pptNum = etPassportNumber.getText().toString().trim();
            String pptExp = etPassportExpiry.getText().toString().trim();
            if (!pptNum.matches("^[a-zA-Z0-9]+$")) {
                tilPassportNumber.setError("Invalid passport (Alphanumeric only)");
                passportValid = false;
            } else {
                tilPassportNumber.setError(null);
            }
            if (pptExp.isEmpty()) {
                tilPassportExpiry.setError("Expiry required");
                passportValid = false;
            } else {
                // Check if expiry is after departure date
                try {
                    java.util.Date expDate = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).parse(pptExp);
                    java.util.Date depDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(selectedFlight.getDepartureDate());
                    if (expDate != null && depDate != null && expDate.before(depDate)) {
                        tilPassportExpiry.setError("Expiry must be after departure");
                        passportValid = false;
                    } else {
                        tilPassportExpiry.setError(null);
                    }
                } catch (Exception e) {
                    tilPassportExpiry.setError("Invalid date format");
                    passportValid = false;
                }
            }
        } else {
            tilPassportNumber.setError(null);
            tilPassportExpiry.setError(null);
        }

        if (!nameValid) tilName.setError("Full name required (min 3 chars)"); else tilName.setError(null);
        if (!dobValid) tilDOB.setError("Date of birth required"); else tilDOB.setError(null);
        if (!emailValid) tilEmail.setError("Valid email required"); else tilEmail.setError(null);
        if (!phoneValid) tilPhone.setError("Valid phone number required"); else tilPhone.setError(null);
        if (!genderValid) tilGender.setError("Gender selection required"); else tilGender.setError(null);
        if (!classValid) tilTravelClass.setError("Travel class selection required"); else tilTravelClass.setError(null);
        if (!natValid) tilNationality.setError("Nationality required"); else tilNationality.setError(null);

        if (!nameValid || !dobValid || !emailValid || !phoneValid || !genderValid || !classValid || !natValid || !passportValid) {
            Toast.makeText(getContext(), "Please fix the errors above to continue.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cbAgreePolicy != null && !cbAgreePolicy.isChecked()) {
            Toast.makeText(getContext(), "You must agree to the booking terms and policies.", Toast.LENGTH_SHORT).show();
            return;
        }

        pbSubmitting.setVisibility(View.VISIBLE);
        btnContinueToPayment.setEnabled(false);

        Bundle bundle = new Bundle();
        bundle.putString("passengerName", etPassengerName.getText().toString().trim());
        bundle.putString("dob", etDOB.getText().toString().trim());
        bundle.putString("email", etEmail.getText().toString().trim());
        bundle.putString("phone", etPhone.getText().toString().trim());
        bundle.putString("gender", actvGender.getText().toString());
        bundle.putString("nationality", actvNationality.getText().toString());
        
        if ("International".equals(actvNationality.getText().toString())) {
            bundle.putString("passportNumber", etPassportNumber.getText().toString().trim());
            bundle.putString("passportExpiry", etPassportExpiry.getText().toString().trim());
        }
        
        String selectedClass = actvTravelClass.getText().toString();
        bundle.putString("travelClass", selectedClass.contains("Business") ? "Business" : "Economy");
        
        String finalPrice = selectedFlight.getPrice();
        if (selectedClass.contains("Business")) finalPrice = selectedFlight.getBusinessPrice();
        
        bundle.putString("final_price", finalPrice);
        bundle.putSerializable("selected_flight", selectedFlight);
        
        if (getArguments() != null && getArguments().containsKey("leg1_flight")) {
            bundle.putSerializable("leg1_flight", getArguments().getSerializable("leg1_flight"));
        }
        
        pbSubmitting.setVisibility(View.GONE);
        
        PaymentFragment paymentFragment = new PaymentFragment();
        paymentFragment.setArguments(bundle);
        if (isAdded() && getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, paymentFragment)
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        }
    }

    private void fillFlightSummary() {
        if (selectedFlight == null) return;
        if (tvSummaryFlightNumber == null || tvSummaryRoute == null || tvSummaryDate == null) return;

        try {
            String flightNumber = selectedFlight.getFlightNumber() != null ? selectedFlight.getFlightNumber() : "Flight Info";
            String fromCode = selectedFlight.getFromCode() != null ? selectedFlight.getFromCode() : "---";
            String toCode = selectedFlight.getToCode() != null ? selectedFlight.getToCode() : "---";
            String depDate = selectedFlight.getDepartureDate() != null ? selectedFlight.getDepartureDate() : "Date TBD";
            String depTime = selectedFlight.getFromTime() != null ? selectedFlight.getFromTime() : "Time TBD";

            tvSummaryFlightNumber.setText(flightNumber);
            tvSummaryRoute.setText(fromCode + " - " + toCode);
            tvSummaryDate.setText(depDate + " \u2022 " + depTime);
        } catch (Exception e) {
            android.util.Log.e("BookingFragment", "Error filling flight summary", e);
        }
    }

    private void setupDobPicker() {
        etDOB.setOnClickListener(v -> {
            Context ctx = getContext();
            if (ctx == null) return;
            java.util.Calendar c = java.util.Calendar.getInstance();
            int year = c.get(java.util.Calendar.YEAR) - 20; 
            int month = c.get(java.util.Calendar.MONTH);
            int day = c.get(java.util.Calendar.DAY_OF_MONTH);

            android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(ctx,
                    (view, y, m, d) -> {
                        String date = String.format(java.util.Locale.US, "%02d/%02d/%d", d, m + 1, y);
                        etDOB.setText(date);
                    }, year, month, day);
            dpd.getDatePicker().setMaxDate(System.currentTimeMillis());
            dpd.show();
        });

        if (etPassportExpiry != null) {
            etPassportExpiry.setOnClickListener(v -> {
                Context ctx = getContext();
                if (ctx == null) return;
                java.util.Calendar c = java.util.Calendar.getInstance();
                int year = c.get(java.util.Calendar.YEAR); 
                int month = c.get(java.util.Calendar.MONTH);
                int day = c.get(java.util.Calendar.DAY_OF_MONTH);

                android.app.DatePickerDialog dpd = new android.app.DatePickerDialog(ctx,
                        (view, y, m, d) -> {
                            String date = String.format(java.util.Locale.US, "%02d/%02d/%d", d, m + 1, y);
                            etPassportExpiry.setText(date);
                        }, year, month, day);
                dpd.getDatePicker().setMinDate(System.currentTimeMillis());
                dpd.show();
            });
        }
    }

    private void fetchUserData(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded()) return;
                    savedUser = doc.toObject(User.class);
                    // Initial prefetch (Smart Autofill: Name/Email only)
                    if (savedUser != null) {
                        if (etPassengerName != null && etPassengerName.getText().toString().isEmpty()) 
                            etPassengerName.setText(savedUser.getName());
                        if (etEmail != null && etEmail.getText().toString().isEmpty()) 
                            etEmail.setText(savedUser.getEmail());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("BookingFragment", "User fetch fail", e);
                });
    }
}
