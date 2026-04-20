package com.example.flightbooking;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.flightbooking.models.Destination;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AddDestinationFragment extends Fragment {

    private ImageView ivPreview;
    private EditText etCity, etCountry, etPrice, etRating;
    private Button btnUpload;
    private ProgressBar progressBar;

    private Uri imageUri;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    ivPreview.setImageURI(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_destination, container, false);

        db = FirebaseFirestore.getInstance();

        ivPreview = view.findViewById(R.id.ivDestinationPreview);
        etCity = view.findViewById(R.id.etDestCity);
        etCountry = view.findViewById(R.id.etDestCountry);
        etPrice = view.findViewById(R.id.etDestPrice);
        etRating = view.findViewById(R.id.etDestRating);
        btnUpload = view.findViewById(R.id.btnSaveDestination);
        progressBar = view.findViewById(R.id.uploadProgress);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());
        view.findViewById(R.id.btnSelectImage).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnUpload.setOnClickListener(v -> validateAndUpload());

        return view;
    }

    private void validateAndUpload() {
        String city = etCity.getText().toString().trim();
        String country = etCountry.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String ratingStr = etRating.getText().toString().trim();

        if (city.isEmpty() || country.isEmpty() || price.isEmpty() || ratingStr.isEmpty()) {
            Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        float rating;
        try {
            rating = Float.parseFloat(ratingStr);
            if (rating < 0 || rating > 5) {
                Toast.makeText(getContext(), "Rating must be between 0 and 5", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid rating format. Use a number (e.g., 4.5)", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpload.setEnabled(false);

        if (imageUri != null) {
            // Compress image and store as Base64 — no Firebase Storage needed!
            new Thread(() -> {
                try {
                    InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
                    Bitmap original = BitmapFactory.decodeStream(inputStream);

                    // Scale down to max 600px wide to keep Firestore document small
                    int maxWidth = 600;
                    int width = original.getWidth();
                    int height = original.getHeight();
                    if (width > maxWidth) {
                        float scale = (float) maxWidth / width;
                        width = maxWidth;
                        height = Math.round(height * scale);
                    }
                    Bitmap scaled = Bitmap.createScaledBitmap(original, width, height, true);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                    float finalRating = rating;
                    requireActivity().runOnUiThread(() ->
                            saveToFirestore(city, country, price, base64Image, null, finalRating));

                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnUpload.setEnabled(true);
                        Toast.makeText(getContext(), "Image processing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        } else {
            // No image selected — save with no image
            saveToFirestore(city, country, price, null, null, rating);
        }
    }

    private void saveToFirestore(String city, String country, String price,
                                  String imageBase64, String imageUrl, float rating) {
        Map<String, Object> data = new HashMap<>();
        data.put("city", city);
        data.put("country", country);
        data.put("price", price);
        data.put("rating", rating);
        if (imageBase64 != null) data.put("imageBase64", imageBase64);
        if (imageUrl != null) data.put("imageUrl", imageUrl);

        db.collection("destinations")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Destination saved successfully!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                    Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
