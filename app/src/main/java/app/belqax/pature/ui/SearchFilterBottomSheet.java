package app.belqax.pature.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.RangeSlider;

import java.util.List;

import app.belqax.pature.R;
import app.belqax.pature.utils.SearchFilters;

public class SearchFilterBottomSheet extends BottomSheetDialogFragment {

    public interface OnFiltersAppliedListener {
        void onFiltersApplied(@NonNull SearchFilters filters);
    }

    private SearchFilters currentFilters = new SearchFilters();
    private OnFiltersAppliedListener listener;

    private ChipGroup speciesChipGroup;
    private ChipGroup genderChipGroup;
    private RangeSlider ageRangeSlider;
    private TextView ageRangeLabel;
    private MaterialCheckBox withPhotoCheckBox;
    private MaterialCheckBox favoritesOnlyCheckBox;
    private MaterialButton resetButton;
    private MaterialButton applyButton;

    public SearchFilterBottomSheet() {
    }

    public static SearchFilterBottomSheet newInstance(@NonNull SearchFilters filters) {
        SearchFilterBottomSheet sheet = new SearchFilterBottomSheet();
        sheet.currentFilters = filters.copy();
        return sheet;
    }

    public void setOnFiltersAppliedListener(@NonNull OnFiltersAppliedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Твой layout с ScrollView
        return inflater.inflate(R.layout.bottom_sheet_search_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ищет элементы по id из твоего XML
        speciesChipGroup = view.findViewById(R.id.speciesChipGroup);
        genderChipGroup = view.findViewById(R.id.genderChipGroup);
        ageRangeSlider = view.findViewById(R.id.ageRangeSlider);
        ageRangeLabel = view.findViewById(R.id.ageRangeLabel);
        withPhotoCheckBox = view.findViewById(R.id.withPhotoCheckBox);
        favoritesOnlyCheckBox = view.findViewById(R.id.favoritesOnlyCheckBox);
        resetButton = view.findViewById(R.id.resetFiltersButton);
        applyButton = view.findViewById(R.id.applyFiltersButton);

        initFromFilters(currentFilters);
        setupListeners();
    }

    private void initFromFilters(@NonNull SearchFilters filters) {
        selectSpeciesChip(filters.species);
        selectGenderChip(filters.gender);

        ageRangeSlider.setValues(
                (float) filters.minAgeYears,
                (float) filters.maxAgeYears
        );
        updateAgeLabel(filters.minAgeYears, filters.maxAgeYears);

        withPhotoCheckBox.setChecked(filters.withPhotoOnly);
        favoritesOnlyCheckBox.setChecked(filters.favoritesOnly);
    }

    private void setupListeners() {
        ageRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            int min = Math.round(values.get(0));
            int max = Math.round(values.get(1));
            currentFilters.minAgeYears = min;
            currentFilters.maxAgeYears = max;
            updateAgeLabel(min, max);
        });

        withPhotoCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> currentFilters.withPhotoOnly = isChecked
        );

        favoritesOnlyCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> currentFilters.favoritesOnly = isChecked
        );

        speciesChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipSpeciesDog) {
                currentFilters.species = SearchFilters.SPECIES_DOG;
            } else if (checkedId == R.id.chipSpeciesCat) {
                currentFilters.species = SearchFilters.SPECIES_CAT;
            } else {
                currentFilters.species = SearchFilters.SPECIES_ANY;
            }
        });

        genderChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipGenderMale) {
                currentFilters.gender = SearchFilters.GENDER_MALE;
            } else if (checkedId == R.id.chipGenderFemale) {
                currentFilters.gender = SearchFilters.GENDER_FEMALE;
            } else {
                currentFilters.gender = SearchFilters.GENDER_ANY;
            }
        });

        resetButton.setOnClickListener(v -> {
            currentFilters = new SearchFilters();
            initFromFilters(currentFilters);
        });

        applyButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFiltersApplied(currentFilters);
            }
            dismiss();
        });
    }

    private void updateAgeLabel(int min, int max) {
        if (ageRangeLabel != null) {
            String text = min + " – " + max;
            ageRangeLabel.setText(text);
        }
    }

    private void selectSpeciesChip(@NonNull String species) {
        int chipId;
        switch (species) {
            case SearchFilters.SPECIES_DOG:
                chipId = R.id.chipSpeciesDog;
                break;
            case SearchFilters.SPECIES_CAT:
                chipId = R.id.chipSpeciesCat;
                break;
            default:
                chipId = R.id.chipSpeciesAny;
        }
        speciesChipGroup.check(chipId);
    }

    private void selectGenderChip(@NonNull String gender) {
        int chipId;
        switch (gender) {
            case SearchFilters.GENDER_MALE:
                chipId = R.id.chipGenderMale;
                break;
            case SearchFilters.GENDER_FEMALE:
                chipId = R.id.chipGenderFemale;
                break;
            default:
                chipId = R.id.chipGenderAny;
        }
        genderChipGroup.check(chipId);
    }
}
