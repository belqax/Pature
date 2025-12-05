package app.belqax.pature.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.recyclerview.widget.RecyclerView;

public class LockSwipeRecyclerView extends RecyclerView {

    private float startX;
    private float startY;

    public LockSwipeRecyclerView(Context context) {
        super(context);
    }

    public LockSwipeRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LockSwipeRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {

        switch (e.getAction()) {

            case MotionEvent.ACTION_DOWN:
                startX = e.getX();
                startY = e.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(e.getX() - startX);
                float dy = Math.abs(e.getY() - startY);

                // Если движение горизонтальное — блокируем перехват события
                if (dx > dy) {
                    return false; // НЕ даём RecyclerView прокручивать горизонтально
                }
                break;
        }

        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return false; // блокируем обработку свайпа самим RecyclerView
    }
}
