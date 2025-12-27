package app.belqax.pature.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textview.MaterialTextView;

import app.belqax.pature.R;

public final class PrivacyVisibilityBottomSheet extends BottomSheetDialogFragment {

    public interface Listener {
        void onApply(@NonNull String visibility);
    }

    public static final String VIS_EVERYONE = "everyone";
    public static final String VIS_MATCHES = "matches";
    public static final String VIS_NOBODY = "nobody";

    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESC = "arg_desc";
    private static final String ARG_CURRENT = "arg_current";

    @Nullable
    private Listener listener;

    @NonNull
    public static PrivacyVisibilityBottomSheet newInstance(
            @NonNull String title,
            @NonNull String description,
            @NonNull String currentValue
    ) {
        PrivacyVisibilityBottomSheet bs = new PrivacyVisibilityBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, description);
        args.putString(ARG_CURRENT, currentValue);
        bs.setArguments(args);
        return bs;
    }

    public void setListener(@Nullable Listener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_privacy_visibility, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialTextView title = view.findViewById(R.id.privacyTitle);
        MaterialTextView desc = view.findViewById(R.id.privacyDescription);

        ChipGroup chips = view.findViewById(R.id.privacyVisibilityChipGroup);
        Chip chipEveryone = view.findViewById(R.id.chipVisibilityEveryone);
        Chip chipMatches = view.findViewById(R.id.chipVisibilityMatchesOnly);
        Chip chipNobody = view.findViewById(R.id.chipVisibilityNobody);

        MaterialButton btnCancel = view.findViewById(R.id.privacyCancelButton);
        MaterialButton btnApply = view.findViewById(R.id.privacyApplyButton);

        String t = argString(ARG_TITLE, getString(R.string.profile_settings_privacy_sheet_title_default));
        String d = argString(ARG_DESC, getString(R.string.profile_settings_privacy_sheet_description_default));
        String current = normalizeVisibility(argString(ARG_CURRENT, VIS_EVERYONE));

        title.setText(t);
        desc.setText(d);

        // Проставляет текущее значение
        if (VIS_MATCHES.equalsIgnoreCase(current)) {
            chipMatches.setChecked(true);
        } else if (VIS_NOBODY.equalsIgnoreCase(current)) {
            chipNobody.setChecked(true);
        } else {
            chipEveryone.setChecked(true);
        }

        btnCancel.setOnClickListener(v -> dismiss());

        btnApply.setOnClickListener(v -> {
            String selected = getSelectedVisibility(chips);
            Listener l = listener;
            dismiss();
            if (l != null) {
                l.onApply(selected);
            }
        });
    }

    @NonNull
    private String getSelectedVisibility(@NonNull ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id == R.id.chipVisibilityMatchesOnly) {
            return VIS_MATCHES;
        }
        if (id == R.id.chipVisibilityNobody) {
            return VIS_NOBODY;
        }
        return VIS_EVERYONE;
    }

    @NonNull
    private String argString(@NonNull String key, @NonNull String fallback) {
        Bundle args = getArguments();
        if (args == null) {
            return fallback;
        }
        String v = args.getString(key);
        if (v == null || v.trim().isEmpty()) {
            return fallback;
        }
        return v;
    }

    @NonNull
    private String normalizeVisibility(@Nullable String value) {
        if (value == null) {
            return VIS_EVERYONE;
        }
        String v = value.trim().toLowerCase();
        if (VIS_MATCHES.equals(v)) return VIS_MATCHES;
        if (VIS_NOBODY.equals(v)) return VIS_NOBODY;
        return VIS_EVERYONE;
    }
}
