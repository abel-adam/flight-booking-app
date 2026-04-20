package com.example.flightbooking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.flightbooking.R;
import com.example.flightbooking.models.Flight;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class AdminFlightAdapter extends RecyclerView.Adapter<AdminFlightAdapter.ViewHolder> {

    public interface OnFlightActionListener {
        void onEdit(Flight flight, int position);
        void onDelete(Flight flight, int position, String docId);
    }

    private final List<Flight> flights;
    private final List<String> docIds;
    private OnFlightActionListener listener;

    public AdminFlightAdapter(List<Flight> flights, List<String> docIds) {
        this.flights = flights;
        this.docIds = docIds;
    }

    public void setOnFlightActionListener(OnFlightActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_flight, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Flight flight = flights.get(position);

        // Route: "JFK - LHR"
        String route = (flight.getFromCode() != null ? flight.getFromCode() : "")
                + " - "
                + (flight.getToCode() != null ? flight.getToCode() : "");
        holder.tvRoute.setText(route);

        // Airline name
        holder.tvAirline.setText(flight.getAirlineName() != null ? flight.getAirlineName() : "");

        // Time: use fromTime
        holder.tvTime.setText(flight.getFromTime() != null ? flight.getFromTime() : "--");

        // Price: prefix $ if not already present
        String price = flight.getPrice() != null ? flight.getPrice() : "0";
        holder.tvPrice.setText(price.startsWith("$") ? price : "$" + price);

        // Status
        String status = flight.getStatus() != null ? flight.getStatus() : "Active";
        holder.tvStatus.setText(status);
        if ("Active".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#22C55E"));
        } else {
            holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#EF4444"));
        }

        // Status toggle on click of status chip parent or status text
        holder.tvStatus.setOnClickListener(v -> {
            String newStatus = "Active".equalsIgnoreCase(flight.getStatus()) ? "Inactive" : "Active";
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("flights").document(flight.getId())
                    .update("status", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        flight.setStatus(newStatus);
                        notifyItemChanged(position);
                    });
        });

        // Load image
        if (flight.getAirlineLogo() != null && !flight.getAirlineLogo().isEmpty()) {
            Glide.with(holder.ivFlightImage.getContext())
                    .load(flight.getAirlineLogo())
                    .placeholder(R.drawable.ic_plane)
                    .error(R.drawable.ic_plane)
                    .into(holder.ivFlightImage);
        } else {
            holder.ivFlightImage.setImageResource(R.drawable.ic_plane);
        }

        // Edit button
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(flight, position);
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                String docId = (docIds != null && position < docIds.size()) ? docIds.get(position) : null;
                listener.onDelete(flight, position, docId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return flights.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivFlightImage;
        TextView tvRoute, tvAirline, tvTime, tvPrice, tvStatus;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFlightImage = itemView.findViewById(R.id.ivFlightImage);
            tvRoute       = itemView.findViewById(R.id.tvRoute);
            tvAirline     = itemView.findViewById(R.id.tvAirline);
            tvTime        = itemView.findViewById(R.id.tvTime);
            tvPrice       = itemView.findViewById(R.id.tvPrice);
            tvStatus      = itemView.findViewById(R.id.tvStatus);
            btnEdit       = itemView.findViewById(R.id.btnEdit);
            btnDelete     = itemView.findViewById(R.id.btnDelete);
        }
    }
}
