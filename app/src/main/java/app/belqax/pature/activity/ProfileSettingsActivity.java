package app.belqax.pature.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

import app.belqax.pature.R;

public class ProfileSettingsActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSettingsActivity";

    private View backButton;

    private LinearLayout changePhoneRow;
    private LinearLayout changeEmailRow;
    private LinearLayout changePasswordRow;
    private LinearLayout verifyAccountRow;

    private SwitchMaterial biometricSwitch;

    private LinearLayout hideProfileRow;
    private TextView hideProfileValueView;

    private LinearLayout hidePhotosRow;
    private TextView hidePhotosValueView;

    private LinearLayout userAgreementRow;
    private LinearLayout privacyPolicyRow;

    private enum PrivacyLevel {
        EVERYONE,
        MATCHES_ONLY,
        NOBODY
    }

    private PrivacyLevel profileVisibilityLevel = PrivacyLevel.EVERYONE;
    private PrivacyLevel photosVisibilityLevel = PrivacyLevel.MATCHES_ONLY;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_profile_settings);

        bindViews();
        setupAccountSettings();
        setupPrivacySettings();
        setupLegalSettings();
    }

    private void bindViews() {
        backButton = findViewById(R.id.profileSettingsBackButton);

        changePhoneRow = findViewById(R.id.profileSettingsChangePhoneRow);
        changeEmailRow = findViewById(R.id.profileSettingsChangeEmailRow);
        changePasswordRow = findViewById(R.id.profileSettingsChangePasswordRow);
        verifyAccountRow = findViewById(R.id.profileSettingsVerifyAccountRow);

        biometricSwitch = findViewById(R.id.profileSettingsBiometricSwitch);

        hideProfileRow = findViewById(R.id.profileSettingsHideProfileRow);
        hideProfileValueView = findViewById(R.id.profileSettingsHideProfileValue);

        hidePhotosRow = findViewById(R.id.profileSettingsHidePhotosRow);
        hidePhotosValueView = findViewById(R.id.profileSettingsHidePhotosValue);

        userAgreementRow = findViewById(R.id.profileSettingsUserAgreementRow);
        privacyPolicyRow = findViewById(R.id.profileSettingsPrivacyPolicyRow);

        if (backButton != null) {
            backButton.setOnClickListener(v -> closeWithAnimation());
        }
    }

    private void setupAccountSettings() {
        if (changePhoneRow != null) {
            changePhoneRow.setOnClickListener(v ->
                    stubAction(R.string.profile_settings_change_phone_stub));
        }
        if (changeEmailRow != null) {
            changeEmailRow.setOnClickListener(v ->
                    stubAction(R.string.profile_settings_change_email_stub));
        }
        if (changePasswordRow != null) {
            changePasswordRow.setOnClickListener(v ->
                    stubAction(R.string.profile_settings_change_password_stub));
        }
        if (verifyAccountRow != null) {
            verifyAccountRow.setOnClickListener(v ->
                    stubAction(R.string.profile_settings_verify_account_stub));
        }

        if (biometricSwitch != null) {
            biometricSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                Log.d(TAG, "Biometrics toggled: " + isChecked);
                // TODO: подключить реальную настройку биометрии.
            });
        }
    }

    private void setupPrivacySettings() {
        updatePrivacyRowValue(hideProfileValueView, profileVisibilityLevel);
        updatePrivacyRowValue(hidePhotosValueView, photosVisibilityLevel);

        if (hideProfileRow != null) {
            hideProfileRow.setOnClickListener(v -> showPrivacySheetForProfile());
        }
        if (hidePhotosRow != null) {
            hidePhotosRow.setOnClickListener(v -> showPrivacySheetForPhotos());
        }
    }

    private void setupLegalSettings() {
        if (userAgreementRow != null) {
            userAgreementRow.setOnClickListener(v ->
                    stubAction(R.string.profile_settings_user_agreement_stub));
        }
        if (privacyPolicyRow != null) {
            privacyPolicyRow.setOnClickListener(v ->
                    stubAction(R.string.profile_settings_privacy_policy_stub));
        }
    }

    private void stubAction(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
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

    private void updatePrivacyRowValue(@Nullable TextView target,
                                       @NonNull PrivacyLevel level) {
        if (target == null) {
            return;
        }
        int textResId;
        switch (level) {
            case EVERYONE:
                textResId = R.string.profile_settings_privacy_visibility_everyone;
                break;
            case MATCHES_ONLY:
                textResId = R.string.profile_settings_privacy_visibility_matches_only;
                break;
            case NOBODY:
                textResId = R.string.profile_settings_privacy_visibility_nobody;
                break;
            default:
                textResId = R.string.profile_settings_privacy_visibility_everyone;
                break;
        }
        target.setText(textResId);
    }

    private void showPrivacySheetForProfile() {
        showPrivacyBottomSheet(
                R.string.profile_settings_privacy_profile_title,
                R.string.profile_settings_privacy_profile_description,
                profileVisibilityLevel,
                new PrivacySelectionListener() {
                    @Override
                    public void onSelected(@NonNull PrivacyLevel level) {
                        profileVisibilityLevel = level;
                        updatePrivacyRowValue(hideProfileValueView, level);
                        // TODO: сохранить уровень приватности профиля в хранилище.
                    }
                }
        );
    }

    private void showPrivacySheetForPhotos() {
        showPrivacyBottomSheet(
                R.string.profile_settings_privacy_photos_title,
                R.string.profile_settings_privacy_photos_description,
                photosVisibilityLevel,
                new PrivacySelectionListener() {
                    @Override
                    public void onSelected(@NonNull PrivacyLevel level) {
                        photosVisibilityLevel = level;
                        updatePrivacyRowValue(hidePhotosValueView, level);
                        // TODO: сохранить уровень приватности фото в хранилище.
                    }
                }
        );
    }

    private interface PrivacySelectionListener {
        void onSelected(@NonNull PrivacyLevel level);
    }

    private void showPrivacyBottomSheet(int titleResId,
                                        int descriptionResId,
                                        @NonNull PrivacyLevel currentLevel,
                                        @NonNull PrivacySelectionListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);

        View contentView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_privacy_visibility, null, false);

        dialog.setContentView(contentView);

        TextView titleView = contentView.findViewById(R.id.privacyTitle);
        TextView descriptionView = contentView.findViewById(R.id.privacyDescription);
        ChipGroup chipGroup = contentView.findViewById(R.id.privacyVisibilityChipGroup);
        MaterialButton cancelButton = contentView.findViewById(R.id.privacyCancelButton);
        MaterialButton applyButton = contentView.findViewById(R.id.privacyApplyButton);

        if (titleView != null) {
            titleView.setText(titleResId);
        }
        if (descriptionView != null) {
            descriptionView.setText(descriptionResId);
        }

        if (chipGroup != null) {
            int chipId = mapPrivacyLevelToChipId(currentLevel);
            if (chipId != 0) {
                chipGroup.check(chipId);
            }
        }

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> dialog.dismiss());
        }

        if (applyButton != null && chipGroup != null) {
            applyButton.setOnClickListener(v -> {
                int checkedId = chipGroup.getCheckedChipId();
                PrivacyLevel selectedLevel = mapChipIdToPrivacyLevel(checkedId);
                if (selectedLevel == null) {
                    selectedLevel = currentLevel;
                }
                listener.onSelected(selectedLevel);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private int mapPrivacyLevelToChipId(@NonNull PrivacyLevel level) {
        switch (level) {
            case EVERYONE:
                return R.id.chipVisibilityEveryone;
            case MATCHES_ONLY:
                return R.id.chipVisibilityMatchesOnly;
            case NOBODY:
                return R.id.chipVisibilityNobody;
            default:
                return R.id.chipVisibilityEveryone;
        }
    }

    @Nullable
    private PrivacyLevel mapChipIdToPrivacyLevel(int chipId) {
        if (chipId == R.id.chipVisibilityEveryone) {
            return PrivacyLevel.EVERYONE;
        } else if (chipId == R.id.chipVisibilityMatchesOnly) {
            return PrivacyLevel.MATCHES_ONLY;
        } else if (chipId == R.id.chipVisibilityNobody) {
            return PrivacyLevel.NOBODY;
        }
        Log.w(TAG, "Unknown chip id for privacy: " + chipId);
        return null;
    }
}
