package com.example.flightbooking.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.flightbooking.R;
import com.example.flightbooking.models.Booking;
import java.util.List;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.ViewHolder> {

    public interface OnBookingActionListener {
        void onView(Booking booking, String docId);
        void onUpdateStatus(Booking booking, String docId, int position);
    }

    private final List<Booking> bookings;
    private final List<String> docIds;
    private OnBookingActionListener listener;

    public AdminBookingAdapter(List<Booking> bookings, List<String> docIds) {
        this.bookings = bookings;
        this.docIds = docIds;
    }

    public void setOnBookingActionListener(OnBookingActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        String docId = (docIds != null && position < docIds.size()) ? docIds.get(position) : null;

        // Booking ID — show short form
        String rawId = safe(booking.getBookingId(), safe(docId, "N/A"));
        String shortId = rawId.length() > 8 ? rawId.substring(0, 8) : rawId;
        holder.tvBookingId.setText("ID: " + shortId);

        // Status badge with dynamic color
        String status = safe(booking.getStatus(), "Pending");
        holder.tvStatus.setText(status);
        applyStatusStyle(holder.tvStatus, status);

        // Passenger name (no "Passenger:" prefix — match design)
        holder.tvPassenger.setText(safe(booking.getPassengerName(), "Unknown"));

        // Route + date: "JFK - LHR • 2026-05-15"
        String from = "";
        String to = "";
        if (booking.getFlight() != null) {
            from = safe(booking.getFlight().getFromCode(), "");
            to   = safe(booking.getFlight().getToCode(), "");
        } else {
            from = safe(booking.getFromCode(), "");
            to   = safe(booking.getToCode(), "");
        }
        String date = safe(booking.getFlightDate(), safe(booking.getDate(), ""));
        String routeText = from + " - " + to + (date.isEmpty() ? "" : " • " + date);
        holder.tvRoute.setText(routeText);

        // View
        holder.btnView.setOnClickListener(v -> {
            if (listener != null) listener.onView(booking, docId);
        });

        // Update Status
        holder.btnUpdate.setOnClickListener(v -> {
            if (listener != null) listener.onUpdateStatus(booking, docId, position);
        });
    }

    private void applyStatusStyle(TextView tv, String status) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(50f);

        switch (status) {
            case "Confirmed":
                bg.setColor(Color.parseColor("#F0FDF4"));
                tv.setTextColor(Color.parseColor("#22C55E"));
                break;
            case "Completed":
                bg.setColor(Color.parseColor("#EFF6FF"));
                tv.setTextColor(Color.parseColor("#3B82F6"));
                break;
            case "Cancelled":
                bg.setColor(Color.parseColor("#FEF2F2"));
                tv.setTextColor(Color.parseColor("#EF4444"));
                break;
            default: // Pending
                bg.setColor(Color.parseColor("#FFFBEB"));
                tv.setTextColor(Color.parseColor("#F59E0B"));
                break;
        }
        tv.setBackground(bg);
    }

    private String safe(String val, String fallback) {
        return (val != null && !val.isEmpty()) ? val : fallback;
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingId, tvStatus, tvPassenger, tvRoute;
        Button btnView, btnUpdate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvStatus    = itemView.findViewById(R.id.tvStatus);
            tvPassenger = itemView.findViewById(R.id.tvPassenger);
            tvRoute     = itemView.findViewById(R.id.tvRoute);
            btnView     = itemView.findViewById(R.id.btnView);
            btnUpdate   = itemView.findViewById(R.id.btnUpdate);
        }
    }
}
