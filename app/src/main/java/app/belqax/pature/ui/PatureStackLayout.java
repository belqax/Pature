package app.belqax.pature.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import app.belqax.pature.R;
import app.belqax.pature.adapter.CardAdapter;

public final class PatureStackLayout extends FrameLayout {

    public interface OnCardSwipedListener {
        void onCardSwiped(@NonNull View cardView, int adapterPosition, @NonNull Direction direction);
        void onCardDrag(@NonNull View cardView, float progress, @NonNull Direction direction);
    }

    public enum Direction {
        LEFT,
        RIGHT
    }

    private static final String TAG = "PatureStackLayout";

    private static final int MAX_VISIBLE = 3;
    private static final float SCALE_STEP = 0.04f;
    private static final float TRANSLATION_Y_STEP_DP = 10f;

    private static final long ANIM_DURATION_MS = 220L;

    // Плавность подтягивания задней карточки во время drag:
    // 0 = не двигается, 1 = полностью переходит в позицию топ-карты при progress=1
    private static final float BACK_CARD_PULL_FACTOR = 1.0f;

    private CardAdapter adapter;
    private int topPosition = 0;

    private float downX;
    private float downY;
    private boolean isDragging;
    private int touchSlop;

    private float swipeThresholdPx;
    private float maxRotationDeg = 12f;

    @Nullable
    private OnCardSwipedListener swipedListener;

    public PatureStackLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PatureStackLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PatureStackLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(@NonNull Context context) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        float density = context.getResources().getDisplayMetrics().density;
        swipeThresholdPx = 120f * density;
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setAdapter(@NonNull CardAdapter adapter) {
        this.adapter = adapter;
        this.topPosition = 0;
        buildInitialStack();
    }

    public void setOnCardSwipedListener(@Nullable OnCardSwipedListener listener) {
        this.swipedListener = listener;
    }

    public int getTopAdapterPosition() {
        return topPosition;
    }

    public boolean hasCards() {
        return adapter != null && topPosition < adapter.getCount();
    }

    public void swipeTopRight() {
        swipeProgrammatically(Direction.RIGHT);
    }

    public void swipeTopLeft() {
        swipeProgrammatically(Direction.LEFT);
    }

    /**
     * Возвращает верхнюю карточку (top-most) или null, если карточек нет.
     */
    @Nullable
    public View getTopCard() {
        return getTopCardView();
    }

    /**
     * Убирает верхнюю карточку без анимации и без вызова onCardSwiped.
     */
    @Nullable
    public View popCard() {
        View top = getTopCardView();
        if (top == null) {
            return null;
        }

        try {
            removeView(top);
        } catch (Exception ex) {
            Log.w(TAG, "popCard: removeView failed: " + ex);
            return null;
        }

        topPosition += 1;
        promoteRemainingCardsAnimated();
        fillStackIfNeeded();
        bindTopCardGesture();
        return top;
    }

    /**
     * Убирает верхнюю карточку без анимации, но уведомляет слушатель как о свайпе.
     */
    @Nullable
    public View popCard(@NonNull Direction direction) {
        View top = getTopCardView();
        if (top == null) {
            return null;
        }

        int swipedPosition = topPosition;

        try {
            removeView(top);
        } catch (Exception ex) {
            Log.w(TAG, "popCard(direction): removeView failed: " + ex);
            return null;
        }

        topPosition += 1;
        promoteRemainingCardsAnimated();
        fillStackIfNeeded();
        bindTopCardGesture();

        OnCardSwipedListener l = swipedListener;
        if (l != null) {
            l.onCardSwiped(top, swipedPosition, direction);
        }

        return top;
    }

    private void swipeProgrammatically(@NonNull Direction dir) {
        View top = getTopCardView();
        if (top == null) {
            return;
        }
        float targetX = dir == Direction.RIGHT ? getWidth() * 1.2f : -getWidth() * 1.2f;
        animateOut(top, targetX, dir);
    }

    @Nullable
    private View getTopCardView() {
        if (getChildCount() == 0) {
            return null;
        }
        return getChildAt(getChildCount() - 1);
    }

    @Nullable
    private View getSecondCardView() {
        if (getChildCount() < 2) {
            return null;
        }
        return getChildAt(getChildCount() - 2);
    }

