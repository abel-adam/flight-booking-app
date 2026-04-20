package com.example.flightbooking;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.example.flightbooking.models.Booking;
import com.example.flightbooking.models.Flight;
import com.example.flightbooking.util.QrBitmapUtil;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.example.flightbooking.R;

public class BoardingPassFragment extends Fragment {

    private FirebaseFirestore db;
    private Booking booking;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_boarding_pass, container, false);
        db = FirebaseFirestore.getInstance();

        String docId = getArguments() != null ? getArguments().getString("bookingDocId") : null;
        if (docId == null) {
            Toast.makeText(getContext(), "Missing booking", Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return view;
        }

        ImageButton btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        MaterialCardView cardBoardingPass = view.findViewById(R.id.cardBoardingPass);
        ImageView ivQr = view.findViewById(R.id.ivQr);
        TextView tvPnr = view.findViewById(R.id.tvPnr);
        TextView tvPassenger = view.findViewById(R.id.tvPassenger);
        TextView tvSeat = view.findViewById(R.id.tvSeat);
        TextView tvGate = view.findViewById(R.id.tvGate);
        TextView tvBoardingTime = view.findViewById(R.id.tvBoardingTime);
        TextView tvTravelClass = view.findViewById(R.id.tvTravelClass);
        TextView tvFlightRoute = view.findViewById(R.id.tvFlightRoute);
        MaterialButton btnShare = view.findViewById(R.id.btnShare);
        MaterialButton btnPdf = view.findViewById(R.id.btnDownloadPdf);

        db.collection("bookings").document(docId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    booking = snapshot.toObject(Booking.class);
                    if (booking == null) {
                        Toast.makeText(getContext(), "Booking not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String pnr = booking.getPnrCode() != null ? booking.getPnrCode() : docId;
                    tvPnr.setText("PNR: " + pnr);
                    tvPassenger.setText(booking.getPassengerName() != null ? booking.getPassengerName() : "—");
                    tvSeat.setText(booking.getSeat() != null ? booking.getSeat() : "—");
                    tvGate.setText(booking.getGate() != null ? booking.getGate() : "—");
                    tvTravelClass.setText(booking.getTravelClass() != null ? booking.getTravelClass() : "Economy");

                    Flight f = booking.getFlight();
                    String from = f != null && f.getFromCode() != null ? f.getFromCode()
                            : booking.getFromCode() != null ? booking.getFromCode() : "—";
                    String to = f != null && f.getToCode() != null ? f.getToCode()
                            : booking.getToCode() != null ? booking.getToCode() : "—";
                    tvFlightRoute.setText(from + " - " + to);

                    String dep = f != null && f.getFromTime() != null ? f.getFromTime()
                            : booking.getFromTime() != null ? booking.getFromTime() : "—";
                    tvBoardingTime.setText(dep);

                    String qrPayload = "PNR:" + pnr + "|" + from + "-" + to + "|SEAT:" + booking.getSeat();
                    android.graphics.Bitmap bmp = QrBitmapUtil.encodeQr(qrPayload, 512);
                    if (bmp != null) {
                        ivQr.setImageBitmap(bmp);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to load: " + e.getMessage(), Toast.LENGTH_SHORT).show());

        btnShare.setOnClickListener(v -> {
            if (booking == null) return;
            shareBoardingPass(cardBoardingPass);
        });

        btnPdf.setOnClickListener(v -> {
            if (booking == null) {
                Toast.makeText(getContext(), "Still loading…", Toast.LENGTH_SHORT).show();
                return;
            }
            PdfGenerator.generateTicketPdf(requireContext(), booking);
        });

        return view;
    }

    private Bitmap getBitMapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void shareBoardingPass(View view) {
        Bitmap bitmap = getBitMapFromView(view);
        try {
            File cachePath = new File(requireContext().getCacheDir(), "images");
            cachePath.mkdirs(); 
            FileOutputStream stream = new FileOutputStream(cachePath + "/boarding_pass.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            File imagePath = new File(requireContext().getCacheDir(), "images");
            File newFile = new File(imagePath, "boarding_pass.png");
            Uri contentUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", newFile);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); 
                shareIntent.setDataAndType(contentUri, requireContext().getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                String pnr = booking.getPnrCode() != null ? booking.getPnrCode() : "";
                shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is my boarding pass (PNR: " + pnr + ")");
                startActivity(Intent.createChooser(shareIntent, "Share Boarding Pass"));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to share image", Toast.LENGTH_SHORT).show();
        }
    }

}
