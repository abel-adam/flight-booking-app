package com.example.flightbooking.util;

import android.os.Bundle;
import androidx.annotation.Nullable;

/**
 * Holds payment-step arguments while the user signs in. Consumed by {@link com.example.flightbooking.MainActivity}.
 */
public final class PendingPaymentCoordinator {

    @Nullable
    private static Bundle pendingArgs;

    private PendingPaymentCoordinator() {}

    public static void setPendingPayment(@Nullable Bundle args) {
        PendingBookingCoordinator.clearPendingBooking();
        pendingArgs = args != null ? new Bundle(args) : null;
    }

    @Nullable
    public static Bundle takePendingPayment() {
        Bundle b = pendingArgs;
        pendingArgs = null;
        return b;
    }

    public static void clear() {
        pendingArgs = null;
    }

    public static boolean hasPendingPayment() {
        return pendingArgs != null;
    }
}
