package com.example.flightbooking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.flightbooking.R;
import com.example.flightbooking.models.Booking;
import com.example.flightbooking.models.Flight;
import com.example.flightbooking.util.BookingUiUtils;
import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    public interface Listener {
        void openBoardingPass(String documentId);

        void openTicket(String documentId);
    }

    private final List<Booking> bookings;
    private final List<String> documentIds;
    private final Listener listener;

    public BookingAdapter(List<Booking> bookings, List<String> documentIds, Listener listener) {
        this.bookings = bookings;
        this.documentIds = documentIds;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);
        String docId = position < documentIds.size() ? documentIds.get(position) : "";

        String displayId = docId.length() > 10 ? docId.substring(0, 8) + "…" : docId;
        holder.tvBookingId.setText("ID: " + (displayId.isEmpty() ? "—" : displayId));

        String status = BookingUiUtils.normalizeStatus(booking.getStatus());
        holder.tvStatus.setText(status);
        applyStatusStyle(holder.tvStatus, status);

        Flight f = booking.getFlight();
        String fromCode = f != null && f.getFromCode() != null ? f.getFromCode()
                : n(booking.getFromCode());
        String toCode = f != null && f.getToCode() != null ? f.getToCode()
                : n(booking.getToCode());
        holder.tvFromCode.setText(fromCode);
        holder.tvToCode.setText(toCode);

        String fromTime = f != null && f.getFromTime() != null ? f.getFromTime()
                : n(booking.getFromTime());
        String toTime = f != null && f.getToTime() != null ? f.getToTime()
                : n(booking.getToTime());
        holder.tvFromTime.setText(fromTime);
        holder.tvToTime.setText(toTime);

        String duration = f != null && f.getDuration() != null ? f.getDuration() : "—";
        holder.tvDuration.setText(duration);

        String direct = f != null && f.getDirect() != null ? f.getDirect() : "Direct";
        holder.tvDirect.setText(direct);

        String airline = f != null && f.getAirlineName() != null ? f.getAirlineName()
                : n(booking.getAirlineName());
        holder.tvAirlineName.setText(airline.isEmpty() ? "Airline" : airline);

        String fn = f != null && f.getFlightNumber() != null ? f.getFlightNumber()
                : n(booking.getFlightNumber());
        holder.tvFlightNumberRow.setText(fn.isEmpty() ? "" : fn);

        String logo = f != null && f.getAirlineLogo() != null ? f.getAirlineLogo() : null;
        if (logo != null && !logo.isEmpty()) {
            Glide.with(holder.ivAirlineLogo.getContext()).load(logo).into(holder.ivAirlineLogo);
        } else {
            holder.ivAirlineLogo.setImageResource(R.drawable.ic_plane);
        }

        String price = f != null && f.getPrice() != null ? f.getPrice()
                : n(booking.getPrice());
        holder.tvPrice.setText(price.isEmpty() ? "—" : price);

        String pName = n(booking.getPassengerName());
        holder.tvPassenger.setText(pName.isEmpty() ? "—" : pName);

        String seat = n(booking.getSeat());
        holder.tvSeat.setText(seat.isEmpty() ? "—" : seat);

        String tc = n(booking.getTravelClass());
        holder.tvTravelClass.setText(tc.isEmpty() ? "Economy" : tc);

        String fd = booking.getFlightDate() != null ? booking.getFlightDate()
                : (booking.getDate() != null ? booking.getDate() : "");
        holder.tvDate.setText(BookingUiUtils.formatDateHuman(fd));

        holder.btnBoardingPass.setOnClickListener(v -> {
            if (listener != null && !docId.isEmpty()) {
                listener.openBoardingPass(docId);
            }
        });
        holder.btnViewTicket.setOnClickListener(v -> {
            if (listener != null && !docId.isEmpty()) {
                listener.openTicket(docId);
            }
        });

        holder.cardBooking.setOnClickListener(v -> {
            if (listener != null && !docId.isEmpty()) {
                listener.openTicket(docId);
            }
        });
    }

    private static String n(String s) {
        return s != null ? s : "";
    }

    private void applyStatusStyle(TextView tv, String status) {
        int bg;
        int fg;
        switch (status) {
            case "Confirmed":
                bg = R.drawable.badge_status_confirmed;
                fg = R.color.green_500;
                break;
            case "Pending":
                bg = R.drawable.badge_status_pending;
                fg = R.color.amber_500;
                break;
            case "Cancelled":
                bg = R.drawable.badge_status_cancelled;
                fg = R.color.red_500;
                break;
            case "Completed":
                bg = R.drawable.badge_status_completed;
                fg = R.color.blue_500;
                break;
            default:
                bg = R.drawable.badge_status_pending;
                fg = R.color.text_muted;
                break;
        }
        tv.setBackgroundResource(bg);
        tv.setTextColor(ContextCompat.getColor(tv.getContext(), fg));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final androidx.cardview.widget.CardView cardBooking;
        final ImageView ivAirlineLogo;
        final TextView tvAirlineName, tvFlightNumberRow, tvBookingId, tvStatus;
        final TextView tvFromTime, tvToTime, tvFromCode, tvToCode, tvDate;
        final TextView tvDuration, tvDirect;
        final TextView tvPassenger, tvSeat, tvTravelClass, tvPrice;
        final TextView btnViewTicket;
        final View btnBoardingPass;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardBooking = itemView.findViewById(R.id.cardBooking);
            ivAirlineLogo = itemView.findViewById(R.id.ivAirlineLogo);
            tvAirlineName = itemView.findViewById(R.id.tvAirlineName);
            tvFlightNumberRow = itemView.findViewById(R.id.tvFlightNumberRow);
            tvBookingId = itemView.findViewById(R.id.tvBookingId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvFromTime = itemView.findViewById(R.id.tvFromTime);
            tvToTime = itemView.findViewById(R.id.tvToTime);
            tvFromCode = itemView.findViewById(R.id.tvFromCode);
            tvToCode = itemView.findViewById(R.id.tvToCode);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDirect = itemView.findViewById(R.id.tvDirect);
            tvPassenger = itemView.findViewById(R.id.tvPassenger);
            tvSeat = itemView.findViewById(R.id.tvSeat);
            tvTravelClass = itemView.findViewById(R.id.tvTravelClass);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnViewTicket = itemView.findViewById(R.id.btnViewTicket);
            btnBoardingPass = itemView.findViewById(R.id.btnBoardingPass);
        }
    }
}
