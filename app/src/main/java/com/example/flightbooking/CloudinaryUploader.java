package com.example.flightbooking;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

/**
 * Uploads images to Cloudinary using an unsigned upload preset.
 * Configure {@code cloudinary_cloud_name} and {@code cloudinary_upload_preset} in {@code strings.xml}.
 */
public final class CloudinaryUploader {

    private static final String TAG = "CloudinaryUploader";
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        void onSuccess(@NonNull String secureUrl);

        void onError(@NonNull String message);
    }

    private CloudinaryUploader() {}

    public static void uploadImage(
            @NonNull File imageFile,
            @NonNull String cloudName,
            @NonNull String uploadPreset,
            @NonNull UploadCallback callback) {
        if (cloudName.isEmpty() || uploadPreset.isEmpty()) {
            MAIN.post(() -> callback.onError("Set cloudinary_cloud_name and cloudinary_upload_preset in strings.xml"));
            return;
        }

        String mime = URLConnection.guessContentTypeFromName(imageFile.getName());
        if (mime == null) {
            mime = "image/jpeg";
        }
        MediaType mediaType = MediaType.parse(mime);
        if (mediaType == null) {
            mediaType = MediaType.parse("image/jpeg");
        }

        RequestBody fileBody = RequestBody.create(imageFile, mediaType);
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("file", imageFile.getName(), fileBody)
                .build();

        String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";
        Request request = new Request.Builder().url(url).post(body).build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Upload request failed", e);
                MAIN.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Network error"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String payload = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    String err = parseCloudinaryError(payload);
                    MAIN.post(() -> callback.onError(err != null ? err : ("HTTP " + response.code())));
                    return;
                }
                try {
                    JSONObject json = new JSONObject(payload);
                    if (json.has("error")) {
                        JSONObject err = json.optJSONObject("error");
                        String msg = err != null ? err.optString("message", "Unknown error") : "Unknown error";
                        MAIN.post(() -> callback.onError(msg));
                        return;
                    }
                    String secureUrl = json.optString("secure_url", "");
                    if (secureUrl.isEmpty()) {
                        MAIN.post(() -> callback.onError("No URL in Cloudinary response"));
                        return;
                    }
                    MAIN.post(() -> callback.onSuccess(secureUrl));
                } catch (Exception e) {
                    Log.e(TAG, "Parse response failed", e);
                    MAIN.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Invalid response"));
                }
            }
        });
    }

    private static String parseCloudinaryError(String payload) {
        try {
            JSONObject json = new JSONObject(payload);
            JSONObject err = json.optJSONObject("error");
            if (err != null) {
                return err.optString("message", payload);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
