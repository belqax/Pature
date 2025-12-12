package app.belqax.pature.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import app.belqax.pature.R;
import app.belqax.pature.data.repository.AddressRepository;
import app.belqax.pature.data.repository.AddressRepository.AddressError;
import app.belqax.pature.data.repository.AddressRepository.SuggestionDto;

public class EditProfileFieldActivity extends AppCompatActivity {

    public static final String EXTRA_FIELD_TYPE = "extra_field_type";
    public static final String EXTRA_INITIAL_VALUE = "extra_initial_value";

    public static final String EXTRA_RESULT_VALUE = "extra_result_value";
    public static final String EXTRA_RESULT_LOCATION = "extra_result_location";

    public static final String FIELD_TYPE_NAME = "name";
    public static final String FIELD_TYPE_AGE = "age";
    public static final String FIELD_TYPE_ABOUT = "about";
    public static final String FIELD_TYPE_LOCATION = "location";

    private static final String TAG = "EditProfileField";

    private static final int ADDRESS_AUTOCOMPLETE_DEBOUNCE_MS = 400;
    private static final int ADDRESS_AUTOCOMPLETE_MIN_LENGTH = 3;
    private static final int ADDRESS_AUTOCOMPLETE_LIMIT = 5;
    private static final String ADDRESS_AUTOCOMPLETE_LANG = "ru";

    private TextView titleView;
    private EditText inputView;
    private Button saveButton;
    private ImageButton closeButton;

    @Nullable
    private View addressSuggestionsContainer;
    @Nullable
    private ListView addressSuggestionsListView;

    @Nullable
    private ArrayAdapter<String> addressSuggestionsAdapter;

    @NonNull
    private final List<String> addressSuggestions = new ArrayList<>();

    @NonNull
    private final List<SuggestionDto> addressSuggestionDtos = new ArrayList<>();

    @Nullable
    private String fieldType;

    @Nullable
    private AddressRepository addressRepository;

    @Nullable
    private SuggestionDto selectedSuggestion;

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    private Runnable pendingAddressRequest;

    @Nullable
    private String lastAddressQuery = null;

