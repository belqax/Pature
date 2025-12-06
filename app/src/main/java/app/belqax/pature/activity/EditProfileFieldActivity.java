package app.belqax.pature.activity;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import app.belqax.pature.R;

public class EditProfileFieldActivity extends AppCompatActivity {

    public static final String EXTRA_FIELD_TYPE = "extra_field_type";

    public static final String FIELD_TYPE_NAME = "name";
    public static final String FIELD_TYPE_AGE = "age";
    public static final String FIELD_TYPE_ABOUT = "about";
    public static final String FIELD_TYPE_LOCATION = "location";

    private static final String TAG = "EditProfileField";

    private TextView titleView;
    private EditText inputView;
    private Button saveButton;
    private ImageButton closeButton;

    @Nullable
    private String fieldType;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Убирает стандартный тулбар, если он есть в теме
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_edit_profile_field);

        fieldType = getIntent().getStringExtra(EXTRA_FIELD_TYPE);
        if (fieldType == null) {
            fieldType = FIELD_TYPE_NAME;
        }

        bindViews();
        configureForFieldType(fieldType);
        setupListeners();
    }

    private void bindViews() {
        titleView = findViewById(R.id.editFieldTitle);
        inputView = findViewById(R.id.editFieldInput);
        saveButton = findViewById(R.id.editFieldSaveButton);
        closeButton = findViewById(R.id.editFieldCloseButton);
    }

    private void configureForFieldType(@NonNull String type) {
        switch (type) {
            case FIELD_TYPE_NAME:
                titleView.setText(R.string.profile_edit_name_title);
                inputView.setHint(R.string.profile_edit_name_hint);
                inputView.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                inputView.setFilters(new InputFilter[]{
                        new InputFilter.LengthFilter(50)
                });
                break;

            case FIELD_TYPE_AGE:
                titleView.setText(R.string.profile_edit_age_title);
                inputView.setHint(R.string.profile_edit_age_hint);
                inputView.setInputType(InputType.TYPE_CLASS_NUMBER);
                inputView.setFilters(new InputFilter[]{
                        new InputFilter.LengthFilter(3)
                });
                break;

            case FIELD_TYPE_ABOUT:
                titleView.setText(R.string.profile_edit_about_title);
                inputView.setHint(R.string.profile_edit_about_hint);
                inputView.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                inputView.setMinLines(4);
                inputView.setMaxLines(6);
                inputView.setFilters(new InputFilter[]{
                        new InputFilter.LengthFilter(500)
                });
                break;

            case FIELD_TYPE_LOCATION:
                titleView.setText(R.string.profile_edit_location_title);
                inputView.setHint(R.string.profile_edit_location_hint);
                inputView.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                inputView.setFilters(new InputFilter[]{
                        new InputFilter.LengthFilter(100)
                });
                break;

            default:
                Log.w(TAG, "Unknown field type: " + type);
                titleView.setText(R.string.profile_edit_default_title);
                inputView.setHint(R.string.profile_edit_default_hint);
                inputView.setInputType(InputType.TYPE_CLASS_TEXT);
                break;
        }
    }

    private void setupListeners() {
        closeButton.setOnClickListener(v -> closeWithAnimation());

        saveButton.setOnClickListener(v -> {
            String value = inputView.getText() != null
                    ? inputView.getText().toString().trim()
                    : "";

            if (!validateInput(fieldType, value)) {
                return;
            }

            // Здесь позже нужно сохранить в ViewModel/репозиторий/сервер.
            Toast.makeText(
                    this,
                    R.string.profile_edit_save_stub,
                    Toast.LENGTH_SHORT
            ).show();

            closeWithAnimation();
        });
    }

    private boolean validateInput(@NonNull String type, @NonNull String value) {
        if (type.equals(FIELD_TYPE_NAME)) {
            if (value.isEmpty()) {
                inputView.setError(getString(R.string.profile_edit_name_error_empty));
                return false;
            }
        } else if (type.equals(FIELD_TYPE_AGE)) {
            if (value.isEmpty()) {
                inputView.setError(getString(R.string.profile_edit_age_error_empty));
                return false;
            }
            try {
                int age = Integer.parseInt(value);
                if (age < 0 || age > 120) {
                    inputView.setError(getString(R.string.profile_edit_age_error_range));
                    return false;
                }
            } catch (NumberFormatException e) {
                inputView.setError(getString(R.string.profile_edit_age_error_format));
                return false;
            }
        }
        // Остальные поля можно валидировать по месту, если понадобится.
        return true;
    }

    @Override
    public void onBackPressed() {
        closeWithAnimation();
    }

    private void closeWithAnimation() {
        try {
            finish();
            overridePendingTransition(R.anim.stay, R.anim.slide_out_down);
        } catch (Exception e) {
            Log.w(TAG, "overridePendingTransition on finish failed", e);
            super.finish();
        }
    }
}
