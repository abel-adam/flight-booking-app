package com.example.flightbooking;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;


public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("CRASH_DIAGNOSTIC", "SplashActivity: onCreate started");
        
        try {
            setContentView(R.layout.activity_splash);
            android.util.Log.d("CRASH_DIAGNOSTIC", "SplashActivity: layout set");

            View logoContainer = findViewById(R.id.logoContainer);
            View floatingPlane = findViewById(R.id.floatingPlane);
            View tvAppName = findViewById(R.id.tvAppName);

            if (logoContainer == null || tvAppName == null) {
                android.util.Log.e("CRASH_DIAGNOSTIC", "SplashActivity: Critical views not found in layout!");
            }

            // 1. Entry Animation for Logo (Scale 0.5 -> 1, Opacity 0 -> 1)
            if (logoContainer != null) {
                logoContainer.setScaleX(0.5f);
                logoContainer.setScaleY(0.5f);
                logoContainer.setAlpha(0f);
                logoContainer.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(800)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }

            // 2. Entry Animation for Text (Slide up + Fade)
            if (tvAppName != null) {
                tvAppName.setAlpha(0f);
                tvAppName.setTranslationY(50f);
                tvAppName.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(500)
                        .setStartDelay(500)
                        .start();
            }

            // 3. Looping Floating Animation for Small Plane
            if (floatingPlane != null) {
                android.util.Log.d("CRASH_DIAGNOSTIC", "SplashActivity: Starting plane animation");
                startFloatingAnimation(floatingPlane);
            }

            // 4. Start Auth Handshake & timer in parallel
            android.util.Log.d("CRASH_DIAGNOSTIC", "SplashActivity: Initializing Firebase");
            long startTime = System.currentTimeMillis();
            FirebaseAuth auth;
            try {
                auth = FirebaseAuth.getInstance();
            } catch (Exception fatalFirebase) {
                android.util.Log.e("CRASH_DIAGNOSTIC", "SplashActivity: Firebase Auth failed - " + fatalFirebase.getMessage());
                // Fallback: wait 2 seconds and try to proceed to main anyway (will run as guest)
                new Handler(Looper.getMainLooper()).postDelayed(this::startMainActivity, 2000);
                return;
            }
            
            if (auth.getCurrentUser() == null) {
                android.util.Log.d("CRASH_DIAGNOSTIC", "SplashActivity: Starting anonymous sign-in");
                auth.signInAnonymously()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            android.util.Log.d("Auth", "Handshake success: " + (auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "null"));
                        } else {
                            android.util.Log.e("Auth", "Handshake failed", task.getException());
                        }
                        // Move to main activity after the branding duration
                        long elapsed = System.currentTimeMillis() - startTime;
                        long remaining = Math.max(200, 3500 - elapsed);
                        new Handler(Looper.getMainLooper()).postDelayed(this::startMainActivity, remaining);
                    });
            } else {
                android.util.Log.d("Auth", "Already authenticated: " + auth.getCurrentUser().getUid());
                new Handler(Looper.getMainLooper()).postDelayed(this::startMainActivity, 3500);
            }
        } catch (Exception e) {
            android.util.Log.e("CRASH_DIAGNOSTIC", "SplashActivity: Fatal error in onCreate", e);
            // Emergency fallback to MainActivity if possible
            new Handler(Looper.getMainLooper()).postDelayed(this::startMainActivity, 1000);
        }
    }

    private void startMainActivity() {
        if (isFinishing()) return;
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        intent.putExtra("ROLE", "customer");
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }



    private void startFloatingAnimation(View view) {
        float screenWidth = getResources().getDisplayMetrics().widthPixels;
        
        // Start from slightly off-screen left and fly to slightly off-screen right
        view.setTranslationX(-300f);
        ObjectAnimator animX = ObjectAnimator.ofFloat(view, "translationX", -300f, screenWidth + 300f);
        
        // Subtle wave motion in Y
        ObjectAnimator animY = ObjectAnimator.ofFloat(view, "translationY", 100f, -100f, 100f);
        animY.setRepeatCount(ValueAnimator.INFINITE);
        animY.setRepeatMode(ValueAnimator.REVERSE);
        
        ObjectAnimator animRotate = ObjectAnimator.ofFloat(view, "rotation", -5f, 10f, -5f);
        animRotate.setRepeatCount(ValueAnimator.INFINITE);
        animRotate.setRepeatMode(ValueAnimator.REVERSE);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animX, animY, animRotate);
        set.setDuration(3500);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        
        set.start();
    }
}
