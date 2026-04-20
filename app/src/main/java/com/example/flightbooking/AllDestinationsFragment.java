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
import com.example.flightbooking.util.AirportSuggestions;
import com.example.flightbooking.util.GuestSearchSession;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AllDestinationsFragment extends Fragment implements DestinationAdapter.OnDestinationClickListener {

    private FirebaseFirestore db;
    private List<Destination> destinationList;
    private DestinationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_destinations, container, false);

        db = FirebaseFirestore.getInstance();

        // 1. Back Button
        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // 2. Setup RecyclerView (Vertical)
        RecyclerView rvAll = view.findViewById(R.id.rvAllDestinations);
        rvAll.setLayoutManager(new LinearLayoutManager(getContext()));
        
        destinationList = new ArrayList<>();
        adapter = new DestinationAdapter(destinationList, this);
        rvAll.setAdapter(adapter);

        fetchAllDestinations();

        return view;
    }

    private void fetchAllDestinations() {
        db.collection("destinations").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    destinationList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        addFallbackDestinations();
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Destination destination = document.toObject(Destination.class);
                            destinationList.add(destination);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    addFallbackDestinations();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Loading offline data...", Toast.LENGTH_SHORT).show();
                });
    }

    private void addFallbackDestinations() {
        destinationList.add(new Destination("Paris", "France", "$650", "https://images.unsplash.com/photo-1502602898657-3e91760cbb34", 4.8f));
        destinationList.add(new Destination("Dubai", "UAE", "$480", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c", 4.9f));
        destinationList.add(new Destination("Tokyo", "Japan", "$820", "https://images.unsplash.com/photo-1540959733332-e94e270b4d82", 4.7f));
        destinationList.add(new Destination("Manila", "Philippines", "$520", "https://images.unsplash.com/photo-1524231757912-21f4fe3a7200", 4.5f));
        destinationList.add(new Destination("London", "UK", "$700", "https://images.unsplash.com/photo-1513635269975-59663e001ad4", 4.6f));
    }

    @Override
    public void onDestinationClick(Destination destination) {
        String from = AirportSuggestions.defaultFromForLocale(Locale.getDefault());
        String to = destination.getCity() != null ? destination.getCity() : "";
        GuestSearchSession.saveHomeRoute(requireContext(), from, to);
        FlightSearchOptionsFragment fragment = new FlightSearchOptionsFragment();
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
