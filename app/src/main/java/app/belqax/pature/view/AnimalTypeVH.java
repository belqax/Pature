package app.belqax.pature.view;

import android.view.View;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;

import app.belqax.pature.R;
import app.belqax.pature.infc.StepActionListener;

public class AnimalTypeVH extends RecyclerView.ViewHolder {

    public AnimalTypeVH(View itemView, StepActionListener listener) {
        super(itemView);

        Button catBtn = itemView.findViewById(R.id.catBtn);
        Button dogBtn = itemView.findViewById(R.id.dogBtn);

        catBtn.setOnClickListener(v -> listener.onStepCompleted());
        dogBtn.setOnClickListener(v -> listener.onStepCompleted());
    }
}

