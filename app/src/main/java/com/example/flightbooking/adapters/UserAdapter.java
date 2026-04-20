package com.example.flightbooking.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.flightbooking.R;
import com.example.flightbooking.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private List<User> users;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public UserAdapter(List<User> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        String name = user.getName();
        holder.tvUserName.setText(name != null && !name.isEmpty() ? name : "—");
        String email = user.getEmail();
        holder.tvUserEmail.setText(email != null ? email : "—");
        String role = user.getRole();
        holder.tvRole.setText(role != null ? role.toUpperCase() : "CUSTOMER");
        String status = user.getStatus();
        if (status == null || status.isEmpty()) {
            status = "Active";
        }
        holder.tvStatus.setText(status);

        if ("Blocked".equalsIgnoreCase(status)) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_500));
            holder.btnStatusAction.setImageResource(R.drawable.ic_check_green);
            holder.btnStatusAction.setBackgroundResource(R.drawable.circle_action_green);
            holder.btnStatusAction.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.green_500));
        } else {
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.green_500));
            holder.btnStatusAction.setImageResource(R.drawable.ic_close);
            holder.btnStatusAction.setBackgroundResource(R.drawable.circle_action_red);
            holder.btnStatusAction.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.red_500));
        }

        holder.btnStatusAction.setOnClickListener(v -> {
            String cur = user.getStatus() != null ? user.getStatus() : "Active";
            String newStatus = "Blocked".equalsIgnoreCase(cur) ? "Active" : "Blocked";
            db.collection("users").document(user.getId())
                    .update("status", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        user.setStatus(newStatus);
                        notifyItemChanged(position);
                    });
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail, tvRole, tvStatus;
        ImageButton btnStatusAction;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRole = itemView.findViewById(R.id.tvRole);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnStatusAction = itemView.findViewById(R.id.btnStatusAction);
        }
    }
}
