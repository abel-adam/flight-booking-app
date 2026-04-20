package com.example.flightbooking;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.flightbooking.adapters.DestinationAdapter;
import com.example.flightbooking.models.Destination;
import com.example.flightbooking.models.User;
import com.example.flightbooking.util.AirportSuggestions;
import com.example.flightbooking.util.GuestSearchSession;
import com.example.flightbooking.util.SavedTripsStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minimal search (From / To) plus popular destinations from Firestore. Next: {@link FlightSearchOptionsFragment}.
 */
public class HomeFragment extends Fragment implements DestinationAdapter.OnDestinationClickListener {

    private FirebaseFirestore db;
    private AutoCompleteTextView etFrom;
    private AutoCompleteTextView etTo;

    private TextView tvDestinationsSectionTitle;
    private List<Destination> destinationList;
    private DestinationAdapter destinationAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        try {
            View view = inflater.inflate(R.layout.fragment_home, container, false);
            return setupFragmentView(view);
        } catch (Exception e) {
            android.util.Log.e("CRASH_DIAGNOSTIC", "HomeFragment: Fatal Error in onCreateView", e);
            // Return a dummy view so we don't crash the activity
            return new View(getContext());
        }
    }

    private View setupFragmentView(View view) {
        db = FirebaseFirestore.getInstance();

        etFrom = view.findViewById(R.id.etFrom);
        etTo = view.findViewById(R.id.etTo);
        
        Context context = getContext();
        if (context != null && etFrom != null && etTo != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_dropdown_item_1line, AirportSuggestions.all());
            etFrom.setAdapter(adapter);
            etTo.setAdapter(adapter);

            // Show all suggestions immediately on click/focus regardless of current text
            etFrom.setOnClickListener(v -> {
                if (etFrom.getAdapter() instanceof android.widget.Filterable) {
                    ((android.widget.Filterable) etFrom.getAdapter()).getFilter().filter(null);
                }
                etFrom.showDropDown();
            });
            etFrom.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (etFrom.getAdapter() instanceof android.widget.Filterable) {
                        ((android.widget.Filterable) etFrom.getAdapter()).getFilter().filter(null);
                    }
                    etFrom.showDropDown();
                }
            });
            etTo.setOnClickListener(v -> {
                if (etTo.getAdapter() instanceof android.widget.Filterable) {
                    ((android.widget.Filterable) etTo.getAdapter()).getFilter().filter(null);
                }
                etTo.showDropDown();
            });
            etTo.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (etTo.getAdapter() instanceof android.widget.Filterable) {
                        ((android.widget.Filterable) etTo.getAdapter()).getFilter().filter(null);
                    }
                    etTo.showDropDown();
                }
            });
        } else {
            android.util.Log.e("CRASH_DIAGNOSTIC", "HomeFragment: Context or views missing during search setup");
        }


        tvDestinationsSectionTitle = view.findViewById(R.id.tvDestinationsSectionTitle);

        RecyclerView rvDest = view.findViewById(R.id.rvDestinations);
        if (rvDest != null) {
            rvDest.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            destinationList = new ArrayList<>();
            destinationAdapter = new DestinationAdapter(destinationList, this);
            rvDest.setAdapter(destinationAdapter);
        } else {
            android.util.Log.e("CRASH_DIAGNOSTIC", "HomeFragment: rvDestinations not found");
        }
        
        try {
            fetchDestinationsFromFirestore();
        } catch (Exception e) {
            android.util.Log.e("CRASH_DIAGNOSTIC", "HomeFragment: Failed to start firestore fetch", e);
            addFallbackDestinations();
        }

        Context ctx = getContext();
        if (ctx != null && GuestSearchSession.hasSavedSearch(ctx)) {
            GuestSearchSession.applyToForm(ctx, new GuestSearchSession.FormTarget() {
                @Override
                public void setFrom(String v) {
                    if (v != null && !v.isEmpty()) etFrom.setText(v);
                }

                @Override
                public void setTo(String v) {
                    if (v != null && !v.isEmpty()) etTo.setText(v);
                }

                @Override
                public void setDate(String v) { }

                @Override
                public void setReturnDate(String v) { }

                @Override
                public void setPassengers(int count) { }

                @Override
                public void setTravelClass(String v) { }

                @Override
                public void setRoundTrip(boolean roundTrip) { }

                @Override
                public void setMultiCity(boolean multiCity) { }
            });
        } else if (etFrom != null && etTo != null) {
            etFrom.setText(AirportSuggestions.defaultFromForLocale(Locale.getDefault()));
            etTo.setText(getString(R.string.sample_to));
        }

        view.findViewById(R.id.btnFillDefaultFrom).setOnClickListener(v ->
                etFrom.setText(AirportSuggestions.defaultFromForLocale(Locale.getDefault())));

        View btnSwap = view.findViewById(R.id.btnSwap);
        if (btnSwap != null) {
            btnSwap.setOnClickListener(v -> {
                if (etFrom != null && etTo != null) {
                    CharSequence tmp = etFrom.getText();
                    etFrom.setText(etTo.getText());
                    etTo.setText(tmp);
                }
            });
        }

        /*
        View btnSignInHeader = view.findViewById(R.id.btnSignInHeader);
        if (btnSignInHeader != null) {
            btnSignInHeader.setOnClickListener(v ->
                    startActivity(new Intent(requireActivity(), LoginActivity.class)));
        }
        */
        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                try {
                    FirebaseAuth.getInstance().signOut();
                    Activity activity = getActivity();
                    if (activity != null) {
                        Intent intent = new Intent(activity, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        activity.finish();
                    }
                } catch (Exception e) {
                    android.util.Log.e("CRASH_DIAGNOSTIC", "HomeFragment: Logout crash", e);
                }
            });
        }

        view.findViewById(R.id.btnSeeAll).setOnClickListener(v ->
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AllDestinationsFragment())
                        .addToBackStack(null)
                        .commitAllowingStateLoss());

        view.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            String from = etFrom.getText().toString().trim();
            String to = etTo.getText().toString().trim();
            if (from.isEmpty()) {
                etFrom.setError(getString(R.string.from_label) + " required");
                return;
            }
            if (to.isEmpty()) {
                etTo.setError(getString(R.string.to_label) + " required");
                return;
            }
            
            List<String> validAirports = AirportSuggestions.all();
            if (!validAirports.contains(from)) {
                etFrom.setError("Please select a valid airport from the dropdown");
                return;
            }
            if (!validAirports.contains(to)) {
                etTo.setError("Please select a valid airport from the dropdown");
                return;
            }
            
            if (getContext() != null) {
                GuestSearchSession.saveHomeRoute(getContext(), from, to);
            }
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, FlightSearchOptionsFragment.newInstance(from, to))
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        });

        updateAuthHeader(view);
        updateDestinationsTitle();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) {
            updateAuthHeader(v);
            updateDestinationsTitle();
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshProfileMenuItem();
        }
    }

    private void updateDestinationsTitle() {
        if (tvDestinationsSectionTitle == null) return;
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        tvDestinationsSectionTitle.setText(u != null
                ? getString(R.string.popular_for_you)
                : getString(R.string.popular_destinations_title));
    }

    private void fetchDestinationsFromFirestore() {
        db.collection("destinations").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (destinationList == null || destinationAdapter == null) return;
                    destinationList.clear();
                    int count = queryDocumentSnapshots.size();
                    android.util.Log.d("HOME_DB", "Fetched " + count + " destinations");
                    
                    if (queryDocumentSnapshots.isEmpty()) {
                        addFallbackDestinations();
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            destinationList.add(document.toObject(Destination.class));
                        }
                    }
                    maybeReorderDestinationsForUser();
                    destinationAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("HOME_DB", "Failed to fetch destinations", e);
                    addFallbackDestinations();
                    maybeReorderDestinationsForUser();
                    destinationAdapter.notifyDataSetChanged();
                });
    }

    private void maybeReorderDestinationsForUser() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || destinationList.size() < 2) {
            return;
        }
        String from = etFrom != null ? etFrom.getText().toString() : "";
        if (from.toUpperCase(Locale.US).contains("ADD")) {
            for (int i = 0; i < destinationList.size(); i++) {
                Destination d = destinationList.get(i);
                if (d.getCity() != null && d.getCity().toLowerCase(Locale.US).contains("dubai")) {
                    destinationList.remove(i);
                    destinationList.add(0, d);
                    break;
                }
            }
        }
    }

    private void addFallbackDestinations() {
        destinationList.add(new Destination("Paris", "France", "$650", "https://images.unsplash.com/photo-1502602898657-3e91760cbb34", 4.8f));
        destinationList.add(new Destination("Dubai", "UAE", "$480", "https://images.unsplash.com/photo-1512453979798-5ea266f8880c", 4.9f));
        destinationList.add(new Destination("Tokyo", "Japan", "$820", "https://images.unsplash.com/photo-1540959733332-e94e270b4d82", 4.7f));
        destinationList.add(new Destination("New York", "USA", "$550", "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9", 4.6f));
    }

    @Override
    public void onDestinationClick(Destination destination) {
        Context ctx = getContext();
        if (ctx == null) return;
        String from = etFrom.getText().toString().trim();
        if (from.isEmpty()) {
            from = AirportSuggestions.defaultFromForLocale(Locale.getDefault());
        }
        String to = destination.getCity() != null ? destination.getCity() : "";
        GuestSearchSession.saveHomeRoute(ctx, from, to);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new FlightSearchOptionsFragment())
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }



    private void updateAuthHeader(View root) {
        if (root == null) return;
        TextView tvGreeting = root.findViewById(R.id.tvGreeting);
        View btnSignIn = null;
        View btnLogout = root.findViewById(R.id.btnLogout);
        if (tvGreeting == null) return;

        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            tvGreeting.setText("Hello, Explorer!");
            if (btnSignIn != null) btnSignIn.setVisibility(View.VISIBLE);
            if (btnLogout != null) btnLogout.setVisibility(View.GONE);
            return;
        }
        if (btnSignIn != null) btnSignIn.setVisibility(View.GONE);
        if (btnLogout != null) btnLogout.setVisibility(View.VISIBLE);

        String display = u.getDisplayName();
        if (display != null && !display.isEmpty()) {
            tvGreeting.setText("Hello, " + firstName(display) + "!");
            return;
        }

        db.collection("users").document(u.getUid()).get()
                .addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    String name = (user != null && user.getName() != null && !user.getName().isEmpty())
                            ? firstName(user.getName())
                            : emailLocalPart(u.getEmail());
                    if (tvGreeting != null) {
                        tvGreeting.setText("Hello, " + name + "!");
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvGreeting != null) {
                        tvGreeting.setText("Hello, " + emailLocalPart(u.getEmail()) + "!");
                    }
                });
    }

    private static String firstName(String full) {
        if (full == null) return "Explorer";
        String t = full.trim();
        int sp = t.indexOf(' ');
        return sp > 0 ? t.substring(0, sp) : t;
    }

    private static String emailLocalPart(String email) {
        if (email == null || !email.contains("@")) return "Explorer";
        return firstName(email.substring(0, email.indexOf('@')).replace('.', ' '));
    }
}
