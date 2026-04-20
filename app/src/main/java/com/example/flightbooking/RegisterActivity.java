package com.example.flightbooking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.flightbooking.models.User;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;
import androidx.annotation.Nullable;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etPassword, etConfirmPassword;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvLogin = findViewById(R.id.tvLogin);

        btnBack.setOnClickListener(v -> finish());
        tvLogin.setOnClickListener(v -> finish());

        String prefillName = getIntent().getStringExtra("prefill_name");
        if (prefillName != null && !prefillName.isEmpty()) {
            etFullName.setText(prefillName);
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnCreateAccount.setOnClickListener(v -> registerUser());
        
        Button btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        if(btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        }
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        findViewById(R.id.progressBar).setVisibility(android.view.View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        checkGoogleUserInFirestore(user);
                    } else {
                        findViewById(R.id.progressBar).setVisibility(android.view.View.GONE);
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e("RegisterActivity", "Google sign in failed: " + error);
                        Toast.makeText(RegisterActivity.this, "Authentication Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkGoogleUserInFirestore(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        navigateToMain(documentSnapshot.getString("role"));
                    } else {
                        User newUser = new User(user.getUid(), user.getDisplayName(), user.getEmail(), "customer", "Active");
                        db.collection("users").document(user.getUid()).set(newUser)
                                .addOnSuccessListener(aVoid -> navigateToMain("customer"))
                                .addOnFailureListener(e -> {
                                    findViewById(R.id.progressBar).setVisibility(android.view.View.GONE);
                                    Toast.makeText(RegisterActivity.this, "Error creating profile", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void navigateToMain(String role) {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.putExtra("ROLE", role != null ? role : "customer");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void registerUser() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etFullName.setError("Name is required");
            return;
        }
        if (name.length() <= 2) {
            etFullName.setError("Name must be more than 2 characters");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        findViewById(R.id.btnCreateAccount).setEnabled(false);

        // Normalize email to lowercase
        String normalizedEmail = email.toLowerCase().trim();

        // Pass all registration data to OTP screen. We will create the Firebase account ONLY after OTP succeeds.
        Intent intent = new Intent(RegisterActivity.this, OtpVerificationActivity.class);
        intent.putExtra("email", normalizedEmail);
        intent.putExtra("password", password);
        intent.putExtra("name", name);
        intent.putExtra("purpose", "registration");
        startActivity(intent);

        // Re-enable button so they can try again if they go back
        findViewById(R.id.btnCreateAccount).setEnabled(true);
    }
}
