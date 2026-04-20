package com.example.flightbooking;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.example.flightbooking.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProfileEditFragment extends Fragment {

    private static final String TAG = "ProfileEditFragment";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private EditText etDisplayName;
    private TextView tvEmail;
    private ImageButton btnProfileCamera;
    private de.hdodenhof.circleimageview.CircleImageView ivProfileEdit;
    private View cardTakePhoto;
    private View cardUpload;
    private Button btnSaveChanges;

    private File pendingUploadFile;
    private String existingPhotoUrl = "";
    private File cameraFile;

    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && cameraFile != null && cameraFile.exists()) {
                    pendingUploadFile = cameraFile;
                    Glide.with(this).load(cameraFile).into(ivProfileEdit);
                }
            });

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) return;
                Uri uri = result.getData().getData();
                if (uri == null || getContext() == null) return;
                File out = new File(requireContext().getCacheDir(), "profile_pick_" + System.currentTimeMillis() + ".jpg");
                try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
                    if (in == null) return;
                    try (FileOutputStream os = new FileOutputStream(out)) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            os.write(buf, 0, n);
                        }
                    }
                    pendingUploadFile = out;
                    Glide.with(this).load(out).into(ivProfileEdit);
                } catch (IOException e) {
                    Log.e(TAG, "Copy picked image failed", e);
                    Toast.makeText(getContext(), R.string.image_read_failed, Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_edit, container, false);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etDisplayName = view.findViewById(R.id.etDisplayName);
        tvEmail = view.findViewById(R.id.tvEmail);
        ivProfileEdit = view.findViewById(R.id.ivProfileEdit);
        btnProfileCamera = view.findViewById(R.id.btnProfileCamera);
        cardTakePhoto = view.findViewById(R.id.cardTakePhoto);
        cardUpload = view.findViewById(R.id.cardUpload);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);

        btnProfileCamera.setOnClickListener(v -> launchCamera());
        cardTakePhoto.setOnClickListener(v -> launchCamera());
        cardUpload.setOnClickListener(v -> launchGalleryPick());

        btnSaveChanges.setOnClickListener(v -> saveProfile());

        loadUserProfile();
        return view;
    }

    private void loadUserProfile() {
        FirebaseUser fu = auth.getCurrentUser();
        if (fu == null) return;
        tvEmail.setText(fu.getEmail() != null ? fu.getEmail() : "");

        String uid = fu.getUid();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    User u = doc.toObject(User.class);
                    if (u != null) {
                        if (u.getName() != null && !u.getName().isEmpty()) {
                            etDisplayName.setText(u.getName());
                        } else if (fu.getDisplayName() != null) {
                            etDisplayName.setText(fu.getDisplayName());
                        }
                        if (u.getPhotoUrl() != null && !u.getPhotoUrl().isEmpty()) {
                            existingPhotoUrl = u.getPhotoUrl();
                            Glide.with(this).load(existingPhotoUrl).into(ivProfileEdit);
                        }
                    } else if (fu.getDisplayName() != null) {
                        etDisplayName.setText(fu.getDisplayName());
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "load user", e));
    }

    private void launchCamera() {
        if (getContext() == null) return;
        cameraFile = new File(requireContext().getCacheDir(), "profile_cam_" + System.currentTimeMillis() + ".jpg");
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                cameraFile);
        takePictureLauncher.launch(uri);
    }

    private void launchGalleryPick() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void saveProfile() {
        if (getContext() == null) return;
        FirebaseUser fu = auth.getCurrentUser();
        if (fu == null) {
            Toast.makeText(getContext(), "Not signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = etDisplayName.getText().toString().trim();
        if (name.isEmpty()) {
            etDisplayName.setError("Required");
            return;
        }

        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText(getString(R.string.flight_save_processing));

        if (pendingUploadFile != null && pendingUploadFile.exists()) {
            String cloud = getString(R.string.cloudinary_cloud_name);
            String preset = getString(R.string.cloudinary_upload_preset);
            CloudinaryUploader.uploadImage(pendingUploadFile, cloud, preset, new CloudinaryUploader.UploadCallback() {
                @Override
                public void onSuccess(@NonNull String secureUrl) {
                    persistUser(fu, name, secureUrl);
                }

                @Override
                public void onError(@NonNull String message) {
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                    Toast.makeText(getContext(), getString(R.string.image_upload_failed_detail, message), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            persistUser(fu, name, existingPhotoUrl != null ? existingPhotoUrl : "");
        }
    }

    private void persistUser(FirebaseUser fu, String name, String photoUrl) {
        String uid = fu.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("photoUrl", photoUrl);
        db.collection("users").document(uid).set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> afterSaveSuccess(fu, name, photoUrl))
                .addOnFailureListener(this::onSaveFailed);
    }

    private void afterSaveSuccess(FirebaseUser fu, String name, String photoUrl) {
        UserProfileChangeRequest.Builder b = new UserProfileChangeRequest.Builder()
                .setDisplayName(name);
        if (photoUrl != null && !photoUrl.isEmpty()) {
            b.setPhotoUri(Uri.parse(photoUrl));
        }
        fu.updateProfile(b.build()).addOnCompleteListener(task -> {
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setText("Save Changes");
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Profile updated", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            }
        });
    }

    private void onSaveFailed(Exception e) {
        btnSaveChanges.setEnabled(true);
        btnSaveChanges.setText("Save Changes");
        if (getContext() != null) {
            Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