    private boolean isProgrammaticTextChange = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_edit_profile_field);

        fieldType = getIntent().getStringExtra(EXTRA_FIELD_TYPE);
        String initialValue = getIntent().getStringExtra(EXTRA_INITIAL_VALUE);

        titleView = findViewById(R.id.editFieldTitle);
        inputView = findViewById(R.id.editFieldInput);
        saveButton = findViewById(R.id.editFieldSaveButton);
        closeButton = findViewById(R.id.editFieldCloseButton);

        addressSuggestionsContainer = findViewById(R.id.addressSuggestionsContainer);
        addressSuggestionsListView = findViewById(R.id.addressSuggestionsList);

        if (fieldType == null) {
            Log.w(TAG, "onCreate: fieldType is null, finishing");
            finish();
            return;
        }

        setupInputForFieldType(fieldType, initialValue);
        setupButtons();

        if (FIELD_TYPE_LOCATION.equals(fieldType)) {
            addressRepository = AddressRepository.create(getApplicationContext());
            setupAddressAutocomplete();
        } else {
            hideAddressSuggestions();
        }
    }

    private void setupInputForFieldType(@NonNull String fieldType,
                                        @Nullable String initialValue) {
        switch (fieldType) {
            case FIELD_TYPE_NAME:
                inputView.setHint(R.string.profile_edit_name_hint);
                inputView.setInputType(
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_FLAG_CAP_WORDS
                );
                inputView.setFilters(new InputFilter[]{
                        new InputFilter.LengthFilter(64)
                });
                break;

            case FIELD_TYPE_AGE:
                inputView.setHint(R.string.profile_edit_age_hint);
                inputView.setInputType(
                        InputType.TYPE_CLASS_NUMBER
                );
                inputView.setFilters(new InputFilter[]{
                        new InputFilter.LengthFilter(3)
                });
                break;

            case FIELD_TYPE_ABOUT:
                titleView.setText(R.string.profile_edit_about_title);
                inputView.setHint(R.string.profile_edit_about_hint);
                inputView.setInputType(
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                );
                inputView.setMinLines(4);
                inputView.setMaxLines(6);
                inputView.setFilters(new InputFilter[]{
                        new InputFilter.LengthFilter(500)
                });
                break;

            case FIELD_TYPE_LOCATION:
                inputView.setHint(R.string.profile_edit_location_hint);
                inputView.setInputType(
                        InputType.TYPE_CLASS_TEXT
                                | InputType.TYPE_TEXT_FLAG_CAP_WORDS
                );
                inputView.setFilters(new InputFilter[]{
                        new InputFilter.LengthFilter(256)
                });
                break;

            default:
                inputView.setInputType(InputType.TYPE_CLASS_TEXT);
                inputView.setFilters(new InputFilter[]{
                        new InputFilter.LengthFilter(256)
                });
                break;
        }

        if (!TextUtils.isEmpty(initialValue)) {
            isProgrammaticTextChange = true;
            inputView.setText(initialValue);
            inputView.setSelection(inputView.getText().length());
            isProgrammaticTextChange = false;
        }
    }

    private void setupButtons() {
        saveButton.setOnClickListener(v -> onSaveClicked());
        closeButton.setOnClickListener(v -> closeWithAnimation());
    }

    private void onSaveClicked() {
        if (fieldType == null) {
            finish();
            return;
        }

        String value = inputView.getText() != null ? inputView.getText().toString() : "";

        switch (fieldType) {
            case FIELD_TYPE_NAME:
                value = value.trim();
                if (value.isEmpty()) {
                    Toast.makeText(this, R.string.profile_name_empty_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                break;

            case FIELD_TYPE_AGE:
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        int age = Integer.parseInt(trimmed);
                        if (age <= 0 || age > 120) {
                            Toast.makeText(this, R.string.profile_age_invalid_error, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, R.string.profile_age_invalid_error, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                value = trimmed;
                break;

            case FIELD_TYPE_ABOUT:
                String aboutTrimmed = value.trim();
                if (aboutTrimmed.length() > 512) {
                    Toast.makeText(this, R.string.profile_about_too_long_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                value = aboutTrimmed;
                break;

            case FIELD_TYPE_LOCATION:
                String locationTrimmed = value.trim();
                if (locationTrimmed.length() > 256) {
                    Toast.makeText(this, R.string.profile_location_too_long_error, Toast.LENGTH_SHORT).show();
                    return;
                }
                value = locationTrimmed;
                break;

            default:
                value = value.trim();
                break;
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_FIELD_TYPE, fieldType);

        if (FIELD_TYPE_LOCATION.equals(fieldType)) {
            LocationPayload payload;

            if (selectedSuggestion != null) {
                payload = LocationPayload.fromSuggestion(selectedSuggestion);
            } else {
                payload = LocationPayload.fromFreeText(value);
            }

            result.putExtra(EXTRA_RESULT_VALUE, payload.location_formatted != null ? payload.location_formatted : "");
            result.putExtra(EXTRA_RESULT_LOCATION, payload);

            Log.d(TAG, "onSaveClicked(location): formatted=" + payload.location_formatted
                    + " city=" + payload.location_city
                    + " state=" + payload.location_state
                    + " country=" + payload.location_country
                    + " postcode=" + payload.location_postcode
                    + " lat=" + payload.location_lat
                    + " lon=" + payload.location_lon
                    + " result_type=" + payload.location_result_type
                    + " confidence=" + payload.location_confidence);
        } else {
            result.putExtra(EXTRA_RESULT_VALUE, value);
        }

        setResult(RESULT_OK, result);
        closeWithAnimation();
    }

    // region Address autocomplete

    private void setupAddressAutocomplete() {
        if (addressSuggestionsContainer == null || addressSuggestionsListView == null) {
            Log.w(TAG, "setupAddressAutocomplete: views for suggestions are missing");
            return;
        }

        addressSuggestionsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                addressSuggestions
        );
        addressSuggestionsListView.setAdapter(addressSuggestionsAdapter);

        addressSuggestionsListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= addressSuggestionDtos.size()) {
                return;
            }

            SuggestionDto dto = addressSuggestionDtos.get(position);
            onSuggestionSelected(dto);
        });

        inputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isProgrammaticTextChange) {
                    return;
                }

                String query = s != null ? s.toString().trim() : "";

                selectedSuggestion = null;

                handleAddressTextChanged(query);
            }
        });
    }

    private void onSuggestionSelected(@NonNull SuggestionDto dto) {
        selectedSuggestion = dto;

        String formatted = dto.getFormatted();
        if (formatted == null) {
            formatted = "";
        }
        formatted = formatted.trim();

        isProgrammaticTextChange = true;
        inputView.setText(formatted);
        inputView.setSelection(formatted.length());
        isProgrammaticTextChange = false;

        clearAddressSuggestions();

        Log.d(TAG, "onSuggestionSelected: formatted=" + formatted
                + " city=" + dto.getCity()
                + " state=" + dto.getState()
                + " country=" + dto.getCountry()
                + " postcode=" + dto.getPostcode()
                + " lat=" + dto.getLat()
                + " lon=" + dto.getLon()
                + " result_type=" + dto.getResultType()
                + " confidence=" + dto.getConfidence());
    }

    private void handleAddressTextChanged(@NonNull String query) {
        lastAddressQuery = query;

        if (query.length() < ADDRESS_AUTOCOMPLETE_MIN_LENGTH) {
            clearAddressSuggestions();
            return;
        }

        if (pendingAddressRequest != null) {
            handler.removeCallbacks(pendingAddressRequest);
        }

        pendingAddressRequest = () -> requestAddressSuggestions(query);
        handler.postDelayed(pendingAddressRequest, ADDRESS_AUTOCOMPLETE_DEBOUNCE_MS);
    }

    private void requestAddressSuggestions(@NonNull String query) {
        if (addressRepository == null) {
            Log.w(TAG, "requestAddressSuggestions: repository is null");
            return;
        }

        if (!query.equals(lastAddressQuery)) {
            return;
        }

        addressRepository.autocomplete(
                query,
                ADDRESS_AUTOCOMPLETE_LIMIT,
                ADDRESS_AUTOCOMPLETE_LANG,
                null,
                new AddressRepository.SuggestionsCallback() {
                    @Override
                    public void onSuccess(@NonNull List<SuggestionDto> suggestions) {
                        updateAddressSuggestionsFromDtos(suggestions);
                    }

                    @Override
                    public void onError(@NonNull AddressError error) {
                        Log.w(TAG, "autocomplete error: " + error.getMessage());
                        clearAddressSuggestions();
                    }
                }
        );
    }

    private void updateAddressSuggestionsFromDtos(@NonNull List<SuggestionDto> dtos) {
        if (addressSuggestionsAdapter == null || addressSuggestionsContainer == null) {
            return;
        }

        addressSuggestions.clear();
        addressSuggestionDtos.clear();

        for (SuggestionDto dto : dtos) {
            String formatted = dto.getFormatted();
            if (formatted != null) {
                String trimmed = formatted.trim();
                if (!trimmed.isEmpty()) {
                    addressSuggestions.add(trimmed);
                    addressSuggestionDtos.add(dto);
                }
            }
        }

        addressSuggestionsAdapter.notifyDataSetChanged();

        if (addressSuggestions.isEmpty()) {
            hideAddressSuggestions();
        } else {
            showAddressSuggestions();
        }
    }

    private void clearAddressSuggestions() {
        if (addressSuggestionsAdapter == null) {
            return;
        }

        addressSuggestions.clear();
        addressSuggestionDtos.clear();

        addressSuggestionsAdapter.notifyDataSetChanged();
        hideAddressSuggestions();
    }

    private void showAddressSuggestions() {
        if (addressSuggestionsContainer != null) {
            addressSuggestionsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void hideAddressSuggestions() {
        if (addressSuggestionsContainer != null) {
            addressSuggestionsContainer.setVisibility(View.GONE);
        }
    }

    // endregion

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingAddressRequest != null) {
            handler.removeCallbacks(pendingAddressRequest);
            pendingAddressRequest = null;
        }
    }

    public static final class LocationPayload implements Serializable {

        @Nullable public final String location_formatted;
        @Nullable public final String location_city;
        @Nullable public final String location_state;
        @Nullable public final String location_country;
        @Nullable public final String location_postcode;

        @Nullable public final Double location_lat;
        @Nullable public final Double location_lon;

        @Nullable public final String location_result_type;
        @Nullable public final Double location_confidence;

        private LocationPayload(
                @Nullable String locationFormatted,
                @Nullable String locationCity,
                @Nullable String locationState,
                @Nullable String locationCountry,
                @Nullable String locationPostcode,
                @Nullable Double locationLat,
                @Nullable Double locationLon,
                @Nullable String locationResultType,
                @Nullable Double locationConfidence
        ) {
            this.location_formatted = locationFormatted;
            this.location_city = locationCity;
            this.location_state = locationState;
            this.location_country = locationCountry;
            this.location_postcode = locationPostcode;
            this.location_lat = locationLat;
            this.location_lon = locationLon;
            this.location_result_type = locationResultType;
            this.location_confidence = locationConfidence;
        }

        @NonNull
        public static LocationPayload fromSuggestion(@NonNull SuggestionDto s) {
            String formatted = safeTrimToNull(s.getFormatted());
            return new LocationPayload(
                    formatted,
                    safeTrimToNull(s.getCity()),
                    safeTrimToNull(s.getState()),
                    safeTrimToNull(s.getCountry()),
                    safeTrimToNull(s.getPostcode()),
                    s.getLat(),
                    s.getLon(),
                    safeTrimToNull(s.getResultType()),
                    s.getConfidence()
            );
        }

        @NonNull
        public static LocationPayload fromFreeText(@NonNull String text) {
            String formatted = safeTrimToNull(text);
            return new LocationPayload(
                    formatted,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        @Nullable
        private static String safeTrimToNull(@Nullable String v) {
            if (v == null) {
                return null;
            }
            String t = v.trim();
            if (t.isEmpty()) {
                return null;
            }
            return t;
        }
    }
}
