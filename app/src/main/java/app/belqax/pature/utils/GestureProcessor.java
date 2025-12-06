package app.belqax.pature.utils;

import android.annotation.SuppressLint;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import app.belqax.pature.R;
import app.belqax.pature.ui.CardAnimator;
import app.belqax.pature.ui.PatureStackLayout;

public final class GestureProcessor {

    private static float downX;
    private static float downY;

    private static final float ROTATION_DEGREES = 15f;
    private static final float SWIPE_THRESHOLD_FRACTION = 0.30f;

    private static final long RETURN_DURATION_MS = 300L;
    private static final long OVERLAY_FADE_DURATION_MS = 180L;

    private static final DecelerateInterpolator DECELERATE_INTERPOLATOR =
            new DecelerateInterpolator();
    private static final OvershootInterpolator OVERSHOOT_INTERPOLATOR =
            new OvershootInterpolator(1.3f);

    private GestureProcessor() {
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void attachToStack(@NonNull PatureStackLayout stack) {
        stack.setOnTouchListener((v, event) -> {
            View card = stack.getTopCard();
            if (card == null) {
                return false;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX();
                    downY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;

                    card.setTranslationX(dx);
                    card.setTranslationY(dy);

                    float rotation = ROTATION_DEGREES * (dx / v.getWidth());
                    card.setRotation(rotation);

                    float progress = Math.min(1f,
                            Math.abs(dx) / (v.getWidth() * SWIPE_THRESHOLD_FRACTION));

                    View likeBg = card.findViewById(R.id.swipeLikeBg);
                    View dislikeBg = card.findViewById(R.id.swipeDislikeBg);
                    ImageView likeIcon = card.findViewById(R.id.swipeLikeOverlay);
                    ImageView dislikeIcon = card.findViewById(R.id.swipeDislikeOverlay);

                    if (dx > 0) {
                        setOverlayAlphas(progress,
                                likeBg, likeIcon,
                                dislikeBg, dislikeIcon);
                    } else if (dx < 0) {
                        setOverlayAlphas(progress,
                                dislikeBg, dislikeIcon,
                                likeBg, likeIcon);
                    } else {
                        // в середине убирает оверлеи, но без анимации, чтобы не "мазать" движение
                        resetOverlaysImmediate(likeBg, dislikeBg, likeIcon, dislikeIcon);
                    }

                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    float dx = event.getRawX() - downX;
                    float absDx = Math.abs(dx);
                    float threshold = v.getWidth() * SWIPE_THRESHOLD_FRACTION;

                    View likeBg = card.findViewById(R.id.swipeLikeBg);
                    View dislikeBg = card.findViewById(R.id.swipeDislikeBg);
                    ImageView likeIcon = card.findViewById(R.id.swipeLikeOverlay);
                    ImageView dislikeIcon = card.findViewById(R.id.swipeDislikeOverlay);

                    if (absDx > threshold) {
                        // свайп засчитан, карта вылетает
                        float endX = dx > 0 ? v.getWidth() * 2f : -v.getWidth() * 2f;
                        CardAnimator.flyOut(card, endX, () -> {
                            resetOverlaysImmediate(likeBg, dislikeBg, likeIcon, dislikeIcon);
                            stack.popCard();
                        });
                    } else {
                        // свайп не дотянул, мягко возвращает карту и плавно гасит оверлеи
                        card.animate()
                                .translationX(0f)
                                .translationY(0f)
                                .rotation(0f)
                                .setInterpolator(OVERSHOOT_INTERPOLATOR)
                                .setDuration(RETURN_DURATION_MS)
                                .start();

                        fadeOutOverlays(likeBg, dislikeBg, likeIcon, dislikeIcon);
                    }
                    return true;
                }

                default:
                    return false;
            }
        });
    }

    private static void setOverlayAlphas(float progress,
                                         View mainBg, ImageView mainIcon,
                                         View otherBg, ImageView otherIcon) {

        float clamped = Math.max(0f, Math.min(1f, progress));

        if (mainBg != null) {
            mainBg.setAlpha(clamped);
        }
        if (mainIcon != null) {
            mainIcon.setAlpha(clamped);
        }
        if (otherBg != null) {
            otherBg.setAlpha(0f);
        }
        if (otherIcon != null) {
            otherIcon.setAlpha(0f);
        }
    }

    private static void resetOverlaysImmediate(View likeBg,
                                               View dislikeBg,
                                               ImageView likeIcon,
                                               ImageView dislikeIcon) {
        if (likeBg != null) {
            likeBg.setAlpha(0f);
        }
        if (dislikeBg != null) {
            dislikeBg.setAlpha(0f);
        }
        if (likeIcon != null) {
            likeIcon.setAlpha(0f);
        }
        if (dislikeIcon != null) {
            dislikeIcon.setAlpha(0f);
        }
    }

    private static void fadeOutOverlays(View likeBg,
                                        View dislikeBg,
                                        ImageView likeIcon,
                                        ImageView dislikeIcon) {

        if (likeBg != null) {
            likeBg.animate()
                    .alpha(0f)
                    .setInterpolator(DECELERATE_INTERPOLATOR)
                    .setDuration(OVERLAY_FADE_DURATION_MS)
                    .start();
        }

        if (dislikeBg != null) {
            dislikeBg.animate()
                    .alpha(0f)
                    .setInterpolator(DECELERATE_INTERPOLATOR)
                    .setDuration(OVERLAY_FADE_DURATION_MS)
                    .start();
        }

        if (likeIcon != null) {
            likeIcon.animate()
                    .alpha(0f)
                    .setInterpolator(DECELERATE_INTERPOLATOR)
                    .setDuration(OVERLAY_FADE_DURATION_MS)
                    .start();
        }

        if (dislikeIcon != null) {
            dislikeIcon.animate()
                    .alpha(0f)
                    .setInterpolator(DECELERATE_INTERPOLATOR)
                    .setDuration(OVERLAY_FADE_DURATION_MS)
                    .start();
        }
    }
}
