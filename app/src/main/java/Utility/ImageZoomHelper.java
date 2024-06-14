package Utility;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

public class ImageZoomHelper {

    private static Animator currentAnimator;
    private static int shortAnimationDuration;

    public static void zoomImageFromThumb(final View thumbView, Drawable imageDrawable, ImageView expandedImageView, Context context) {
        // If there's an animation in progress, cancel it immediately and proceed with this one.
        if (currentAnimator != null) {
            currentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        expandedImageView.setImageDrawable(imageDrawable);

        // Calculate the starting and ending bounds for the zoomed-in image.
        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        // The start bounds are the global visible rectangle of the thumbnail,
        // and the final bounds are the global visible rectangle of the container view.
        // Set the container view's offset as the origin for the bounds, since that's the origin
        // for the positioning animation properties (X, Y).
        thumbView.getGlobalVisibleRect(startBounds);
        expandedImageView.getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        // Using the "center crop" technique, adjust the start bounds to be the same aspect ratio
        // as the final bounds. This prevents unwanted stretching during the animation.
        // Calculate the start scaling factor. The end scaling factor is always 1.0.
        float startScale;
        if ((float) finalBounds.width() / finalBounds.height() > (float) startBounds.width() / startBounds.height()) {
            // Extend start bounds horizontally.
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            // Extend start bounds vertically.
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }

        // Hide the thumbnail and show the zoomed-in view. When the animation begins, it positions
        // the zoomed-in view in the place of the thumbnail.
        thumbView.setAlpha(0f);
        expandedImageView.setVisibility(View.VISIBLE);

        animateZoomToLargeImage(startBounds, finalBounds, startScale, expandedImageView);

        shortAnimationDuration = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    private static void animateZoomToLargeImage(Rect startBounds, Rect finalBounds, float startScale, ImageView expandedImageView) {
        // Set the pivot point for SCALE_X and SCALE_Y transformations to the top-left corner of the zoomed-in view.
        // The default is the center of the view.
        expandedImageView.setPivotX(0f);
        expandedImageView.setPivotY(0f);

        // Construct and run the parallel animation of the four translation and scale properties: X, Y, SCALE_X, and SCALE_Y.
        AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f));
        set.setDuration(shortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                currentAnimator = null;
            }
        });
        set.start();
        currentAnimator = set;
    }

    public static void setDismissLargeImageAnimation(final View thumbView, final Rect startBounds, final float startScale, final ImageView expandedImageView) {
        // When the zoomed-in image is tapped, it zooms down to the original bounds and shows the thumbnail instead of the expanded image.
        expandedImageView.setOnClickListener(view -> {
            if (currentAnimator != null) {
                currentAnimator.cancel();
            }

            // Animate the four positioning and sizing properties in parallel, back to their original values.
            AnimatorSet set = new AnimatorSet();
            set.play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left))
                    .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                    .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale))
                    .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale));
            set.setDuration(shortAnimationDuration);
            set.setInterpolator(new DecelerateInterpolator());
            set.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    thumbView.setAlpha(1f);
                    expandedImageView.setVisibility(View.GONE);
                    currentAnimator = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    thumbView.setAlpha(1f);
                    expandedImageView.setVisibility(View.GONE);
                    currentAnimator = null;
                }
            });
            set.start();
            currentAnimator = set;
        });
    }
}
