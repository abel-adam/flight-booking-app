package com.example.flightbooking;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import com.example.flightbooking.models.Flight;
import com.example.flightbooking.util.PendingBookingCoordinator;
import com.example.flightbooking.util.PendingPaymentCoordinator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

public final class BookingFlowDialogs {

    private BookingFlowDialogs() {}

    /** Guest must sign in or register before continuing to passenger booking for a selected flight. */
    public static void openBookingOrGate(Fragment fragment, Flight flight, Runnable openBooking) {
        if (fragment == null || !fragment.isAdded()) {
            android.util.Log.e("BookingFlow", "Cannot open booking gate: Fragment null or not added.");
            return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            android.util.Log.d("BookingFlow", "User is logged in (" + (email != null ? email : "anonymous") + "). Proceeding to booking.");
            if (openBooking != null) {
                openBooking.run();
            }
            return;
        }
        android.util.Log.w("BookingFlow", "No user logged in. Showing auth dialog.");
        showAuthDialog(fragment, R.string.booking_gate_title, R.string.auth_required_select_flight, flight, openBooking);
    }

    /** Guest must sign in or register before payment; payment args are stored only after the user picks Log in or Sign up. */
    public static void promptAuthForPayment(Fragment fragment, Bundle paymentArgs) {
        if (fragment == null || !fragment.isAdded()) {
            android.util.Log.e("BookingFlow", "Cannot prompt auth for payment: Fragment null or not added.");
            return;
        }
        
        Context ctx = fragment.getContext();
        if (ctx == null) {
            android.util.Log.e("BookingFlow", "Cannot prompt auth for payment: Context is null.");
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            android.util.Log.d("BookingFlow", "User already logged in for payment. Ignored prompt.");
            return;
        }

        String[] options = {
                ctx.getString(R.string.booking_gate_login),
                ctx.getString(R.string.booking_gate_signup)
        };
        new MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.booking_gate_title)
                .setMessage(R.string.auth_required_payment)
                .setItems(options, (dialog, which) -> {
                    PendingPaymentCoordinator.setPendingPayment(paymentArgs);
                    if (which == 0) {
                        ctx.startActivity(new Intent(ctx, LoginActivity.class));
                    } else {
                        ctx.startActivity(new Intent(ctx, RegisterActivity.class));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void showAuthDialog(Fragment fragment, @StringRes int title,
                                       @StringRes int message, Flight flight, Runnable openBooking) {
        if (fragment == null || !fragment.isAdded()) return;
        Context ctx = fragment.getContext();
        if (ctx == null) return;
        
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Sign in Required")
                .setMessage("To secure your booking and manage your trips, please sign in or create an account to continue.")
                .setPositiveButton("Log In", (dialog, which) -> {
                    if (flight != null) PendingBookingCoordinator.setPendingBooking(flight);
                    ctx.startActivity(new Intent(ctx, LoginActivity.class));
                })
                .setNeutralButton("Sign Up", (dialog, which) -> {
                    if (flight != null) PendingBookingCoordinator.setPendingBooking(flight);
                    ctx.startActivity(new Intent(ctx, RegisterActivity.class));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
