package com.example.flightbooking;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        email = getIntent().getStringExtra("email");

        // Trigger the official Firebase Reset Email automatically
        // This will use the SMTP settings you configured in the console.
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Toast.makeText(this, "Error sending link: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });

        Button btnOpenEmail = findViewById(R.id.btnOpenEmail);
        Button btnBackToLogin = findViewById(R.id.btnBackToLogin);

        btnOpenEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_EMAIL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show();
            }
        });

        btnBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
