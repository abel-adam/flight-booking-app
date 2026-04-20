package com.example.flightbooking.util;

import androidx.annotation.Nullable;
import com.example.flightbooking.models.Flight;

/**
 * Holds a selected flight while the user leaves the app flow to log in or register.
 * Consumed when {@link com.example.flightbooking.MainActivity} starts the booking step.
 */
public final class PendingBookingCoordinator {

    private static Flight pendingFlight;

    private PendingBookingCoordinator() {}

    public static void setPendingBooking(@Nullable Flight flight) {
        PendingPaymentCoordinator.clear();
        pendingFlight = flight;
    }

    public static void clearPendingBooking() {
        pendingFlight = null;
    }

    @Nullable
    public static Flight takePendingBooking() {
        Flight f = pendingFlight;
        pendingFlight = null;
        return f;
    }

    public static boolean hasPendingBooking() {
        return pendingFlight != null;
    }
}
