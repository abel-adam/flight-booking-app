package com.example.flightbooking;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.flightbooking.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Locale;

public class OtpVerificationActivity extends AppCompatActivity {

    private EditText otp1, otp2, otp3, otp4, otp5, otp6;
    private TextView tvResendTimer, btnResend, tvInstruction;
    private ProgressBar progressBar;
    private String email, purpose, generatedOtp;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        email = getIntent().getStringExtra("email");
        purpose = getIntent().getStringExtra("purpose"); // "registration" or "forgot_password"

        initViews();
        setupOtpInputs();
        startResendTimer();

        tvInstruction.setText("We have sent a 6-digit code to\n" + email);
        
        // Generate and send OTP automatically on start
        generateAndSendOtp();

        findViewById(R.id.btnVerify).setOnClickListener(v -> verifyOtp());
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnResend.setOnClickListener(v -> {
            // Resend OTP
            generateAndSendOtp();
            Toast.makeText(this, "Resending OTP to " + email, Toast.LENGTH_SHORT).show();
            startResendTimer();
        });
    }

    private void initViews() {
        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        otp5 = findViewById(R.id.otp5);
        otp6 = findViewById(R.id.otp6);
        tvResendTimer = findViewById(R.id.tvResendTimer);
        btnResend = findViewById(R.id.btnResend);
        tvInstruction = findViewById(R.id.tvInstruction);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupOtpInputs() {
        EditText[] otpFields = {otp1, otp2, otp3, otp4, otp5, otp6};
        for (int i = 0; i < otpFields.length; i++) {
            final int index = i;
            otpFields[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // Handle paste of 6 digits into any field
                    if (s.length() >= 6) {
                        String pastedOTP = s.toString().trim();
                        if (pastedOTP.length() >= 6) {
                            for (int j = 0; j < 6; j++) {
                                otpFields[j].setText(String.valueOf(pastedOTP.charAt(j)));
                            }
                            otpFields[5].requestFocus();
                            verifyOtp(); // Auto-verify on paste
                        }
                    } else if (s.length() == 1 && index < otpFields.length - 1) {
                        otpFields[index + 1].requestFocus();
                    }
                }
                @Override public void afterTextChanged(Editable s) {
                    if (s.length() == 0 && index > 0) {
                        otpFields[index - 1].requestFocus();
                    }
                    // Auto-verify when the last digit is typed manually
                    if (index == 5 && s.length() == 1) {
                        verifyOtp();
                    }
                }
            });
        }
    }

    private void startResendTimer() {
        btnResend.setVisibility(View.GONE);
        tvResendTimer.setVisibility(View.VISIBLE);
        if (countDownTimer != null) countDownTimer.cancel();
        
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvResendTimer.setText(String.format(Locale.getDefault(), "Resend code in 00:%02d", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                tvResendTimer.setVisibility(View.GONE);
                btnResend.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    private void generateAndSendOtp() {
        // Generate random 6-digit OTP
        int randomPin = (int) (Math.random() * 900000) + 100000;
        generatedOtp = String.valueOf(randomPin);

        progressBar.setVisibility(View.VISIBLE);
        String subject = "Your Flight Booking Verification Code";
        String messageBody = "Hello,\n\nYour OTP code is: " + generatedOtp + "\n\nPlease do not share this code with anyone.";

        new com.example.flightbooking.util.EmailUtils(email, subject, messageBody, success -> {
            progressBar.setVisibility(View.GONE);
            if (success) {
                Toast.makeText(this, "OTP sent successfully! Please check your email.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to send OTP to email. Setup valid SMTP info in EmailUtils.", Toast.LENGTH_LONG).show();
                // For demonstration purposes, if mail sending fails (due to dummy credentials), show it in Toast
                Toast.makeText(this, "DEMO FALLBACK OTP: " + generatedOtp, Toast.LENGTH_LONG).show();
            }
        }).execute();
    }

    private void verifyOtp() {
        String enteredOtp = otp1.getText().toString() + otp2.getText().toString() + 
                           otp3.getText().toString() + otp4.getText().toString() + 
                           otp5.getText().toString() + otp6.getText().toString();

        if (enteredOtp.length() < 6) {
            Toast.makeText(this, "Please enter full 6-digit code", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredOtp.equals(generatedOtp)) {
            if ("registration".equals(purpose)) {
                createAccountInFirebase();
            } else if ("forgot_password".equals(purpose)) {
                Intent intent = new Intent(OtpVerificationActivity.this, ResetPasswordActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
                finish();
            }
        } else {
            Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void createAccountInFirebase() {
        progressBar.setVisibility(View.VISIBLE);
        String name = getIntent().getStringExtra("name");
        String password = getIntent().getStringExtra("password");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        String normalizedEmail = email.toLowerCase().trim();
                        String role = "customer"; // Default to customer
                        User user = new User(uid, name, normalizedEmail, role, "Active");

                        db.collection("users").document(uid).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(OtpVerificationActivity.this, "Verification & Registration Successful!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(OtpVerificationActivity.this, MainActivity.class);
                                    intent.putExtra("ROLE", role);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(OtpVerificationActivity.this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(OtpVerificationActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
