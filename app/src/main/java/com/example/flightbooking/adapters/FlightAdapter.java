package com.example.flightbooking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.flightbooking.R;
import com.example.flightbooking.models.Flight;
import com.example.flightbooking.ImageLoader;
import com.example.flightbooking.util.AirportDisplayHelper;
import java.util.List;

public class FlightAdapter extends RecyclerView.Adapter<FlightAdapter.ViewHolder> {

    private final List<Flight> flights;
    private final OnFlightClickListener listener;
    private final OnSaveTripListener saveListener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnFlightClickListener {
        void onFlightClick(Flight flight);
    }

    public interface OnSaveTripListener {
        void onSaveTripClick(Flight flight);
    }

    public FlightAdapter(List<Flight> flights, OnFlightClickListener listener, OnSaveTripListener saveListener) {
        this.flights = flights;
        this.listener = listener;
        this.saveListener = saveListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_flight, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Flight flight = flights.get(position);
            holder.tvAirlineName.setText(flight.getAirlineName());
            holder.tvFlightNumber.setText(flight.getFlightNumber());
            holder.tvFromCode.setText(flight.getFromCode());
            holder.tvToCode.setText(flight.getToCode());
            holder.tvFromCity.setText(AirportDisplayHelper.cityNameForCode(flight.getFromCode()));
            holder.tvToCity.setText(AirportDisplayHelper.cityNameForCode(flight.getToCode()));
            holder.tvFromTime.setText(flight.getFromTime());
            holder.tvToTime.setText(flight.getToTime());
            holder.tvPrice.setText(flight.getPrice());
            holder.tvDuration.setText(flight.getDuration());
            holder.tvStops.setText(flight.getStops());
            holder.tvFromTerminal.setText(flight.getTerminalFrom());
            holder.tvToTerminal.setText(flight.getTerminalTo());
            holder.tvBusinessBadge.setText("Business: " + flight.getBusinessPrice());

            if (flight.getIsRefundable()) {
                holder.tvRefundableBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvRefundableBadge.setVisibility(View.GONE);
            }

            // Interactivity for badges
            holder.tvBusinessBadge.setOnClickListener(v -> {
                holder.tvPrice.setText(flight.getBusinessPrice());
                holder.tvBusinessBadge.setAlpha(1.0f);
                holder.tvPrice.setTextColor(ContextCompat.getColor(v.getContext(), R.color.amber_500));
            });

            holder.tvRefundableBadge.setOnClickListener(v -> {
                v.setAlpha(v.getAlpha() == 1.0f ? 0.7f : 1.0f);
            });

            if (flight.getAirlineLogo() != null && !flight.getAirlineLogo().isEmpty()) {
                ImageLoader.load(flight.getAirlineLogo(), holder.ivAirlineLogo);
            } else {
                try {
                    holder.ivAirlineLogo.setImageDrawable(androidx.appcompat.content.res.AppCompatResources.getDrawable(holder.ivAirlineLogo.getContext(), R.drawable.ic_plane));
                } catch (Exception e) {
                    holder.ivAirlineLogo.setImageResource(R.drawable.ic_plane);
                }
            }

            boolean selected = position == selectedPosition;
            int bg = ContextCompat.getColor(holder.cardFlightItem.getContext(),
                    selected ? R.color.flight_card_selected_bg : R.color.white);
            holder.cardFlightItem.setCardBackgroundColor(bg);
            float elev = selected ? 8f : 2f;
            holder.cardFlightItem.setCardElevation(elev);

            holder.cardFlightItem.setOnClickListener(v -> {
                int old = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                if (old != RecyclerView.NO_POSITION) {
                    notifyItemChanged(old);
                }
                if (selectedPosition != RecyclerView.NO_POSITION) {
                    notifyItemChanged(selectedPosition);
                }
            });

            int availableSeats = flight.getTotalCapacity() - flight.getBookedSeats();
            if (flight.getTotalCapacity() > 0 && availableSeats <= 0) {
                if (holder.btnBook instanceof android.widget.Button) {
                    ((android.widget.Button) holder.btnBook).setText("Sold Out");
                }
                holder.btnBook.setEnabled(false);
                holder.btnBook.setAlpha(0.5f);
                holder.btnBook.setOnClickListener(null);
            } else {
                if (holder.btnBook instanceof android.widget.Button) {
                    ((android.widget.Button) holder.btnBook).setText("Book now");
                }
                holder.btnBook.setEnabled(true);
                holder.btnBook.setAlpha(1.0f);
                holder.btnBook.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onFlightClick(flight);
                    }
                });
            }

            holder.btnSaveTrip.setOnClickListener(v -> {
                if (saveListener != null) {
                    saveListener.onSaveTripClick(flight);
                }
            });
        } catch (Exception e) {
            android.util.Log.e("FlightAdapter", "Binding error at pos " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return flights.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final CardView cardFlightItem;
        TextView tvAirlineName, tvFlightNumber, tvFromCode, tvToCode, tvFromCity, tvToCity;
        TextView tvFromTime, tvToTime, tvPrice, tvDuration, tvStops;
        TextView tvBusinessBadge, tvRefundableBadge;
        TextView tvFromTerminal, tvToTerminal;
        ImageView ivAirlineLogo;
        View btnBook;
        ImageView btnSaveTrip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardFlightItem = itemView.findViewById(R.id.cardFlightItem);
            tvAirlineName = itemView.findViewById(R.id.tvAirlineName);
            tvFlightNumber = itemView.findViewById(R.id.tvFlightNumber);
            tvFromCode = itemView.findViewById(R.id.tvFromCode);
            tvToCode = itemView.findViewById(R.id.tvToCode);
            tvFromCity = itemView.findViewById(R.id.tvFromCity);
            tvToCity = itemView.findViewById(R.id.tvToCity);
            tvFromTime = itemView.findViewById(R.id.tvFromTime);
            tvToTime = itemView.findViewById(R.id.tvToTime);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvStops = itemView.findViewById(R.id.tvStops);
            tvBusinessBadge = itemView.findViewById(R.id.tvBusinessBadge);
            tvRefundableBadge = itemView.findViewById(R.id.tvRefundableBadge);
            tvFromTerminal = itemView.findViewById(R.id.tvFromTerminal);
            tvToTerminal = itemView.findViewById(R.id.tvToTerminal);
            ivAirlineLogo = itemView.findViewById(R.id.ivAirlineLogo);
            btnBook = itemView.findViewById(R.id.btnBook);
            btnSaveTrip = itemView.findViewById(R.id.btnSaveTrip);
        }
    }
}