    private void buildInitialStack() {
        removeAllViews();

        if (adapter == null) {
            return;
        }

        int count = adapter.getCount();
        if (topPosition >= count) {
            return;
        }

        int end = Math.min(count, topPosition + MAX_VISIBLE);

        // Важно: добавляет снизу вверх, последней добавится topPosition и будет top-most.
        for (int i = end - 1; i >= topPosition; i--) {
            View v = adapter.getView(i, this);
            addView(v, new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        applyStackTransformsImmediate();
        bindTopCardGesture();
    }

    private void applyStackTransformsImmediate() {
        int n = getChildCount();
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < n; i++) {
            View child = getChildAt(i);
            int indexFromTop = (n - 1) - i;

            float scale = 1f - (SCALE_STEP * indexFromTop);
            float ty = (TRANSLATION_Y_STEP_DP * density) * indexFromTop;

            child.setScaleX(scale);
            child.setScaleY(scale);
            child.setTranslationY(ty);
        }
    }

    private void promoteRemainingCardsAnimated() {
        int n = getChildCount();
        float density = getResources().getDisplayMetrics().density;

        for (int i = 0; i < n; i++) {
            View child = getChildAt(i);
            int indexFromTop = (n - 1) - i;

            float targetScale = 1f - (SCALE_STEP * indexFromTop);
            float targetTy = (TRANSLATION_Y_STEP_DP * density) * indexFromTop;

            child.animate()
                    .scaleX(targetScale)
                    .scaleY(targetScale)
                    .translationY(targetTy)
                    .setDuration(ANIM_DURATION_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void fillStackIfNeeded() {
        if (adapter == null) {
            return;
        }

        // Додерживает количество детей до MAX_VISIBLE, добавляя снизу.
        while (getChildCount() < MAX_VISIBLE) {
            int nextIndex = topPosition + getChildCount();
            if (nextIndex >= adapter.getCount()) {
                break;
            }

            View newBottom = adapter.getView(nextIndex, this);

            // Новый элемент добавляется в "низ" стека, то есть в начало списка детей.
            // При этом он должен быть визуально "самым дальним".
            addView(newBottom, 0, new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));

            // Ставит начальные трансформы сразу (без миганий), потом общий promote анимирует.
            applyStackTransformsImmediate();
        }
    }

    private void bindTopCardGesture() {
        View top = getTopCardView();
        if (top == null) {
            return;
        }

        top.setOnTouchListener((v, event) -> handleTouchOnTop(v, event));
    }

    private boolean handleTouchOnTop(@NonNull View card, @NonNull MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                downX = e.getRawX();
                downY = e.getRawY();
                isDragging = false;
                card.animate().cancel();
                return true;
            }
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getRawX() - downX;
                float dy = e.getRawY() - downY;

                if (!isDragging) {
                    if (Math.hypot(dx, dy) > touchSlop) {
                        isDragging = true;
                    } else {
                        return true;
                    }
                }

                card.setTranslationX(dx);
                card.setTranslationY(dy);

                float progress = Math.min(1f, Math.abs(dx) / swipeThresholdPx);
                float rotation = (dx / swipeThresholdPx) * maxRotationDeg;
                card.setRotation(rotation);

                Direction dir = dx >= 0 ? Direction.RIGHT : Direction.LEFT;
                updateOverlays(card, progress, dir);

                // Плавно подтягивает заднюю карточку вперёд во время drag.
                updateBackCardDuringDrag(progress);

                OnCardSwipedListener l = swipedListener;
                if (l != null) {
                    l.onCardDrag(card, progress, dir);
                }

                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                float dx = e.getRawX() - downX;
                if (!isDragging) {
                    resetCard(card);
                    return true;
                }

                if (Math.abs(dx) >= swipeThresholdPx) {
                    Direction dir = dx >= 0 ? Direction.RIGHT : Direction.LEFT;
                    float targetX = dx >= 0 ? getWidth() * 1.2f : -getWidth() * 1.2f;
                    animateOut(card, targetX, dir);
                } else {
                    resetCard(card);
                }
                return true;
            }
            default:
                return false;
        }
    }

    private void updateBackCardDuringDrag(float progress01) {
        View back = getSecondCardView();
        if (back == null) {
            return;
        }

        float density = getResources().getDisplayMetrics().density;

        float baseScale = 1f - SCALE_STEP; // позиция "вторая сверху"
        float baseTy = TRANSLATION_Y_STEP_DP * density;

        float t = clamp01(progress01 * BACK_CARD_PULL_FACTOR);

        // Интерполяция к позиции топ-карты (scale=1, ty=0)
        back.setScaleX(lerp(baseScale, 1f, t));
        back.setScaleY(lerp(baseScale, 1f, t));
        back.setTranslationY(lerp(baseTy, 0f, t));
    }

    private void resetCard(@NonNull View card) {
        updateOverlays(card, 0f, Direction.RIGHT);

        // Возвращает заднюю карточку обратно на базовую позицию.
        promoteRemainingCardsAnimated();

        card.animate()
                .translationX(0f)
                .translationY(0f)
                .rotation(0f)
                .setDuration(ANIM_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void animateOut(@NonNull View card, float targetX, @NonNull Direction dir) {
        updateOverlays(card, 1f, dir);

        // Доводит заднюю карточку максимально вперёд перед удалением топ-карты.
        updateBackCardDuringDrag(1f);

        card.animate()
                .translationX(targetX)
                .translationY(card.getTranslationY())
                .rotation((dir == Direction.RIGHT ? 1f : -1f) * maxRotationDeg)
                .setDuration(ANIM_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        card.animate().setListener(null);
                        onCardRemoved(card, dir);
                    }
                })
                .start();
    }

    private void onCardRemoved(@NonNull View card, @NonNull Direction dir) {
        int swipedPosition = topPosition;

        try {
            removeView(card);
        } catch (Exception ex) {
            Log.w(TAG, "removeView failed: " + ex);
        }

        topPosition += 1;

        // Ключевой момент: НЕ пересобирает весь стек.
        // Просто анимирует оставшиеся карточки вперёд и достраивает одну снизу.
        promoteRemainingCardsAnimated();
        fillStackIfNeeded();
        bindTopCardGesture();

        OnCardSwipedListener l = swipedListener;
        if (l != null) {
            l.onCardSwiped(card, swipedPosition, dir);
        }
    }

    private void updateOverlays(@NonNull View card, float progress, @NonNull Direction dir) {
        View likeBg = card.findViewById(R.id.swipeLikeBg);
        View dislikeBg = card.findViewById(R.id.swipeDislikeBg);
        View likeOverlay = card.findViewById(R.id.swipeLikeOverlay);
        View dislikeOverlay = card.findViewById(R.id.swipeDislikeOverlay);

        if (likeBg != null) likeBg.setAlpha(dir == Direction.RIGHT ? progress : 0f);
        if (dislikeBg != null) dislikeBg.setAlpha(dir == Direction.LEFT ? progress : 0f);

        if (likeOverlay != null) likeOverlay.setAlpha(dir == Direction.RIGHT ? progress : 0f);
        if (dislikeOverlay != null) dislikeOverlay.setAlpha(dir == Direction.LEFT ? progress : 0f);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }
}
