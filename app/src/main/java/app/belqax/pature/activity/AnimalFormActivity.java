package app.belqax.pature.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import app.belqax.pature.R;
import app.belqax.pature.data.repository.AddressRepository;
import app.belqax.pature.data.repository.AnimalRepository;

public final class AnimalFormActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_ANIMAL_ID = "extra_animal_id";

    public static final String MODE_CREATE = "create";
    public static final String MODE_EDIT = "edit";

    private static final String TAG = "AnimalFormActivity";

    private static final String DATE_FMT = "yyyy-MM-dd";
    private static final int MAX_AGE_YEARS = 120;
    private static final int MAX_AGE_MONTHS = 11;

    private static final int ADDRESS_MIN_CHARS = 3;
    private static final int ADDRESS_LIMIT = 8;
    private static final long ADDRESS_DEBOUNCE_MS = 300L;

    // Views

    private TextInputLayout tilName;
    private TextInputLayout tilSpecies;
    private TextInputLayout tilBreed;
    private TextInputLayout tilSex;
    private TextInputLayout tilDob;
    private TextInputLayout tilAgeYears;
    private TextInputLayout tilAgeMonths;

    private TextInputLayout tilWeight;
    private TextInputLayout tilHeight;
    private TextInputLayout tilColor;
    private TextInputLayout tilColorOther;
    private TextInputLayout tilPattern;

    private TextInputLayout tilChipNumber;

    private TextInputLayout tilTemperament;
    private TextInputLayout tilDescription;

    private TextInputLayout tilAddress;

    private TextInputEditText etName;
    private MaterialAutoCompleteTextView acSpecies;
    private MaterialAutoCompleteTextView acBreed;
    private MaterialAutoCompleteTextView acSex;
    private TextInputEditText etDob;
    private TextInputEditText etAgeYears;
    private TextInputEditText etAgeMonths;

    private TextInputEditText etWeight;
    private TextInputEditText etHeight;
    private MaterialAutoCompleteTextView acColor;
    private TextInputEditText etColorOther;
    private MaterialAutoCompleteTextView acPattern;

    private MaterialCheckBox cbNeutered;
    private MaterialCheckBox cbVaccinated;
    private MaterialCheckBox cbChipped;
    private TextInputEditText etChipNumber;

    private TextInputEditText etTemperament;
    private TextInputEditText etDescription;

    private MaterialAutoCompleteTextView acAddress;

    private MaterialButton btnSave;

    private ImageButton backbutton;
    // Data
    private AnimalRepository animalRepo;
    private AddressRepository addressRepo;

    private String mode;
    private long animalId = -1;
    private boolean isBusy = false;

    private final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FMT, Locale.US);

    // Address autocomplete state
    private final Handler handler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable pendingAddressRunnable;

    @Nullable
    private List<AddressRepository.SuggestionDto> lastSuggestions;
    @Nullable
    private AddressRepository.SuggestionDto selectedSuggestion;

    private boolean suppressAddressTextWatcher = false;
    private boolean addressLoading = false;
    private long addressRequestSeq = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animal_form);

        animalRepo = new AnimalRepository();
        addressRepo = AddressRepository.create(this);

        bindViews();
        lockAgeFields();
        setupDropdowns();
        setupInteractions();

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) {
            mode = MODE_CREATE;
        }

        if (MODE_EDIT.equals(mode)) {
            animalId = getIntent().getLongExtra(EXTRA_ANIMAL_ID, -1);
            setTitle(getString(R.string.animal_form_edit_title));
            if (animalId > 0) {
                loadAnimal(animalId);
            } else {
                Toast.makeText(this, R.string.animal_form_load_error, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } else {
            setTitle(getString(R.string.animal_form_create_title));
        }

        btnSave.setOnClickListener(v -> onSaveClicked());
    }

    private void bindViews() {
        tilName = findViewById(R.id.tilName);
        tilSpecies = findViewById(R.id.tilSpecies);
        tilBreed = findViewById(R.id.tilBreed);
        tilSex = findViewById(R.id.tilSex);
        tilDob = findViewById(R.id.tilDob);
        tilAgeYears = findViewById(R.id.tilAgeYears);
        tilAgeMonths = findViewById(R.id.tilAgeMonths);

        tilWeight = findViewById(R.id.tilWeight);
        tilHeight = findViewById(R.id.tilHeight);
        tilColor = findViewById(R.id.tilColor);
        tilColorOther = findViewById(R.id.tilColorOther);
        tilPattern = findViewById(R.id.tilPattern);

        tilChipNumber = findViewById(R.id.tilChipNumber);

        tilTemperament = findViewById(R.id.tilTemperament);
        tilDescription = findViewById(R.id.tilDescription);

        tilAddress = findViewById(R.id.tilAddress);

        etName = findViewById(R.id.etName);
        acSpecies = findViewById(R.id.acSpecies);
        acBreed = findViewById(R.id.acBreed);
        acSex = findViewById(R.id.acSex);
        etDob = findViewById(R.id.etDob);
        etAgeYears = findViewById(R.id.etAgeYears);
        etAgeMonths = findViewById(R.id.etAgeMonths);

        etWeight = findViewById(R.id.etWeight);
        etHeight = findViewById(R.id.etHeight);
        acColor = findViewById(R.id.acColor);
        etColorOther = findViewById(R.id.etColorOther);
        acPattern = findViewById(R.id.acPattern);

        cbNeutered = findViewById(R.id.cbNeutered);
        cbVaccinated = findViewById(R.id.cbVaccinated);
        cbChipped = findViewById(R.id.cbChipped);
        etChipNumber = findViewById(R.id.etChipNumber);

        etTemperament = findViewById(R.id.etTemperament);
        etDescription = findViewById(R.id.etDescription);

        acAddress = findViewById(R.id.acAddress);

        btnSave = findViewById(R.id.btnSave);
        backbutton = findViewById(R.id.myAnimalsFormBackButton);


    }
    private void lockAgeFields() {
        etAgeYears.setFocusable(false);
        etAgeYears.setClickable(false);
        etAgeYears.setLongClickable(false);
        etAgeYears.setCursorVisible(false);

        etAgeMonths.setFocusable(false);
        etAgeMonths.setClickable(false);
        etAgeMonths.setLongClickable(false);
        etAgeMonths.setCursorVisible(false);
    }

    private void setupDropdowns() {
        setDropdownAdapter(acSpecies, R.array.animal_species);
        setDropdownAdapter(acSex, R.array.animal_sex);
        setDropdownAdapter(acColor, R.array.animal_colors);
        setDropdownAdapter(acPattern, R.array.animal_patterns);

        acBreed.setEnabled(false);
        tilBreed.setEnabled(false);
    }

    private void setupInteractions() {
        View.OnClickListener dobClick = v -> openDobPicker();
        etDob.setOnClickListener(dobClick);
        tilDob.setEndIconOnClickListener(dobClick);
        backbutton.setOnClickListener(v -> finish());
        acSpecies.setOnItemClickListener((parent, view, position, id) -> {
            String species = safeText(acSpecies);
            updateBreedAdapterForSpecies(species);
        });

        acColor.setOnItemClickListener((parent, view, position, id) -> handleOtherToggleForColor());

        cbChipped.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etChipNumber.setEnabled(isChecked);
            if (!isChecked) {
                etChipNumber.setText("");
                tilChipNumber.setError(null);
            }
        });

        // Address autocomplete: text watcher с debounce
        acAddress.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressAddressTextWatcher) {
                    return;
                }

                String q = (s != null) ? s.toString() : "";
                selectedSuggestion = null;
                tilAddress.setError(null);

                scheduleAddressAutocomplete(q);
            }
        });

        // Выбор подсказки: маппим позицию на SuggestionDto
        acAddress.setOnItemClickListener((parent, view, position, id) -> {
            List<AddressRepository.SuggestionDto> suggestions = lastSuggestions;
            if (suggestions == null || position < 0 || position >= suggestions.size()) {
                selectedSuggestion = null;
                return;
            }
            selectedSuggestion = suggestions.get(position);
        });
    }

    private void scheduleAddressAutocomplete(@NonNull String query) {
        if (pendingAddressRunnable != null) {
            handler.removeCallbacks(pendingAddressRunnable);
        }

        final String trimmed = query.trim();
        if (trimmed.length() < ADDRESS_MIN_CHARS) {
            lastSuggestions = null;
            acAddress.dismissDropDown();
            return;
        }

        pendingAddressRunnable = () -> requestAddressAutocomplete(trimmed);
        handler.postDelayed(pendingAddressRunnable, ADDRESS_DEBOUNCE_MS);
    }

    private void requestAddressAutocomplete(@NonNull String query) {
        if (isBusy) {
            return;
        }

        addressLoading = true;
        long seq = ++addressRequestSeq;

        addressRepo.autocomplete(
                query,
                ADDRESS_LIMIT,
                "ru",
                null,
                new AddressRepository.SuggestionsCallback() {
                    @Override
                    public void onSuccess(@NonNull List<AddressRepository.SuggestionDto> suggestions) {
                        if (isFinishing()) {
                            return;
                        }
                        if (seq != addressRequestSeq) {
                            return;
                        }

                        addressLoading = false;
                        lastSuggestions = suggestions;

                        String[] items = mapSuggestionsToStrings(suggestions);
                        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                                AnimalFormActivity.this,
                                android.R.layout.simple_list_item_1,
                                items
                        );

                        suppressAddressTextWatcher = true;
                        try {
                            acAddress.setAdapter(adapter);
                            if (!suggestions.isEmpty()) {
                                acAddress.showDropDown();
                            } else {
                                acAddress.dismissDropDown();
                            }
                        } finally {
                            suppressAddressTextWatcher = false;
                        }
                    }

                    @Override
                    public void onError(@NonNull AddressRepository.AddressError error) {
                        if (isFinishing()) {
                            return;
                        }
                        if (seq != addressRequestSeq) {
                            return;
                        }

                        addressLoading = false;
                        lastSuggestions = null;

                        // Ошибку сети не считаем "ошибкой валидации", просто не мешаем вводить
                        Log.w(TAG, "autocomplete error: " + error.getMessage() + " http=" + error.getHttpCode());
                    }
                }
        );
    }

    @NonNull
    private String[] mapSuggestionsToStrings(@NonNull List<AddressRepository.SuggestionDto> suggestions) {
        String[] arr = new String[suggestions.size()];
        for (int i = 0; i < suggestions.size(); i++) {
            AddressRepository.SuggestionDto s = suggestions.get(i);
            String formatted = (s != null) ? s.getFormatted() : null;
            arr[i] = (formatted != null && !formatted.trim().isEmpty()) ? formatted.trim() : "Адрес";
        }
        return arr;
    }

    private void openDobPicker() {
        Calendar cal = Calendar.getInstance();
        String current = safeText(etDob);
        if (current != null && !current.trim().isEmpty()) {
            try {
                cal.setTime(Objects.requireNonNull(sdf.parse(current.trim())));
            } catch (Exception ignored) {
            }
        }

        Calendar now = Calendar.getInstance();

        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(Calendar.YEAR, year);
                    picked.set(Calendar.MONTH, month);
                    picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    normalizeDayStart(picked);

                    if (picked.after(normalizeDayStartCopy(now))) {
                        tilDob.setError(getString(R.string.animal_form_error_date_future));
                        Toast.makeText(this, R.string.animal_form_error_date_future, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String value = sdf.format(picked.getTime());
                    etDob.setText(value);
                    tilDob.setError(null);

                    applyAgeFromDob(picked);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        // Запрещает выбирать дату позже сегодняшней
        dlg.getDatePicker().setMaxDate(now.getTimeInMillis());

        dlg.show();
    }
    private void applyAgeFromDob(@NonNull Calendar dob) {
        Calendar now = Calendar.getInstance();
        normalizeDayStart(dob);
        normalizeDayStart(now);

        if (dob.after(now)) {
            etAgeYears.setText("");
            etAgeMonths.setText("");
            return;
        }

        int years = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        int months = now.get(Calendar.MONTH) - dob.get(Calendar.MONTH);

        int nowDay = now.get(Calendar.DAY_OF_MONTH);
        int dobDay = dob.get(Calendar.DAY_OF_MONTH);

        // Если текущий день месяца меньше дня рождения, месяц ещё "не наступил"
        if (nowDay < dobDay) {
            months -= 1;
        }

        if (months < 0) {
            years -= 1;
            months += 12;
        }

        if (years < 0) years = 0;
        if (months < 0) months = 0;

        etAgeYears.setText(String.valueOf(years));
        etAgeMonths.setText(String.valueOf(months));
    }

    private void normalizeDayStart(@NonNull Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    @NonNull
    private Calendar normalizeDayStartCopy(@NonNull Calendar source) {
        Calendar c = (Calendar) source.clone();
        normalizeDayStart(c);
        return c;
    }
    private void handleOtherToggleForColor() {
        String color = safeText(acColor);
        boolean isOther = color != null && color.trim().equalsIgnoreCase("другое…");
        tilColorOther.setVisibility(isOther ? View.VISIBLE : View.GONE);
        if (!isOther) {
            etColorOther.setText("");
        }
    }

    private void updateBreedAdapterForSpecies(@Nullable String species) {
        int arr = 0;

        if (species != null) {
            String s = species.trim().toLowerCase(Locale.ROOT);
            if (s.equals("кошка")) arr = R.array.breeds_cat;
            else if (s.equals("собака")) arr = R.array.breeds_dog;
            else if (s.equals("хорёк") || s.equals("хорек")) arr = R.array.breeds_ferret;
            else if (s.equals("медведь") || s.equals("медвед")) arr = R.array.breeds_bear;
        }

        if (arr == 0) {
            acBreed.setText("", false);
            acBreed.setEnabled(false);
            tilBreed.setEnabled(false);
            return;
        }

        setDropdownAdapter(acBreed, arr);
        acBreed.setText("", false);
        acBreed.setEnabled(true);
        tilBreed.setEnabled(true);
    }

    private void setDropdownAdapter(@NonNull MaterialAutoCompleteTextView view, @ArrayRes int arrayRes) {
        String[] items = getResources().getStringArray(arrayRes);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                items
        );
        view.setAdapter(adapter);
    }

    private void loadAnimal(long id) {
        setBusy(true);
        animalRepo.getAnimalById(id, result -> {
            if (isFinishing()) {
                return;
            }

            if (!result.isSuccess || result.data == null) {
                Log.w(TAG, "getAnimalById failed http=" + result.httpCode + " body=" + result.errorBody);
                setBusy(false);
                Toast.makeText(this, R.string.animal_form_load_error, Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            AnimalRepository.AnimalDto dto = result.data;

            etName.setText(nullSafe(dto.name));

            if (dto.species != null) {
                acSpecies.setText(dto.species, false);
                updateBreedAdapterForSpecies(dto.species);
            }
            acBreed.setText(nullSafe(dto.breed), false);
            acSex.setText(nullSafe(dto.sex), false);

            etDob.setText(nullSafe(dto.dateOfBirth));
            if (dto.dateOfBirth != null && !dto.dateOfBirth.trim().isEmpty()) {
                Calendar c = parseDobToCalendarOrNull(dto.dateOfBirth);
                if (c != null) {
                    applyAgeFromDob(c);
                }
            }
            etAgeYears.setText(dto.approxAgeYears != null ? String.valueOf(dto.approxAgeYears) : "");
            etAgeMonths.setText(dto.approxAgeMonths != null ? String.valueOf(dto.approxAgeMonths) : "");

            etWeight.setText(dto.weightKg != null ? String.valueOf(dto.weightKg) : "");
            etHeight.setText(dto.heightCm != null ? String.valueOf(dto.heightCm) : "");

            if (dto.color != null) {
                acColor.setText(dto.color, false);
            }
            etColorOther.setText("");
            tilColorOther.setVisibility(View.GONE);

            acPattern.setText(nullSafe(dto.pattern), false);

            cbNeutered.setChecked(dto.isNeutered != null && dto.isNeutered);
            cbVaccinated.setChecked(dto.isVaccinated != null && dto.isVaccinated);
            cbChipped.setChecked(dto.isChipped != null && dto.isChipped);

            etChipNumber.setEnabled(cbChipped.isChecked());
            etChipNumber.setText(nullSafe(dto.chipNumber));

            etTemperament.setText(nullSafe(dto.temperamentNote));
            etDescription.setText(nullSafe(dto.description));

            // Адрес: сервер хранит только city, поэтому при редактировании просто показываем city.
            suppressAddressTextWatcher = true;
            try {
                acAddress.setText(nullSafe(dto.city), false);
            } finally {
                suppressAddressTextWatcher = false;
            }
            selectedSuggestion = null;
            lastSuggestions = null;

            setBusy(false);
        });
    }

    private void onSaveClicked() {
        if (isBusy) {
            return;
        }

        clearAllErrors();

        String name = normalizeNullable(safeText(etName));
        if (name == null) {
            tilName.setError(getString(R.string.animal_form_error_name_required));
            Toast.makeText(this, R.string.animal_form_error_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String species = normalizeNullable(safeText(acSpecies));
        if (species == null) {
            tilSpecies.setError(getString(R.string.animal_form_error_species_required));
            Toast.makeText(this, R.string.animal_form_error_species_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String sex = normalizeNullable(safeText(acSex));
        if (sex == null) {
            tilSex.setError(getString(R.string.animal_form_error_sex_required));
            Toast.makeText(this, R.string.animal_form_error_sex_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String dob = normalizeNullable(safeText(etDob));
        if (dob != null && !isValidDate(dob)) {
            Calendar dobCal = parseDobToCalendarOrNull(dob);
            if (dobCal == null) {
                tilDob.setError(getString(R.string.animal_form_error_date_invalid));
                Toast.makeText(this, R.string.animal_form_error_date_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
            Calendar now = Calendar.getInstance();
            normalizeDayStart(dobCal);
            normalizeDayStart(now);
            if (dobCal.after(now)) {
                tilDob.setError(getString(R.string.animal_form_error_date_future));
                Toast.makeText(this, R.string.animal_form_error_date_future, Toast.LENGTH_SHORT).show();
                return;
            }

            // Гарантированно пересчитывает возраст на момент сохранения
            applyAgeFromDob(dobCal);
        }

        Integer ageYears = parseIntOrNull(etAgeYears, tilAgeYears);
        Integer ageMonths = parseIntOrNull(etAgeMonths, tilAgeMonths);
        if (ageYears != null && (ageYears < 0 || ageYears > MAX_AGE_YEARS)) {
            tilAgeYears.setError(getString(R.string.animal_form_error_number_invalid));
            return;
        }
        if (ageMonths != null && (ageMonths < 0 || ageMonths > MAX_AGE_MONTHS)) {
            tilAgeMonths.setError(getString(R.string.animal_form_error_number_invalid));
            return;
        }

        Double weightKg = parseDoubleOrNull(etWeight, tilWeight);
        Double heightCm = parseDoubleOrNull(etHeight, tilHeight);

        String breed = normalizeNullable(safeText(acBreed));

        String color = normalizeNullable(safeText(acColor));
        if (color != null && color.equalsIgnoreCase("другое…")) {
            color = normalizeNullable(safeText(etColorOther));
        }

        String pattern = normalizeNullable(safeText(acPattern));

        boolean isNeutered = cbNeutered.isChecked();
        boolean isVaccinated = cbVaccinated.isChecked();
        boolean isChipped = cbChipped.isChecked();

        String chipNumber = normalizeNullable(safeText(etChipNumber));
        if (isChipped && chipNumber == null) {
            tilChipNumber.setError(getString(R.string.animal_form_error_chip_number_required));
            Toast.makeText(this, R.string.animal_form_error_chip_number_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String temperament = normalizeNullable(safeText(etTemperament));
        String description = normalizeNullable(safeText(etDescription));

        // Город/адрес: требуется выбор из подсказок
        AddressRepository.SuggestionDto picked = selectedSuggestion;
        if (picked == null) {
            tilAddress.setError(getString(R.string.animal_form_error_address_required));
            Toast.makeText(this, R.string.animal_form_error_address_required, Toast.LENGTH_SHORT).show();
            return;
        }

        String city = normalizeNullable(picked.getCity());
        if (city == null) {
            city = extractCityFallbackFromFormatted(picked.getFormatted());
        }

        Double geoLat = picked.getLat();
        Double geoLng = picked.getLon();

        if (MODE_EDIT.equals(mode)) {
            AnimalRepository.AnimalUpdateRequestDto body = new AnimalRepository.AnimalUpdateRequestDto();
            body.name = name;
            body.species = species;
            body.breed = breed;
            body.sex = sex;
            body.dateOfBirth = dob;
            body.approxAgeYears = ageYears;
            body.approxAgeMonths = ageMonths;
            body.weightKg = weightKg;
            body.heightCm = heightCm;
            body.color = color;
            body.pattern = pattern;
            body.isNeutered = isNeutered;
            body.isVaccinated = isVaccinated;
            body.isChipped = isChipped;
            body.chipNumber = chipNumber;
            body.temperamentNote = temperament;
            body.description = description;

            // Важно: статус UI не задаёт, но можем оставить server default.
            // Если бэк требует status, можно принудительно: body.status = "active";
            body.city = city;
            body.geoLat = geoLat;
            body.geoLng = geoLng;

            sendUpdate(animalId, body);
        } else {
            AnimalRepository.AnimalCreateRequestDto body = new AnimalRepository.AnimalCreateRequestDto();
            body.name = name;
            body.species = species;
            body.breed = breed;
            body.sex = sex;
            body.dateOfBirth = dob;
            body.approxAgeYears = ageYears;
            body.approxAgeMonths = ageMonths;
            body.weightKg = weightKg;
            body.heightCm = heightCm;
            body.color = color;
            body.pattern = pattern;
            body.isNeutered = isNeutered;
            body.isVaccinated = isVaccinated;
            body.isChipped = isChipped;
            body.chipNumber = chipNumber;
            body.temperamentNote = temperament;
            body.description = description;

            // Если бэк требует status, можно: body.status = "active";
            body.city = city;
            body.geoLat = geoLat;
            body.geoLng = geoLng;

            sendCreate(body);
        }
    }
    @Nullable
    private Calendar parseDobToCalendarOrNull(@NonNull String dob) {
        try {
            sdf.setLenient(false);
            Calendar c = Calendar.getInstance();
            c.setTime(Objects.requireNonNull(sdf.parse(dob.trim())));
            return c;
        } catch (Exception e) {
            return null;
        }
    }
    @Nullable
    private String extractCityFallbackFromFormatted(@Nullable String formatted) {
        if (formatted == null) {
            return null;
        }
        String t = formatted.trim();
        if (t.isEmpty()) {
            return null;
        }
        // Простейший фоллбек: берём первый сегмент до запятой.
        int idx = t.indexOf(',');
        if (idx > 0) {
            String first = t.substring(0, idx).trim();
            return first.isEmpty() ? t : first;
        }
        return t;
    }

    private void sendCreate(@NonNull AnimalRepository.AnimalCreateRequestDto body) {
        setBusy(true);
        animalRepo.createAnimal(body, result -> {
            if (isFinishing()) {
                return;
            }
            setBusy(false);

            if (!result.isSuccess) {
                Log.w(TAG, "createAnimal failed http=" + result.httpCode + " body=" + result.errorBody);
                Toast.makeText(this, R.string.animal_form_save_error, Toast.LENGTH_LONG).show();
                return;
            }

            setResult(RESULT_OK);
            finish();
        });
    }

    private void sendUpdate(long id, @NonNull AnimalRepository.AnimalUpdateRequestDto body) {
        setBusy(true);
        animalRepo.updateAnimal(id, body, result -> {
            if (isFinishing()) {
                return;
            }
            setBusy(false);

            if (!result.isSuccess) {
                Log.w(TAG, "updateAnimal failed http=" + result.httpCode + " body=" + result.errorBody);
                Toast.makeText(this, R.string.animal_form_save_error, Toast.LENGTH_LONG).show();
                return;
            }

            setResult(RESULT_OK);
            finish();
        });
    }

    private void setBusy(boolean busy) {
        isBusy = busy;

        btnSave.setEnabled(!busy);
        btnSave.setAlpha(busy ? 0.65f : 1f);

        tilName.setEnabled(!busy);
        tilSpecies.setEnabled(!busy);
        tilBreed.setEnabled(!busy);
        tilSex.setEnabled(!busy);
        tilDob.setEnabled(!busy);
        tilAgeYears.setEnabled(!busy);
        tilAgeMonths.setEnabled(!busy);
        tilWeight.setEnabled(!busy);
        tilHeight.setEnabled(!busy);
        tilColor.setEnabled(!busy);
        tilColorOther.setEnabled(!busy);
        tilPattern.setEnabled(!busy);
        cbNeutered.setEnabled(!busy);
        cbVaccinated.setEnabled(!busy);
        cbChipped.setEnabled(!busy);
        tilChipNumber.setEnabled(!busy);
        tilTemperament.setEnabled(!busy);
        tilDescription.setEnabled(!busy);

        tilAddress.setEnabled(!busy);
        acAddress.setEnabled(!busy && !addressLoading);
    }

    private void clearAllErrors() {
        tilName.setError(null);
        tilSpecies.setError(null);
        tilBreed.setError(null);
        tilSex.setError(null);
        tilDob.setError(null);
        tilAgeYears.setError(null);
        tilAgeMonths.setError(null);
        tilWeight.setError(null);
        tilHeight.setError(null);
        tilColor.setError(null);
        tilColorOther.setError(null);
        tilPattern.setError(null);
        tilChipNumber.setError(null);
        tilAddress.setError(null);
    }

    private boolean isValidDate(@NonNull String date) {
        try {
            sdf.setLenient(false);
            sdf.parse(date.trim());
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    @Nullable
    private Integer parseIntOrNull(@NonNull TextInputEditText et, @NonNull TextInputLayout til) {
        String s = safeText(et);
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(t);
        } catch (Exception e) {
            til.setError(getString(R.string.animal_form_error_number_invalid));
            return null;
        }
    }

    @Nullable
    private Double parseDoubleOrNull(@NonNull TextInputEditText et, @NonNull TextInputLayout til) {
        String s = safeText(et);
        if (s == null) {
            return null;
        }
        String t = s.trim().replace(',', '.');
        if (t.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(t);
        } catch (Exception e) {
            til.setError(getString(R.string.animal_form_error_number_invalid));
            return null;
        }
    }

    @Nullable
    private static String normalizeNullable(@Nullable String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Nullable
    private static String safeText(@Nullable TextInputEditText et) {
        if (et == null || et.getText() == null) return null;
        return et.getText().toString();
    }

    @Nullable
    private static String safeText(@Nullable MaterialAutoCompleteTextView et) {
        if (et == null || et.getText() == null) return null;
        return et.getText().toString();
    }

    @NonNull
    private static String nullSafe(@Nullable String s) {
        return s == null ? "" : s;
    }

    // Утилита: простой TextWatcher без лишнего шума
    private abstract static class SimpleTextWatcher implements android.text.TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void afterTextChanged(android.text.Editable s) { }
    }
}
