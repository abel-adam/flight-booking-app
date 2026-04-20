package com.example.flightbooking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.flightbooking.adapters.BookingAdapter;
import com.example.flightbooking.models.Booking;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyBookingsFragment extends Fragment implements BookingAdapter.Listener {

    private enum BookingTab { UPCOMING, PAST, CANCELLED }

    private enum SortMode { DATE, PRICE, STATUS }

    private FirebaseFirestore db;
    private final List<Booking> allBookings = new ArrayList<>();
    private final List<String> allDocIds = new ArrayList<>();
    private final List<Booking> displayBookings = new ArrayList<>();
    private final List<String> displayDocIds = new ArrayList<>();

    private BookingAdapter adapter;
    private ProgressBar progress;
    private TextView tvError;
    private View layoutEmpty;
    private TextView tvEmptySubtitle;
    private RecyclerView rvBookings;
    private TextView tabUpcoming;
    private TextView tabPast;
    private TextView tabCancelled;
    private TextView btnSortBookings;

    private BookingTab currentTab = BookingTab.UPCOMING;
    private SortMode sortMode = SortMode.DATE;
    private ListenerRegistration registration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_bookings, container, false);
        db = FirebaseFirestore.getInstance();

        progress = view.findViewById(R.id.progressBookings);
        tvError = view.findViewById(R.id.tvBookingsError);
        layoutEmpty = view.findViewById(R.id.layoutEmptyBookings);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);
        rvBookings = view.findViewById(R.id.rvBookings);
        tabUpcoming = view.findViewById(R.id.tabUpcoming);
        tabPast = view.findViewById(R.id.tabPast);
        tabCancelled = view.findViewById(R.id.tabCancelled);
        btnSortBookings = view.findViewById(R.id.btnSortBookings);
        MaterialButton btnBookFirst = view.findViewById(R.id.btnBookFirstFlight);

        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new BookingAdapter(displayBookings, displayDocIds, this);
        rvBookings.setAdapter(adapter);

        tabUpcoming.setOnClickListener(v -> selectTab(BookingTab.UPCOMING));
        tabPast.setOnClickListener(v -> selectTab(BookingTab.PAST));
        tabCancelled.setOnClickListener(v -> selectTab(BookingTab.CANCELLED));

        btnSortBookings.setOnClickListener(this::showSortMenu);

        btnBookFirst.setOnClickListener(v -> {
            if (getActivity() == null) return;
            com.google.android.material.bottomnavigation.BottomNavigationView nav =
                    getActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) {
                nav.setSelectedItemId(R.id.nav_home);
            }
        });

        attachListener();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void attachListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        showLoading(true);
        tvError.setVisibility(View.GONE);

        if (user == null) {
            showLoading(false);
            showSignedOutEmpty();
            return;
        }

        if (registration != null) {
            registration.remove();
        }

        Query q = db.collection("bookings").whereEqualTo("userId", user.getUid());
        registration = q.addSnapshotListener((snapshot, error) -> {
            showLoading(false);
            if (error != null) {
                showError(error.getMessage());
                return;
            }
            allBookings.clear();
            allDocIds.clear();
            if (snapshot != null) {
                for (QueryDocumentSnapshot doc : snapshot) {
                    Booking b = doc.toObject(Booking.class);
                    if (b.getBookingId() == null || b.getBookingId().isEmpty()) {
                        b.setBookingId(doc.getId());
                    }
                    allBookings.add(b);
                    allDocIds.add(doc.getId());
                }
            }
            applyFilterAndSort();
        });
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (loading) {
            rvBookings.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.GONE);
            tvError.setVisibility(View.GONE);
        }
    }

    private void showError(String msg) {
        tvError.setText(msg != null ? msg : getString(R.string.err_something_went_wrong));
        tvError.setVisibility(View.VISIBLE);
        rvBookings.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
    }

    private void showSignedOutEmpty() {
        layoutEmpty.setVisibility(View.VISIBLE);
        tvEmptySubtitle.setText(R.string.empty_bookings_signed_out);
        rvBookings.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void selectTab(BookingTab tab) {
        currentTab = tab;
        int textMuted = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_muted);
        int primaryEt = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary_et);

        tabUpcoming.setBackgroundResource(tab == BookingTab.UPCOMING ? R.drawable.tab_selected : 0);
        tabUpcoming.setTextColor(tab == BookingTab.UPCOMING ? primaryEt : textMuted);
        tabUpcoming.setTypeface(null, tab == BookingTab.UPCOMING ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        tabPast.setBackgroundResource(tab == BookingTab.PAST ? R.drawable.tab_selected : 0);
        tabPast.setTextColor(tab == BookingTab.PAST ? primaryEt : textMuted);
        tabPast.setTypeface(null, tab == BookingTab.PAST ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        tabCancelled.setBackgroundResource(tab == BookingTab.CANCELLED ? R.drawable.tab_selected : 0);
        tabCancelled.setTextColor(tab == BookingTab.CANCELLED ? primaryEt : textMuted);
        tabCancelled.setTypeface(null, tab == BookingTab.CANCELLED ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        applyFilterAndSort();
    }

    private void applyFilterAndSort() {
        displayBookings.clear();
        displayDocIds.clear();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date today = cal.getTime();

        for (int i = 0; i < allBookings.size(); i++) {
            Booking b = allBookings.get(i);
            String id = allDocIds.get(i);
            String st = com.example.flightbooking.util.BookingUiUtils.normalizeStatus(b.getStatus());
            Date flightDate = parseFlightDate(b);

            if (currentTab == BookingTab.CANCELLED) {
                if ("Cancelled".equalsIgnoreCase(st)) {
                    displayBookings.add(b);
                    displayDocIds.add(id);
                }
                continue;
            }

            if ("Cancelled".equalsIgnoreCase(st)) {
                continue;
            }

            if (currentTab == BookingTab.UPCOMING) {
                // Active / future trips only (not completed; date today or later, or unknown date)
                if ("Completed".equalsIgnoreCase(st)) {
                    continue;
                }
                if (flightDate != null && flightDate.before(today)) {
                    continue;
                }
                displayBookings.add(b);
                displayDocIds.add(id);
            } else {
                // Past: completed journeys, or flight date strictly before today
                boolean past = "Completed".equalsIgnoreCase(st)
                        || (flightDate != null && flightDate.before(today));
                if (past) {
                    displayBookings.add(b);
                    displayDocIds.add(id);
                }
            }
        }

        sortDisplayList();
        adapter.notifyDataSetChanged();

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        if (allBookings.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptySubtitle.setText(R.string.empty_bookings_none);
            rvBookings.setVisibility(View.GONE);
            return;
        }
        if (displayBookings.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEmptySubtitle.setText(R.string.empty_bookings_tab);
            rvBookings.setVisibility(View.GONE);
            return;
        }
        layoutEmpty.setVisibility(View.GONE);
        rvBookings.setVisibility(View.VISIBLE);
        rvBookings.scheduleLayoutAnimation();
    }

    private Date parseFlightDate(Booking b) {
        String raw = b.getFlightDate() != null ? b.getFlightDate() : b.getDate();
        if (raw == null) return null;
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return fmt.parse(raw.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private void sortDisplayList() {
        Comparator<Booking> cmp = switch (sortMode) {
            case PRICE -> Comparator.comparingInt(this::priceOf);
            case STATUS ->
                    Comparator.comparing(b -> com.example.flightbooking.util.BookingUiUtils.normalizeStatus(b.getStatus()));
            case DATE ->
                    Comparator.comparing(this::parseFlightDate, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < displayBookings.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> cmp.compare(displayBookings.get(a), displayBookings.get(b)));

        List<Booking> nb = new ArrayList<>();
        List<String> nd = new ArrayList<>();
        for (int i : order) {
            nb.add(displayBookings.get(i));
            nd.add(displayDocIds.get(i));
        }
        displayBookings.clear();
        displayDocIds.clear();
        displayBookings.addAll(nb);
        displayDocIds.addAll(nd);
    }

    private int priceOf(Booking b) {
        String p = b.getFlight() != null && b.getFlight().getPrice() != null ? b.getFlight().getPrice()
                : b.getPrice() != null ? b.getPrice() : "0";
        try {
            return Integer.parseInt(p.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void showSortMenu(View anchor) {
        PopupMenu pm = new PopupMenu(requireContext(), anchor);
        pm.getMenu().add(0, 1, 0, R.string.menu_sort_date);
        pm.getMenu().add(0, 2, 0, R.string.menu_sort_price);
        pm.getMenu().add(0, 3, 0, R.string.menu_sort_status);
        pm.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                sortMode = SortMode.DATE;
                btnSortBookings.setText(R.string.sort_date_label);
            } else if (id == 2) {
                sortMode = SortMode.PRICE;
                btnSortBookings.setText(R.string.sort_price_label);
            } else if (id == 3) {
                sortMode = SortMode.STATUS;
                btnSortBookings.setText(R.string.sort_status_label);
            }
            applyFilterAndSort();
            return true;
        });
        pm.show();
    }

    @Override
    public void openBoardingPass(String documentId) {
        BoardingPassFragment f = new BoardingPassFragment();
        Bundle b = new Bundle();
        b.putString("bookingDocId", documentId);
        f.setArguments(b);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void openTicket(String documentId) {
        TicketFragment f = new TicketFragment();
        Bundle b = new Bundle();
        b.putString("bookingId", documentId);
        f.setArguments(b);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, f)
                .addToBackStack(null)
                .commit();
    }
}
