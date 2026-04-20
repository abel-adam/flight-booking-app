package com.example.flightbooking;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

public class ImageLoader {

    /** Load from a URL (http/https). */
    public static void load(String url, ImageView imageView) {
        if (imageView == null) return;
        if (url == null || url.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_location_pin);
            return;
        }

        Context context = imageView.getContext();

        CircularProgressDrawable progressDrawable = new CircularProgressDrawable(context);
        progressDrawable.setStrokeWidth(5f);
        progressDrawable.setCenterRadius(30f);
        progressDrawable.setColorSchemeColors(androidx.core.content.ContextCompat.getColor(context, R.color.primary));
        progressDrawable.start();

        Glide.with(context)
            .load(url)
            .placeholder(progressDrawable)
            .error(R.drawable.ic_location_pin)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(imageView);
    }

    /** Load from a Base64-encoded string (stored directly in Firestore). */
    public static void loadBase64(String base64, ImageView imageView) {
        if (imageView == null) return;
        if (base64 == null || base64.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_location_pin);
            return;
        }
        try {
            byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else {
                imageView.setImageResource(R.drawable.ic_location_pin);
            }
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.ic_location_pin);
        }
    }

    /**
     * Smart loader: uses Base64 if available, falls back to URL, then placeholder.
     * Use this everywhere destinations are displayed.
     */
    public static void loadDestinationImage(String base64, String url, ImageView imageView) {
        if (imageView == null) return;
        if (base64 != null && !base64.isEmpty()) {
            loadBase64(base64, imageView);
        } else if (url != null && !url.isEmpty()) {
            load(url, imageView);
        } else {
            imageView.setImageResource(R.drawable.ic_location_pin);
        }
    }
}
