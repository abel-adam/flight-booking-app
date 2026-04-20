package com.example.flightbooking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.flightbooking.adapters.DestinationAdapter;
import com.example.flightbooking.models.Destination;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class DestinationManagementFragment extends Fragment implements DestinationAdapter.OnDestinationClickListener {

    private FirebaseFirestore db;
    private List<Destination> destinationList;
    private DestinationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_destination_management, container, false);

        db = FirebaseFirestore.getInstance();

        // 1. Back Button
        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // 2. Setup RecyclerView
        RecyclerView rv = view.findViewById(R.id.rvManageDestinations);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        destinationList = new ArrayList<>();
        adapter = new DestinationAdapter(destinationList, this);
        rv.setAdapter(adapter);

        // 3. Add Destination FAB
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAddDestination);
        fabAdd.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new AddDestinationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        fetchDestinations();

        return view;
    }

    private void fetchDestinations() {
        db.collection("destinations").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    destinationList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Destination destination = document.toObject(Destination.class);
                        destinationList.add(destination);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error fetching destinations", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestinationClick(Destination destination) {
        // We could implement edit/delete here later
        Toast.makeText(getContext(), "Click 'Add' to upload new data", Toast.LENGTH_SHORT).show();
    }
}
