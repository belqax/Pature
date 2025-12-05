package app.belqax.pature.helper;


import android.content.Context;
import androidx.recyclerview.widget.LinearSmoothScroller;

public class SmoothScrollerHelper extends LinearSmoothScroller {

    public SmoothScrollerHelper(Context context) {
        super(context);
    }

    @Override
    protected int getVerticalSnapPreference() {
        return SNAP_TO_START;
    }

    @Override
    protected int getHorizontalSnapPreference() {
        return SNAP_TO_START;
    }

    @Override
    protected float calculateSpeedPerPixel(android.util.DisplayMetrics displayMetrics) {
        return 40f / displayMetrics.densityDpi; // регулирует скорость
    }
}
