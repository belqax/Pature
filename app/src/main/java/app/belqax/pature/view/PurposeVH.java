package app.belqax.pature.view;

import android.view.View;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;

import app.belqax.pature.R;
import app.belqax.pature.infc.StepActionListener;

public class PurposeVH extends RecyclerView.ViewHolder {

    public PurposeVH(View itemView, StepActionListener listener) {
        super(itemView);

        Button breeding = itemView.findViewById(R.id.breedingBtn);
        Button walk = itemView.findViewById(R.id.walkBtn);
        Button fun = itemView.findViewById(R.id.funBtn);
        Button social = itemView.findViewById(R.id.socialBtn);

        View.OnClickListener clickListener = v -> {
            // Можно сохранить выбранную цель
            listener.onStepCompleted();
        };

        breeding.setOnClickListener(clickListener);
        walk.setOnClickListener(clickListener);
        fun.setOnClickListener(clickListener);
        social.setOnClickListener(clickListener);
    }
}
