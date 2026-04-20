package com.example.flightbooking;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.flightbooking.adapters.AdminFlightAdapter;
import com.example.flightbooking.models.Flight;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.flightbooking.util.AirportDisplayHelper;
import com.example.flightbooking.util.AirportSuggestions;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import android.widget.Filterable;
import android.widget.ListAdapter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FlightManagementFragment extends Fragment {

    private static final String TAG = "FlightManagement";

    private FirebaseFirestore db;
    private final List<Flight> flights = new ArrayList<>();
    private final List<String> docIds = new ArrayList<>();
    private AdminFlightAdapter adapter;
    private TextView tvFlightCount;
    private Uri selectedImageUri;
    private ImageView currentDialogImageView;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (currentDialogImageView != null) {
                        currentDialogImageView.setImageURI(selectedImageUri);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_flight_management, container, false);
        db = FirebaseFirestore.getInstance();

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        tvFlightCount = view.findViewById(R.id.tvFlightCount);

        RecyclerView rv = view.findViewById(R.id.rvAdminUsers); // Note: Should be rvAdminFlights but using current ID from layout
        if (rv == null) rv = view.findViewById(R.id.rvAdminFlights);
        
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminFlightAdapter(flights, docIds);
        adapter.setOnFlightActionListener(new AdminFlightAdapter.OnFlightActionListener() {
            @Override
            public void onEdit(Flight flight, int position) {
                showAddFlightDialog(flight, docIds.get(position), position);
            }

            @Override
            public void onDelete(Flight flight, int position, String docId) {
                if (docId == null) return;
                db.collection("flights").document(docId).delete()
                        .addOnSuccessListener(aVoid -> {
                            flights.remove(position);
                            docIds.remove(position);
                            adapter.notifyItemRemoved(position);
                            updateFlightCountLabel();
                            Toast.makeText(getContext(), "Flight deleted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Delete failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show());
            }
        });
        rv.setAdapter(adapter);

        view.findViewById(R.id.btnAddFlight).setOnClickListener(v ->
                showAddFlightDialog(null, null, -1));

        fetchFlights();
        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchFlights() {
        db.collection("flights").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                flights.clear();
                docIds.clear();
                for (DocumentSnapshot doc : value.getDocuments()) {
                    Flight f = doc.toObject(Flight.class);
                    if (f != null) {
                        f.setId(doc.getId());
                        flights.add(f);
                        docIds.add(doc.getId());
                    }
                }
                adapter.notifyDataSetChanged();
                updateFlightCountLabel();
            }
        });
    }

    private void updateFlightCountLabel() {
        if (tvFlightCount != null) {
            int count = flights.size();
            tvFlightCount.setText(count + " Flight" + (count == 1 ? "" : "s") + " Available");
        }
    }

    private void showAddFlightDialog(@Nullable Flight existing, @Nullable String docId, int position) {
        if (getContext() == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(getContext(), R.style.BottomSheetDialogTheme);
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_flight, null);
        dialog.setContentView(dialogView);

        EditText etAirline           = dialogView.findViewById(R.id.etAirlineName);
        AutoCompleteTextView etFrom  = dialogView.findViewById(R.id.etFromCode);
        AutoCompleteTextView etTo    = dialogView.findViewById(R.id.etToCode);
        TextView etDepartureDate     = dialogView.findViewById(R.id.etDepartureDate);
        TextView etDepartureTime     = dialogView.findViewById(R.id.etDepartureTime);
        TextView etArrivalDate       = dialogView.findViewById(R.id.etArrivalDate);
        TextView etArrivalTime       = dialogView.findViewById(R.id.etArrivalTime);
        EditText etTotalCapacity     = dialogView.findViewById(R.id.etTotalCapacity);
        EditText etPrice             = dialogView.findViewById(R.id.etPrice);
        EditText etBusinessPrice     = dialogView.findViewById(R.id.etBusinessPrice);
        Button  btnSave              = dialogView.findViewById(R.id.btnSaveFlight);
        View    btnClose             = dialogView.findViewById(R.id.btnCloseDialog);
        ImageView ivLogo             = dialogView.findViewById(R.id.ivFlightLogo);

        currentDialogImageView = ivLogo;
        selectedImageUri = null;

        // Pre-populate airports
        ArrayAdapter<String> airportAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, AirportSuggestions.all());
        etFrom.setAdapter(airportAdapter);
        etTo.setAdapter(airportAdapter);

        // UI Fix: Show dropdown on click/focus
        View.OnClickListener dropdownOpener = v -> {
            if (v instanceof AutoCompleteTextView) {
                AutoCompleteTextView atv = (AutoCompleteTextView) v;
                ListAdapter adapter = atv.getAdapter();
                if (adapter instanceof Filterable) {
                    ((Filterable) adapter).getFilter().filter(null);
                }
                atv.showDropDown();
            }
        };
        etFrom.setOnClickListener(dropdownOpener);
        etTo.setOnClickListener(dropdownOpener);
        etFrom.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) dropdownOpener.onClick(v); });
        etTo.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) dropdownOpener.onClick(v); });

        // Date Picker Setup (preventing past dates)
        View.OnClickListener dateClickListener = v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialogDate = new DatePickerDialog(getContext(), (view1, year, month, dayOfMonth) -> {
                String date = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                ((TextView) v).setText(date);
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            dialogDate.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            dialogDate.show();
        };
        etDepartureDate.setOnClickListener(dateClickListener);
        etArrivalDate.setOnClickListener(dateClickListener);

        // Time Picker Setup
        View.OnClickListener timeClickListener = v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog dialogTime = new TimePickerDialog(getContext(), (view12, hourOfDay, minute) -> {
                String amPm = hourOfDay >= 12 ? "PM" : "AM";
                int adjustedHour = hourOfDay % 12;
                if (adjustedHour == 0) adjustedHour = 12;
                String time = String.format(java.util.Locale.US, "%02d:%02d %s", adjustedHour, minute, amPm);
                ((TextView) v).setText(time);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
            dialogTime.show();
        };
        etDepartureTime.setOnClickListener(timeClickListener);
        etArrivalTime.setOnClickListener(timeClickListener);

        // Pre-fill if editing
        boolean isEdit = existing != null;
        if (isEdit) {
            etAirline.setText(existing.getAirlineName());
            etFrom.setText(existing.getFromCode());
            etTo.setText(existing.getToCode());
            etDepartureDate.setText(existing.getDepartureDate() != null ? existing.getDepartureDate() : "");
            etDepartureTime.setText(existing.getFromTime());
            etArrivalDate.setText(existing.getArrivalDate() != null ? existing.getArrivalDate() : "");
            etArrivalTime.setText(existing.getToTime());
            etTotalCapacity.setText(String.valueOf(existing.getTotalCapacity() > 0 ? existing.getTotalCapacity() : 150));
            
            String p = existing.getPrice() != null ? existing.getPrice() : "";
            etPrice.setText(p.replace("$", ""));
            
            String bp = existing.getBusinessPrice() != null ? existing.getBusinessPrice() : "";
            etBusinessPrice.setText(bp.replace("$", ""));
            
            btnSave.setText(getString(R.string.save_flight_changes));
            
            if (existing.getAirlineLogo() != null && !existing.getAirlineLogo().isEmpty()) {
                Glide.with(this).load(existing.getAirlineLogo()).into(ivLogo);
            }
        }

        ivLogo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String airline   = etAirline.getText().toString().trim();
            String from      = etFrom.getText().toString().trim().toUpperCase();
            String to        = etTo.getText().toString().trim().toUpperCase();
            String depDate   = etDepartureDate.getText().toString().trim();
            String depTime   = etDepartureTime.getText().toString().trim();
            String arrDate   = etArrivalDate.getText().toString().trim();
            String arrTime   = etArrivalTime.getText().toString().trim();
            String capacityStr = etTotalCapacity.getText().toString().trim();
            String price     = etPrice.getText().toString().trim();
            String businessPriceInput = etBusinessPrice.getText().toString().trim();

            if (airline.isEmpty() || from.isEmpty() || to.isEmpty() || depDate.isEmpty() 
                || depTime.isEmpty() || arrDate.isEmpty() || arrTime.isEmpty() || price.isEmpty() || capacityStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int capacity = 150;
            try { capacity = Integer.parseInt(capacityStr); } catch (NumberFormatException ignored) {}

            btnSave.setEnabled(false);
            btnSave.setText(getString(R.string.flight_save_processing));
            
            String fromIata = AirportDisplayHelper.extractIataCode(from);
            String toIata = AirportDisplayHelper.extractIataCode(to);
            
            // Auto-correct common mistakes: if user typed Dublin code but meant Dubai city
            if (to.toUpperCase().contains("DUBAI") && toIata.equals("DUB")) {
                toIata = "DXB";
            } else if (to.toUpperCase().contains("DUBLIN") && toIata.equals("DXB")) {
                toIata = "DUB";
            }

            Flight flightData = new Flight();
            flightData.setAirlineName(airline);
            flightData.setFromCode(fromIata);
            flightData.setToCode(toIata);
            flightData.setDepartureDate(depDate);
            flightData.setFromTime(depTime);
            flightData.setArrivalDate(arrDate);
            flightData.setToTime(arrTime);
            flightData.setTotalCapacity(capacity);
            flightData.setBookedSeats(isEdit ? existing.getBookedSeats() : 0);
            flightData.setPrice("$" + price);
            flightData.setStatus(isEdit ? existing.getStatus() : "Active");
            flightData.setFlightNumber(isEdit ? existing.getFlightNumber() : "FL-" + (int)(System.currentTimeMillis() % 9000 + 1000));
            flightData.setDuration("4h 30m"); // Ethiopian Airlines average to Dubai
            flightData.setDirect("Direct");
            flightData.setStops("Direct");
            flightData.setBaggageAllowance("1 x 23kg Checked Bag");
            flightData.setTerminalFrom("Terminal 2");
            flightData.setTerminalTo("Terminal 1");
            flightData.setIsRefundable(true);
            int economy = 0;
            try { economy = Integer.parseInt(price.replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            
            if (!businessPriceInput.isEmpty()) {
                flightData.setBusinessPrice("$" + businessPriceInput);
            } else {
                flightData.setBusinessPrice("$" + (economy * 2));
            }

            if (selectedImageUri != null) {
                uploadImageAndSave(flightData, isEdit, docId, position, dialog, btnSave, existing);
            } else {
                flightData.setAirlineLogo(isEdit ? existing.getAirlineLogo() : "");
                saveFlight(flightData, isEdit, docId, dialog, btnSave);
            }
        });

        dialog.show();
    }

    private void uploadImageAndSave(Flight flightData, boolean isEdit, String docId, int position, BottomSheetDialog dialog,
                                  Button btnSave, Flight existing) {
        Context ctx = getContext();
        if (ctx == null) return;

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            resetFlightSaveButton(btnSave, isEdit);
            Toast.makeText(ctx, R.string.must_sign_in_to_upload, Toast.LENGTH_LONG).show();
            return;
        }

        File cacheFile = new File(ctx.getCacheDir(), "flight_logo_upload_" + System.currentTimeMillis() + ".jpg");
        try {
            try (InputStream in = ctx.getContentResolver().openInputStream(selectedImageUri)) {
                if (in == null) {
                    resetFlightSaveButton(btnSave, isEdit);
                    Toast.makeText(ctx, R.string.image_read_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                try (FileOutputStream out = new FileOutputStream(cacheFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Copy image to cache failed", e);
            resetFlightSaveButton(btnSave, isEdit);
            Toast.makeText(ctx, getString(R.string.image_upload_failed_detail, e.getMessage()), Toast.LENGTH_LONG).show();
            return;
        }

        String cloudName = getString(R.string.cloudinary_cloud_name);
        String uploadPreset = getString(R.string.cloudinary_upload_preset);
        CloudinaryUploader.uploadImage(cacheFile, cloudName, uploadPreset, new CloudinaryUploader.UploadCallback() {
            @Override
            public void onSuccess(@NonNull String secureUrl) {
                if (!cacheFile.delete()) {
                    Log.w(TAG, "Could not delete temp upload file");
                }
                flightData.setAirlineLogo(secureUrl);
                saveFlight(flightData, isEdit, docId, dialog, btnSave);
            }

            @Override
            public void onError(@NonNull String message) {
                if (!cacheFile.delete()) {
                    Log.w(TAG, "Could not delete temp upload file after failure");
                }
                Log.e(TAG, "Cloudinary upload failed: " + message);
                resetFlightSaveButton(btnSave, isEdit);
                Toast.makeText(ctx, getString(R.string.image_upload_failed_detail, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void resetFlightSaveButton(Button btnSave, boolean isEdit) {
        btnSave.setEnabled(true);
        btnSave.setText(isEdit ? getString(R.string.save_flight_changes) : getString(R.string.add_flight_confirm));
    }

    private void saveFlight(Flight flightData, boolean isEdit, String docId, BottomSheetDialog dialog, Button btnSave) {
        Context ctx = getContext();
        if (isEdit && docId != null) {
            db.collection("flights").document(docId).set(flightData)
                    .addOnSuccessListener(aVoid -> {
                        dialog.dismiss();
                        if (ctx != null) {
                            Toast.makeText(ctx, "Flight updated!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        resetFlightSaveButton(btnSave, true);
                        if (ctx != null) {
                            Toast.makeText(ctx, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            db.collection("flights").add(flightData)
                    .addOnSuccessListener(ref -> {
                        dialog.dismiss();
                        if (ctx != null) {
                            Toast.makeText(ctx, "Flight added!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        resetFlightSaveButton(btnSave, false);
                        if (ctx != null) {
                            Toast.makeText(ctx, "Add failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }
}
