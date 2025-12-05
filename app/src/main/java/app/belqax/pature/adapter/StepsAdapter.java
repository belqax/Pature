package app.belqax.pature.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import app.belqax.pature.R;
import app.belqax.pature.infc.StepActionListener;
import app.belqax.pature.model.Step;
import app.belqax.pature.view.AnimalTypeVH;
import app.belqax.pature.view.BirthdayVH;
import app.belqax.pature.view.NameVH;
import app.belqax.pature.view.PurposeVH;

public class StepsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Step> steps;
    private final StepActionListener listener;

    public StepsAdapter(List<Step> steps, StepActionListener listener) {
        this.steps = steps;
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    @Override
    public int getItemViewType(int position) {
        Step step = steps.get(position);
        switch (step) {
            case ANIMAL_TYPE: return 1;
            case PET_NAME: return 2;
            case BIRTHDAY: return 3;
            case PURPOSE: return 4;
            default: return -1;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType
    ) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case 1:
                View v1 = inflater.inflate(R.layout.step_animal_type, parent, false);
                return new AnimalTypeVH(v1, listener);

            case 2:
                View v2 = inflater.inflate(R.layout.step_name, parent, false);
                return new NameVH(v2, listener);

            case 3:
                View v3 = inflater.inflate(R.layout.step_birthday, parent, false);
                return new BirthdayVH(v3, listener);

            case 4:
                View v4 = inflater.inflate(R.layout.step_purpose, parent, false);
                return new PurposeVH(v4, listener);

            default:
                throw new IllegalStateException("Неизвестный viewType: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {}
}
