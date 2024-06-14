package Utility;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.chateasy.R;

public class ImageUtils {

    public static void loadImage(Context context, String imageUrl, ImageLoadListener imageLoadListener) {
        try {
            Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .circleCrop()
                    .placeholder(R.drawable.user)
                    .sizeMultiplier(0.50f)
                    .addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                            Log.e("TAG", "loadImage: failed", e);
                            if (imageLoadListener != null) {
                                imageLoadListener.onLoadFailed();
                            }
                            return true;
                        }

                        @Override
                        public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                            Log.d("TAG", "loadImage: ready");
                            // Ensure UI update is performed on the main thread
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    if (imageLoadListener != null) {
                                        imageLoadListener.onResourceReady(resource);
                                    }
                                }
                            });
                            return true;
                        }
                    })
                    .submit();
        } catch (IllegalArgumentException e) {
            Log.e("TAG", "loadImage: " + e.getMessage());
            if (imageLoadListener != null) {
                imageLoadListener.onLoadFailed();
            }
        }
    }

    public interface ImageLoadListener {
        void onResourceReady(Drawable resource);

        void onLoadFailed();
    }
}
