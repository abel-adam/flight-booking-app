package com.example.flightbooking;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.flightbooking.util.AirportDisplayHelper;
import com.example.flightbooking.util.AirportSuggestions;
import com.example.flightbooking.util.GuestSearchSession;
import java.util.Calendar;
import java.util.Locale;

/**
 * Trip options after home route: one-way, round-trip (return required, no default), or multi-city (two segments).
 */
public class FlightSearchOptionsFragment extends Fragment {

    private static final int MODE_ONE_WAY = 0;
    private static final int MODE_ROUND = 1;
    private static final int MODE_MULTI = 2;

    public static FlightSearchOptionsFragment newInstance(String from, String to) {
        FlightSearchOptionsFragment fragment = new FlightSearchOptionsFragment();
        Bundle args = new Bundle();
        args.putString("fromCity", from);
        args.putString("toCity", to);
        fragment.setArguments(args);
        return fragment;
    }

    private int tripMode = MODE_ONE_WAY;
    private int passengerCount = 1;
    private Calendar calendar;
    private TextView tvDate;
    private TextView tvReturnDate;
    private TextView tabOneWay;
    private TextView tabRoundTrip;
    private TextView tabMultiCity;
    private View layoutReturnDateContainer;
    private View layoutMcLeg1Summary;
    private View layoutMcLeg2Block;
    private TextView tvMcLeg1Route;
    private TextView tvDepartureDateLabel;
    private TextView tvTravelClass;
    private TextView tvPassengerCount;
    private TextView tvMc2Date;
    private AutoCompleteTextView etMc2From;
    private AutoCompleteTextView etMc2To;
    private String fromCity = "";
    private String toCity = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flight_search_options, container, false);
        calendar = Calendar.getInstance();

        tabOneWay = view.findViewById(R.id.tabOneWay);
        tabRoundTrip = view.findViewById(R.id.tabRoundTrip);
        tabMultiCity = view.findViewById(R.id.tabMultiCity);
        layoutReturnDateContainer = view.findViewById(R.id.layoutReturnDateContainer);
        layoutMcLeg1Summary = view.findViewById(R.id.layoutMcLeg1Summary);
        layoutMcLeg2Block = view.findViewById(R.id.layoutMcLeg2Block);
        tvMcLeg1Route = view.findViewById(R.id.tvMcLeg1Route);
        tvDepartureDateLabel = view.findViewById(R.id.tvDepartureDateLabel);
        tvDate = view.findViewById(R.id.tvDate);
        tvReturnDate = view.findViewById(R.id.tvReturnDate);
        tvTravelClass = view.findViewById(R.id.tvTravelClass);
        tvMc2Date = view.findViewById(R.id.tvMc2Date);
        etMc2From = view.findViewById(R.id.etMc2From);
        etMc2To = view.findViewById(R.id.etMc2To);

        Context ctx = getContext();
        if (ctx != null) {
            ArrayAdapter<String> airportAdapter = new ArrayAdapter<>(ctx,
                    android.R.layout.simple_dropdown_item_1line, AirportSuggestions.all());
            etMc2From.setAdapter(airportAdapter);
            etMc2To.setAdapter(airportAdapter);

            // Show suggestions immediately on click/focus
            etMc2From.setOnClickListener(v -> {
                if (etMc2From.getAdapter() instanceof android.widget.Filterable) {
                    ((android.widget.Filterable) etMc2From.getAdapter()).getFilter().filter(null);
                }
                etMc2From.showDropDown();
            });
            etMc2From.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (etMc2From.getAdapter() instanceof android.widget.Filterable) {
                        ((android.widget.Filterable) etMc2From.getAdapter()).getFilter().filter(null);
                    }
                    etMc2From.showDropDown();
                }
            });
            etMc2To.setOnClickListener(v -> {
                if (etMc2To.getAdapter() instanceof android.widget.Filterable) {
                    ((android.widget.Filterable) etMc2To.getAdapter()).getFilter().filter(null);
                }
                etMc2To.showDropDown();
            });
            etMc2To.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (etMc2To.getAdapter() instanceof android.widget.Filterable) {
                        ((android.widget.Filterable) etMc2To.getAdapter()).getFilter().filter(null);
                    }
                    etMc2To.showDropDown();
                }
            });
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        tabOneWay.setOnClickListener(v -> selectTripMode(MODE_ONE_WAY, false));
        tabRoundTrip.setOnClickListener(v -> selectTripMode(MODE_ROUND, false));
        tabMultiCity.setOnClickListener(v -> selectTripMode(MODE_MULTI, false));

        tvPassengerCount = view.findViewById(R.id.tvPassengerCount);
        view.findViewById(R.id.btnIncrease).setOnClickListener(v -> {
            passengerCount++;
            tvPassengerCount.setText(String.valueOf(passengerCount));
        });
        view.findViewById(R.id.btnDecrease).setOnClickListener(v -> {
            if (passengerCount > 1) {
                passengerCount--;
                tvPassengerCount.setText(String.valueOf(passengerCount));
            }
        });

        view.findViewById(R.id.layoutDate).setOnClickListener(v -> showDatePicker(tvDate));
        view.findViewById(R.id.layoutReturnDate).setOnClickListener(v -> showDatePicker(tvReturnDate));
        view.findViewById(R.id.layoutMc2Date).setOnClickListener(v -> showDatePicker(tvMc2Date));

        view.findViewById(R.id.layoutTravelClass).setOnClickListener(v -> {
            Context popupCtx = getContext();
            if (popupCtx == null) return;
            android.widget.PopupMenu popup = new android.widget.PopupMenu(popupCtx, v);
            popup.getMenu().add("Economy");
            popup.getMenu().add("Business");
            popup.setOnMenuItemClickListener(item -> {
                tvTravelClass.setText(item.getTitle());
                return true;
            });
            popup.show();
        });

        mergeArgumentsIntoSession();
        loadFromSession();
        selectTripMode(tripMode, true);

        view.findViewById(R.id.btnFindFlights).setOnClickListener(v -> findFlights());

        return view;
    }

    private void mergeArgumentsIntoSession() {
        Bundle args = getArguments();
        if (args == null) return;
        String t = args.getString("toCity");
        String f = args.getString("fromCity");
        if (t != null && !t.isEmpty() && f != null && !f.isEmpty()) {
            if (getContext() != null) {
                GuestSearchSession.saveHomeRoute(getContext(), f, t);
            }
        }
    }

    private void loadFromSession() {
        Context currentCtx = getContext();
        if (currentCtx == null) return;
        GuestSearchSession.applyToForm(currentCtx, new GuestSearchSession.FormTarget() {
            @Override
            public void setFrom(String v) {
                if (v != null) fromCity = v;
            }

            @Override
            public void setTo(String v) {
                if (v != null) toCity = v;
            }

            @Override
            public void setDate(String v) {
                if (v != null && !v.isEmpty()) {
                    tvDate.setText(v);
                    tvDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
                }
            }

            @Override
            public void setReturnDate(String v) {
                if (v != null && !v.isEmpty() && !isReturnPlaceholderText(v.toString())) {
                    tvReturnDate.setText(v);
                    tvReturnDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
                }
            }

            @Override
            public void setPassengers(int count) {
                if (count > 0) {
                    passengerCount = count;
                    if (tvPassengerCount != null) {
                        tvPassengerCount.setText(String.valueOf(count));
                    }
                }
            }

            @Override
            public void setTravelClass(String v) {
                if (v != null && !v.isEmpty()) tvTravelClass.setText(v);
            }

            @Override
            public void setRoundTrip(boolean roundTrip) {
                tripMode = roundTrip ? MODE_ROUND : MODE_ONE_WAY;
            }

            @Override
            public void setMultiCity(boolean multiCity) {
                if (multiCity) tripMode = MODE_MULTI;
            }
        });

        String mc2f = GuestSearchSession.getMultiLeg2From(requireContext());
        String mc2t = GuestSearchSession.getMultiLeg2To(requireContext());
        String mc2d = GuestSearchSession.getMultiLeg2Date(requireContext());
        if (mc2f != null) etMc2From.setText(mc2f);
        if (mc2t != null) etMc2To.setText(mc2t);
        if (mc2d != null && !mc2d.isEmpty()) {
            tvMc2Date.setText(mc2d);
            tvMc2Date.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
        }

        if (tvDate.getText() == null || tvDate.getText().length() == 0) {
            setDefaultDepartureDate();
        }

        if (fromCity.isEmpty()) {
            fromCity = getString(R.string.sample_from);
        }
        if (toCity.isEmpty()) {
            toCity = getString(R.string.sample_to);
        }
        GuestSearchSession.saveHomeRoute(requireContext(), fromCity, toCity);
        tvMcLeg1Route.setText(fromCity + "  -  " + toCity);

        if (tripMode == MODE_ROUND && !hasValidReturnDate()) {
            setReturnPlaceholder();
        } else if (tripMode != MODE_ROUND) {
            setReturnPlaceholder();
        }

        if (tripMode == MODE_MULTI && (etMc2From.getText() == null || etMc2From.getText().length() == 0)) {
            etMc2From.setText(toCity);
        }
    }

    private boolean isReturnPlaceholderText(String t) {
        return getString(R.string.select_date).equals(t)
                || getString(R.string.select_return_date).equals(t);
    }

    private void setReturnPlaceholder() {
        tvReturnDate.setText(R.string.select_return_date);
        tvReturnDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
    }

    private boolean hasValidReturnDate() {
        CharSequence t = tvReturnDate.getText();
        return t != null && t.length() > 0 && !isReturnPlaceholderText(t.toString());
    }

    private static String shortRouteLabel(String from, String to) {
        String a = from == null ? "" : from.trim();
        String b = to == null ? "" : to.trim();
        if (a.length() > 28) a = a.substring(0, 25) + "…";
        if (b.length() > 28) b = b.substring(0, 25) + "…";
        return a + "  -  " + b;
    }

    private void setDefaultDepartureDate() {
        int y = calendar.get(Calendar.YEAR);
        int m = calendar.get(Calendar.MONTH);
        int d = calendar.get(Calendar.DAY_OF_MONTH);
        String text = d + " " + getMonthName(m) + " " + y;
        tvDate.setText(text);
        tvDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_main));
    }

    private void selectTripMode(int mode, boolean preserveReturn) {
        tripMode = mode;
        int primary = ContextCompat.getColor(requireContext(), R.color.primary_et);
        int secondary = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int white = ContextCompat.getColor(requireContext(), R.color.white);

        tabOneWay.setBackground(null);
        tabRoundTrip.setBackground(null);
        tabMultiCity.setBackground(null);
        tabOneWay.setTextColor(secondary);
        tabRoundTrip.setTextColor(secondary);
        tabMultiCity.setTextColor(secondary);
        tabOneWay.setTypeface(null, Typeface.NORMAL);
        tabRoundTrip.setTypeface(null, Typeface.NORMAL);
        tabMultiCity.setTypeface(null, Typeface.NORMAL);

        TextView tvLeg1Title = getView() != null ? getView().findViewById(R.id.tvLeg1Title) : null;

        if (mode == MODE_ONE_WAY) {
            tabOneWay.setBackgroundResource(R.drawable.bg_segmented_item_selected);
            tabOneWay.setTextColor(white);
            tabOneWay.setTypeface(null, Typeface.BOLD);
            layoutReturnDateContainer.setVisibility(View.GONE);
            layoutMcLeg1Summary.setVisibility(View.GONE);
            layoutMcLeg2Block.setVisibility(View.GONE);
            if (tvLeg1Title != null) tvLeg1Title.setVisibility(View.GONE);
            tvDepartureDateLabel.setText(R.string.departure_date);
        } else if (mode == MODE_ROUND) {
            tabRoundTrip.setBackgroundResource(R.drawable.bg_segmented_item_selected);
            tabRoundTrip.setTextColor(white);
            tabRoundTrip.setTypeface(null, Typeface.BOLD);
            layoutReturnDateContainer.setVisibility(View.VISIBLE);
            layoutMcLeg1Summary.setVisibility(View.GONE);
            layoutMcLeg2Block.setVisibility(View.GONE);
            if (tvLeg1Title != null) tvLeg1Title.setVisibility(View.GONE);
            tvDepartureDateLabel.setText(R.string.departure_date);
            if (!preserveReturn || !hasValidReturnDate()) {
                setReturnPlaceholder();
            }
        } else {
            tabMultiCity.setBackgroundResource(R.drawable.bg_segmented_item_selected);
            tabMultiCity.setTextColor(white);
            tabMultiCity.setTypeface(null, Typeface.BOLD);
            layoutReturnDateContainer.setVisibility(View.GONE);
            layoutMcLeg1Summary.setVisibility(View.VISIBLE);
            layoutMcLeg2Block.setVisibility(View.VISIBLE);
            if (tvLeg1Title != null) tvLeg1Title.setVisibility(View.VISIBLE);
            tvDepartureDateLabel.setText(R.string.flight_1_outbound);
            if (etMc2From.getText() == null || etMc2From.getText().toString().trim().isEmpty()) {
                etMc2From.setText(toCity);
            }
            if (tvMc2Date.getText() == null || tvMc2Date.getText().length() == 0
                    || getString(R.string.select_date).contentEquals(tvMc2Date.getText())) {
                tvMc2Date.setText(R.string.select_date);
                if (getContext() != null) {
                    tvMc2Date.setTextColor(ContextCompat.getColor(getContext(), R.color.text_muted));
                }
            }
        }
    }


    private void findFlights() {
        if (tripMode == MODE_ROUND) {
            if (!hasValidReturnDate()) {
                Toast.makeText(getContext(), R.string.please_choose_return, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        Context ctx = getContext();
        if (ctx == null) return;

        if (tripMode == MODE_MULTI) {
            String l2f = etMc2From.getText().toString().trim();
            String l2t = etMc2To.getText().toString().trim();
            CharSequence l2d = tvMc2Date.getText();
            if (l2f.isEmpty()) {
                etMc2From.setError(getString(R.string.from_label));
                return;
            }
            if (l2t.isEmpty()) {
                etMc2To.setError(getString(R.string.to_label));
                return;
            }
            if (l2d == null || l2d.length() == 0 || getString(R.string.select_date).contentEquals(l2d)) {
                Toast.makeText(ctx, R.string.leg2_date, Toast.LENGTH_SHORT).show();
                return;
            }
            GuestSearchSession.saveMultiCityLeg2(ctx, l2f, l2t, l2d.toString());
        }

        // Use latest from input fields in case they changed since loadFromSession
        String currentFrom = fromCity;
        String currentTo = toCity;
        // In this fragment, fromCity/toCity are typically loaded from session, 
        // but let's ensure they are fresh if the UI has them.

        String returnForSave = tripMode == MODE_ROUND && hasValidReturnDate()
                ? tvReturnDate.getText().toString() : "";

        GuestSearchSession.saveCurrentSearch(ctx,
                fromCity,
                toCity,
                tvDate.getText().toString(),
                returnForSave,
                passengerCount,
                tvTravelClass.getText().toString(),
                tripMode == MODE_ROUND,
                tripMode == MODE_MULTI);
        GuestSearchSession.pushRecentSearch(ctx, fromCity, toCity);

        FlightListingFragment fragment = new FlightListingFragment();
        Bundle args = new Bundle();
        args.putString("fromCity", fromCity);
        args.putString("toCity", toCity);
        args.putString("date", tvDate.getText().toString());
        args.putInt("passengers", passengerCount);
        args.putString("travelClass", tvTravelClass.getText().toString());
        args.putBoolean("multiCity", tripMode == MODE_MULTI);
        if (tripMode == MODE_MULTI) {
            args.putString("mc2From", etMc2From.getText().toString().trim());
            args.putString("mc2To", etMc2To.getText().toString().trim());
            args.putString("mc2Date", tvMc2Date.getText().toString());
        }
        fragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    private void showDatePicker(TextView target) {
        Context ctx = getContext();
        if (ctx == null) return;
        DatePickerDialog dlg = new DatePickerDialog(
                ctx,
                (v, year, month, dayOfMonth) -> {
                    String selected = dayOfMonth + " " + getMonthName(month) + " " + year;
                    target.setText(selected);
                    if (getContext() != null) {
                        target.setTextColor(ContextCompat.getColor(getContext(), R.color.text_main));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dlg.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dlg.show();
    }

    private String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return months[month];
    }
}
