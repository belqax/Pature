package app.belqax.pature.ui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Отвечает за анимацию вылета карточки из стека.
 */
public final class CardAnimator {

    // Длительность анимации вылета
    private static final long FLY_OUT_DURATION_MS = 220L;

    // Дополнительное смещение по Y, чтобы карта немного "проваливалась" при вылете
    private static final float FLY_OUT_EXTRA_TRANSLATION_Y = 40f;

    // Дополнительный угол поворота при вылете
    private static final float FLY_OUT_EXTRA_ROTATION_DEGREES = 8f;

    private CardAnimator() {
        // Запрещает создавать экземпляры
    }

    /**
     * Анимирует вылет верхней карточки за пределы экрана.
     *
     * @param card              карточка, которую нужно анимировать
     * @param targetTranslationX конечное смещение по оси X (за пределами экрана)
     * @param onEnd             колбэк, который вызывается после завершения анимации
     */
    public static void flyOut(@NonNull View card,
                              float targetTranslationX,
                              @Nullable Runnable onEnd) {

        // Если вью уже ни к чему не прикреплена, не анимирует, просто завершает
        if (!card.isAttachedToWindow()) {
            if (onEnd != null) {
                onEnd.run();
            }
            return;
        }

        // Отменяет возможную предыдущую анимацию
        card.animate().cancel();

        float directionSign = targetTranslationX >= 0f ? 1f : -1f;
        float targetRotation = card.getRotation()
                + (directionSign * FLY_OUT_EXTRA_ROTATION_DEGREES);

        card.animate()
                .translationX(targetTranslationX)
                .translationY(card.getTranslationY() + FLY_OUT_EXTRA_TRANSLATION_Y)
                .rotation(targetRotation)
                .alpha(0f)
                .setDuration(FLY_OUT_DURATION_MS)
                .withEndAction(() -> {
                    // На всякий случай отменяет анимацию, чтобы не было "хвостов"
                    card.animate().cancel();

                    if (onEnd != null) {
                        onEnd.run();
                    }
                })
                .start();
    }
}
