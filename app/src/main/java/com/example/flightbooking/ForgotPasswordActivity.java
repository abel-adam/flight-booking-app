package com.example.flightbooking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private ProgressBar progressBar;
    private Button btnResetPassword;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etEmail);
        progressBar = findViewById(R.id.progressBar);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnResetPassword.setOnClickListener(v -> startOtpFlow());
    }

    private void startOtpFlow() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnResetPassword.setEnabled(false);

        // First check if user exists
        db.collection("users").whereEqualTo("email", email.toLowerCase()).get()
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                btnResetPassword.setEnabled(true);
                
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    // User exists, send them to OTP screen (OTP is sent automatically there)
                    Intent intent = new Intent(ForgotPasswordActivity.this, OtpVerificationActivity.class);
                    intent.putExtra("email", email.toLowerCase());
                    intent.putExtra("purpose", "forgot_password");
                    startActivity(intent);
                } else {
                    Toast.makeText(ForgotPasswordActivity.this, "No account found with this email.", Toast.LENGTH_LONG).show();
                }
            });
    }
}
