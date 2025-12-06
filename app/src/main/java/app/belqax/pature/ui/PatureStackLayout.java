package app.belqax.pature.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import app.belqax.pature.adapter.CardAdapter;
import app.belqax.pature.utils.GestureProcessor;

public class PatureStackLayout extends ViewGroup {

    private static final int MAX_VISIBLE_CARDS = 3;
    private static final float SCALE_STEP = 0.04f;
    private static final float TRANSLATION_Y_STEP = 24f;
    private static final long ANIMATION_DURATION_MS = 220L;

    @Nullable
    private CardAdapter adapter;

    // cardViews[0] — нижняя, cardViews[last] — верхняя
    private final List<View> cardViews = new ArrayList<>();
    private int adapterTopIndex = 0;

    private final DecelerateInterpolator interpolator = new DecelerateInterpolator();

    public PatureStackLayout(Context context) {
        super(context);
        init();
    }

    public PatureStackLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PatureStackLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClipChildren(false);
        setClipToPadding(false);
    }

    public void setAdapter(@Nullable CardAdapter adapter) {
        this.adapter = adapter;
        adapterTopIndex = 0;

        removeAllViews();
        cardViews.clear();

        if (adapter != null && adapter.getCount() > 0) {
            int toAdd = Math.min(adapter.getCount(), MAX_VISIBLE_CARDS);
            for (int i = 0; i < toAdd; i++) {
                View card = adapter.getView(adapterTopIndex + i, this);
                resetCardTransform(card);
                cardViews.add(card);
                addView(card);
            }
            // первый показ без анимации
            applyStackTransforms(false, null);
        }

        GestureProcessor.attachToStack(this);
    }

    @Nullable
    public View getTopCard() {
        if (cardViews.isEmpty()) {
            return null;
        }
        return cardViews.get(cardViews.size() - 1);
    }

    public void popCard() {
        if (cardViews.isEmpty()) {
            return;
        }

        View top = cardViews.remove(cardViews.size() - 1);
        removeView(top);

        adapterTopIndex++;

        View newBackCard = null;

        if (adapter != null && adapterTopIndex + cardViews.size() < adapter.getCount()) {
            int nextPos = adapterTopIndex + cardViews.size();
            newBackCard = adapter.getView(nextPos, this);
            resetCardTransform(newBackCard);
            // добавляет в самый низ стека
            cardViews.add(0, newBackCard);
            addView(newBackCard, 0);
        }

        // плавно меняет размер/позицию существующих карточек,
        // новая задняя сразу ставится в конечное положение без анимации
        applyStackTransforms(true, newBackCard);
    }

    private void resetCardTransform(@NonNull View card) {
        card.setAlpha(1f);
        card.setScaleX(1f);
        card.setScaleY(1f);
        card.setTranslationX(0f);
        card.setTranslationY(0f);
        card.setRotation(0f);
    }

    /**
     * @param animate      нужно ли анимировать уже существующие карточки
     * @param newBackCard  только что добавленная нижняя карточка (для неё анимация выключена)
     */
    private void applyStackTransforms(boolean animate, @Nullable View newBackCard) {
        int size = cardViews.size();
        if (size == 0) {
            return;
        }

        for (int i = 0; i < size; i++) {
            View card = cardViews.get(i);

            int levelFromTop = size - 1 - i; // 0 — верхняя
            float targetScale = 1f - (levelFromTop * SCALE_STEP);
            float targetTransY = levelFromTop * TRANSLATION_Y_STEP;

            // отменяет возможные старые анимации
            card.animate().cancel();

            if (!animate) {
                card.setScaleX(targetScale);
                card.setScaleY(targetScale);
                card.setTranslationY(targetTransY);
            } else {
                // новая задняя карточка сразу ставится в конечное положение, без движения
                if (card == newBackCard) {
                    card.setScaleX(targetScale);
                    card.setScaleY(targetScale);
                    card.setTranslationY(targetTransY);
                } else {
                    card.animate()
                            .scaleX(targetScale)
                            .scaleY(targetScale)
                            .translationY(targetTransY)
                            .setDuration(ANIMATION_DURATION_MS)
                            .setInterpolator(interpolator)
                            .start();
                }
            }
        }
    }

    // region ViewGroup

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);

        int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);

        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            measureChild(c, childWidthSpec, childHeightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = getWidth();
        int h = getHeight();
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            c.layout(0, 0, w, h);
        }
    }

    // endregion
}
