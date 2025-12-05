package app.belqax.pature.view;


import android.app.DatePickerDialog;
import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;

import app.belqax.pature.R;
import app.belqax.pature.infc.StepActionListener;

public class BirthdayVH extends RecyclerView.ViewHolder {

    public BirthdayVH(View itemView, StepActionListener listener) {
        super(itemView);

        TextInputEditText input = itemView.findViewById(R.id.birthInput);
        TextInputLayout layout = itemView.findViewById(R.id.birthLayout);
        MaterialButton next = itemView.findViewById(R.id.nextBtn);
        Calendar calendar = Calendar.getInstance();

        View.OnClickListener openPicker = v -> {

            Context context = itemView.getContext();

            DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    R.style.Theme_Pature_NativeDatePicker,   // твоя тема
                    (view, year, month, day) -> {

                        String formatted = String.format(Locale.getDefault(), "%02d.%02d.%04d",
                                day, month + 1, year);

                        input.setText(formatted);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            dialog.show();
        };

        next.setOnClickListener(v -> {
            if (input.getText() == null || input.getText().toString().trim().isEmpty()) {
                input.setError("Выберите дату");
            } else {
                listener.onStepCompleted();
            }
        });
        input.setOnClickListener(openPicker);
        layout.setOnClickListener(openPicker);
    }
}


