package app.belqax.pature.view;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.recyclerview.widget.RecyclerView;

import app.belqax.pature.R;
import app.belqax.pature.infc.StepActionListener;
import app.belqax.pature.utils.KeyboardUtils;

public class NameVH extends RecyclerView.ViewHolder {

    public NameVH(View itemView, StepActionListener listener) {
        super(itemView);

        EditText nameInput = itemView.findViewById(R.id.nameInput);
        Button nextBtn = itemView.findViewById(R.id.nextBtn);

        nextBtn.setOnClickListener(v -> {
            String text = nameInput.getText().toString().trim();

            if (!TextUtils.isEmpty(text)) {
                KeyboardUtils.hide(itemView.getContext(), nameInput);
                nameInput.clearFocus();
                listener.onStepCompleted();

            } else {
                nameInput.setError("Введите имя");
            }
        });
    }
}
