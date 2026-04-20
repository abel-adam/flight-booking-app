package com.example.flightbooking;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.flightbooking.models.User;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private EditText etEmail, etPassword;
    private CheckBox cbRememberMe;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Safety Check: If already logged in, skip login screen
        if (mAuth.getCurrentUser() != null) {
            checkUserRole(mAuth.getCurrentUser().getUid());
            return;
        }

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        TextView tvSignUp = findViewById(R.id.tvSignUp);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Load remembered email
        android.content.SharedPreferences prefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String rememberedEmail = prefs.getString("email", "");
        if (!rememberedEmail.isEmpty()) {
            etEmail.setText(rememberedEmail);
            cbRememberMe.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> loginUser());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });
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
                        Log.e("LoginActivity", "Google sign in failed: " + error);
                        Toast.makeText(LoginActivity.this, "Authentication Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkGoogleUserInFirestore(FirebaseUser user) {
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        checkUserRole(user.getUid());
                    } else {
                        // Create new profile for Google user
                        User newUser = new User(user.getUid(), user.getDisplayName(), user.getEmail(), "customer", "Active");
                        db.collection("users").document(user.getUid()).set(newUser)
                                .addOnSuccessListener(aVoid -> checkUserRole(user.getUid()))
                                .addOnFailureListener(e -> {
                                    findViewById(R.id.progressBar).setVisibility(android.view.View.GONE);
                                    Toast.makeText(LoginActivity.this, "Error creating profile", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

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

        // Handle Remember Me
        android.content.SharedPreferences.Editor editor = getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit();
        if (cbRememberMe.isChecked()) {
            editor.putString("email", email);
        } else {
            editor.remove("email");
        }
        editor.apply();

        findViewById(R.id.btnLogin).setEnabled(false);
        findViewById(R.id.progressBar).setVisibility(android.view.View.VISIBLE);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkUserRole(mAuth.getCurrentUser().getUid());
                    } else {
                        findViewById(R.id.btnLogin).setEnabled(true);
                        findViewById(R.id.progressBar).setVisibility(android.view.View.GONE);
                        if (task.getException() instanceof FirebaseAuthInvalidUserException ||
                            task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            etEmail.setError("Incorrect email or password");
                            etPassword.setError("Incorrect email or password");
                        } else if (task.getException() != null) {
                            Toast.makeText(LoginActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void checkUserRole(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        if ("Pending".equals(status)) {
                            // Redirect to OTP verification
                            findViewById(R.id.progressBar).setVisibility(android.view.View.GONE);
                            findViewById(R.id.btnLogin).setEnabled(true);
                            Intent intent = new Intent(LoginActivity.this, OtpVerificationActivity.class);
                            intent.putExtra("email", documentSnapshot.getString("email"));
                            intent.putExtra("purpose", "registration");
                            startActivity(intent);
                            mAuth.signOut(); // Sign out until verified
                            return;
                        } else if ("Blocked".equals(status)) {
                            findViewById(R.id.progressBar).setVisibility(android.view.View.GONE);
                            findViewById(R.id.btnLogin).setEnabled(true);
                            Toast.makeText(this, "Your account has been blocked.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            return;
                        }

                        String role = documentSnapshot.getString("role");
                        navigateToMain(role != null ? role : "customer");
                    } else {
                        // For safety, if document missing but Auth exists (shouldn't happen with normal flow)
                        navigateToMain("customer");
                    }
                })
                .addOnFailureListener(e -> {
                    findViewById(R.id.progressBar).setVisibility(android.view.View.GONE);
                    findViewById(R.id.btnLogin).setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Verification error. Try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToMain(String role) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("ROLE", role);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
