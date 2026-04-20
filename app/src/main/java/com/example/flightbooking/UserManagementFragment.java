package com.example.flightbooking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.flightbooking.adapters.UserAdapter;
import com.example.flightbooking.models.User;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class UserManagementFragment extends Fragment {

    private RecyclerView rv;
    private UserAdapter adapter;
    private List<User> userList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_management, container, false);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        rv = view.findViewById(R.id.rvAdminUsers);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new UserAdapter(userList);
        rv.setAdapter(adapter);

        fetchUsers();

        return view;
    }

    private void fetchUsers() {
        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                userList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    User user = doc.toObject(User.class);
                    user.setId(doc.getId());
                    // Ensure status is not null
                    if (user.getStatus() == null) user.setStatus("Active");
                    userList.add(user);
                }
                adapter.notifyDataSetChanged();
            }
        });
    }
}
