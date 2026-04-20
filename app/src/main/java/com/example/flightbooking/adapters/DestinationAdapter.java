package com.example.flightbooking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.flightbooking.R;
import com.example.flightbooking.models.Destination;
import com.example.flightbooking.ImageLoader;
import java.util.List;

public class DestinationAdapter extends RecyclerView.Adapter<DestinationAdapter.ViewHolder> {

    public interface OnDestinationClickListener {
        void onDestinationClick(Destination destination);
    }

    private List<Destination> destinations;
    private OnDestinationClickListener listener;

    public DestinationAdapter(List<Destination> destinations, OnDestinationClickListener listener) {
        this.destinations = destinations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_destination, parent, false);
        
        // If parent is a horizontal RecyclerView, set a fixed width
        if (parent.getLayoutParams() != null && parent.getLayoutParams().height != ViewGroup.LayoutParams.MATCH_PARENT) {
            // This is a heuristic, better to pass a flag, but we'll use a standard width for horizontal
            ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp != null && parent instanceof RecyclerView) {
                RecyclerView rv = (RecyclerView) parent;
                if (rv.getLayoutManager() instanceof LinearLayoutManager && 
                    ((LinearLayoutManager) rv.getLayoutManager()).getOrientation() == LinearLayoutManager.HORIZONTAL) {
                    lp.width = (int) (parent.getContext().getResources().getDisplayMetrics().density * 190);
                }
            }
        }
        
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Destination doc = destinations.get(position);
        holder.tvCity.setText(doc.getCity());
        holder.tvCountry.setText(doc.getCountry());
        holder.tvPrice.setText(doc.getPrice());
        holder.tvRating.setText(String.valueOf(doc.getRating()));
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDestinationClick(doc);
            }
        });
        
        // Load image: prefers Base64 (new), falls back to URL (old/fallback)
        ImageLoader.loadDestinationImage(doc.getImageBase64(), doc.getImageUrl(), holder.ivDestination);
    }

    @Override
    public int getItemCount() {
        return destinations.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCity, tvCountry, tvPrice, tvRating;
        ImageView ivDestination;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCity = itemView.findViewById(R.id.tvCity);
            tvCountry = itemView.findViewById(R.id.tvCountry);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvRating = itemView.findViewById(R.id.tvRating);
            ivDestination = itemView.findViewById(R.id.ivDestination);
        }
    }
}
