package com.example.flightbooking;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.flightbooking.databinding.ActivityMainBinding;
import com.example.flightbooking.models.Flight;
import com.example.flightbooking.util.PendingBookingCoordinator;
import com.example.flightbooking.util.PendingPaymentCoordinator;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("CRASH_DIAGNOSTIC", "MainActivity: onCreate started");
        
        try {
            setContentView(R.layout.activity_main);
        } catch (Exception e) {
            android.util.Log.e("CRASH_DIAGNOSTIC", "MainActivity: FATAL INFLATION ERROR", e);
            return;
        }
        android.util.Log.d("CRASH_DIAGNOSTIC", "MainActivity: Layout set successfully");

        // 2. Fragment & Logic Setup
        if (savedInstanceState == null) {
            String role = getIntent().getStringExtra("ROLE");
            if (role == null) role = "customer";
            android.util.Log.d("MAIN_DEBUG", "Received role: " + role);

            try {
                // Secondary Flow logic
                Bundle paymentArgs = PendingPaymentCoordinator.takePendingPayment();
                Flight pending = PendingBookingCoordinator.takePendingBooking();

                if (paymentArgs != null) {
                    PaymentFragment pf = new PaymentFragment();
                    pf.setArguments(paymentArgs);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, pf)
                            .addToBackStack(null)
                            .commitAllowingStateLoss();
                } else if (pending != null) {
                    BookingFragment bf = new BookingFragment();
                    Bundle b = new Bundle();
                    b.putSerializable("selected_flight", pending);
                    bf.setArguments(b);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, bf)
                            .addToBackStack(null)
                            .commitAllowingStateLoss();
                } else if ("admin".equals(role)) {
                    AdminDashboardFragment adf = new AdminDashboardFragment();
                    bottomNavigation = findViewById(R.id.bottom_navigation);
                    if (bottomNavigation != null) bottomNavigation.setVisibility(android.view.View.GONE);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, adf)
                            .commitAllowingStateLoss();
                } else {
                    // Default Customer View
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, new HomeFragment())
                            .commitAllowingStateLoss();
                }
            } catch (Exception e) {
                android.util.Log.e("CRASH_DIAGNOSTIC", "MainActivity: Error in transition logic, falling back to Home", e);
                // Last ditch fallback to Home
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commitAllowingStateLoss();
            }
        }

        try {
            bottomNavigation = findViewById(R.id.bottom_navigation);
            refreshProfileMenuItem();
        } catch (Exception e) {
            android.util.Log.e("CRASH_DIAGNOSTIC", "MainActivity: Failed to refresh profile menu", e);
        }

        if (bottomNavigation != null) {
            bottomNavigation.setOnItemSelectedListener(item -> {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_book) {
                    selectedFragment = new FlightSearchOptionsFragment();
                } else if (itemId == R.id.nav_bookings) {
                    selectedFragment = new MyBookingsFragment(); 
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commitAllowingStateLoss();
                }
                return true;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshProfileMenuItem();
    }

    /** Guest: bottom nav shows "Sign in"; signed-in: "Profile". */
    public void refreshProfileMenuItem() {
        if (bottomNavigation == null) bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation == null) return;
        MenuItem item = bottomNavigation.getMenu().findItem(R.id.nav_profile);
        if (item == null) return;
        
        try {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                item.setTitle(R.string.nav_sign_in);
            } else {
                item.setTitle(R.string.nav_profile_label);
            }
        } catch (Exception firebaseErr) {
            android.util.Log.e("CRASH_DIAGNOSTIC", "MainActivity: Firebase error in refreshProfileMenu", firebaseErr);
            item.setTitle("Account");
        }
    }
}
