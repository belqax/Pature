package app.belqax.pature.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import app.belqax.pature.R;
import app.belqax.pature.data.repository.ProfileRepository;
import app.belqax.pature.ui.ChangePasswordBottomSheet;

public class ProfileSettingsActivity extends AppCompatActivity {

    private static final String TAG = "ProfileSettingsActivity";

    private ProfileRepository repository;

    private ImageButton backButton;

    private SwitchMaterial biometricSwitch;

    private LinearLayout hideProfileRow;
    private LinearLayout hidePhotosRow;

    private TextView hideProfileValue;
    private TextView hidePhotosValue;
    private LinearLayout profileSettingsChangePasswordRow;

    private boolean isUpdatingUiProgrammatically = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        repository = ProfileRepository.create(getApplicationContext());

        bindViews();
        bindActions();

        // 1) Сначала рисует из кеша (быстро)
        ProfileRepository.MeResponse cached = repository.getCachedProfile();
        if (cached != null) {
            renderFromMe(cached);
        }

        // 2) Затем обновляет с сервера (актуально)
        repository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull ProfileRepository.MeResponse meResponse) {
                renderFromMe(meResponse);
            }

            @Override
            public void onError(@NonNull ProfileRepository.ProfileError error) {
                Log.w(TAG, "loadProfile error: " + error.getMessage());
                if (cached == null) {
                    toast(error.getMessage());
                }
            }
        });
    }

    private void bindViews() {
        backButton = findViewById(R.id.profileSettingsBackButton);

        biometricSwitch = findViewById(R.id.profileSettingsBiometricSwitch);

        hideProfileRow = findViewById(R.id.profileSettingsHideProfileRow);
        hidePhotosRow = findViewById(R.id.profileSettingsHidePhotosRow);

        hideProfileValue = findViewById(R.id.profileSettingsHideProfileValue);
        hidePhotosValue = findViewById(R.id.profileSettingsHidePhotosValue);
        profileSettingsChangePasswordRow = findViewById(R.id.profileSettingsChangePasswordRow);
    }

    private void bindActions() {
        backButton.setOnClickListener(v -> finish());

        biometricSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingUiProgrammatically) {
                return;
            }
            patchBiometric(isChecked);
        });

        profileSettingsChangePasswordRow.setOnClickListener(v -> {
            ChangePasswordBottomSheet.newInstance()
                    .show(getSupportFragmentManager(), "ChangePasswordBottomSheet");
        });
        hideProfileRow.setOnClickListener(v -> showProfileVisibilityDialog());
        hidePhotosRow.setOnClickListener(v -> showPhotosVisibilityDialog());
    }

    private void renderFromMe(@NonNull ProfileRepository.MeResponse me) {
        ProfileRepository.SettingsDto settings = me.getSettings();
        ProfileRepository.PrivacyDto privacy = me.getPrivacy();

        if (settings != null) {
            setSwitchSafely(biometricSwitch, settings.isBiometricLoginEnabled());
        }

        if (privacy != null) {
            hideProfileValue.setText(getVisibilityLabelForProfile(privacy.getProfileVisibility()));
            hidePhotosValue.setText(getVisibilityLabelForPhotos(privacy.getPhotosVisibility()));
        }
    }

    private void setSwitchSafely(@NonNull SwitchMaterial sw, boolean value) {
        isUpdatingUiProgrammatically = true;
        try {
            sw.setChecked(value);
        } finally {
            isUpdatingUiProgrammatically = false;
        }
    }

    private void patchBiometric(boolean enabled) {
        setInteractive(false);

        ProfileRepository.UpdateSettingsRequest req =
                new ProfileRepository.UpdateSettingsRequest(
                        null,               // language_code
                        null,               // timezone
                        enabled,            // biometric_login_enabled
                        null,               // push_enabled
                        null,               // push_new_messages
                        null,               // push_events
                        null                // push_news
                );

        repository.updateSettings(req, new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull ProfileRepository.MeResponse meResponse) {
                renderFromMe(meResponse);
                setInteractive(true);
            }

            @Override
            public void onError(@NonNull ProfileRepository.ProfileError error) {
                Log.w(TAG, "updateSettings error: " + error.getMessage());

                // Откат UI, чтобы состояние соответствовало серверу/кешу
                ProfileRepository.MeResponse cached = repository.getCachedProfile();
                if (cached != null && cached.getSettings() != null) {
                    setSwitchSafely(biometricSwitch, cached.getSettings().isBiometricLoginEnabled());
                } else {
                    setSwitchSafely(biometricSwitch, !enabled);
                }

                setInteractive(true);
                toast(error.getMessage());
            }
        });
    }
    private interface VisibilityChosenCallback {
        void onChosen(@NonNull String value);
    }
    private void showVisibilityPickerDialog(@NonNull String title,
                                            @NonNull String currentValue,
                                            @NonNull VisibilityChosenCallback callback) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_visibility_picker, null);

        View optionEveryone = dialogView.findViewById(R.id.visibilityOptionEveryone);
        View optionMatches = dialogView.findViewById(R.id.visibilityOptionMatches);
        View optionNone = dialogView.findViewById(R.id.visibilityOptionNone);

        com.google.android.material.radiobutton.MaterialRadioButton radioEveryone =
                dialogView.findViewById(R.id.visibilityEveryoneRadio);
        com.google.android.material.radiobutton.MaterialRadioButton radioMatches =
                dialogView.findViewById(R.id.visibilityMatchesRadio);
        com.google.android.material.radiobutton.MaterialRadioButton radioNone =
                dialogView.findViewById(R.id.visibilityNoneRadio);

        setRadioState(currentValue, radioEveryone, radioMatches, radioNone);

        AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(dialogView)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        optionEveryone.setOnClickListener(v -> {
            setRadioState("everyone", radioEveryone, radioMatches, radioNone);
            dialog.dismiss();
            callback.onChosen("everyone");
        });

        optionMatches.setOnClickListener(v -> {
            setRadioState("matches", radioEveryone, radioMatches, radioNone);
            dialog.dismiss();
            callback.onChosen("matches");
        });

        optionNone.setOnClickListener(v -> {
            setRadioState("nobody", radioEveryone, radioMatches, radioNone);
            dialog.dismiss();
            callback.onChosen("nobody");
        });

        // Чтобы тап по радиокнопке тоже работал, а не только по карточке
        radioEveryone.setOnClickListener(v -> optionEveryone.performClick());
        radioMatches.setOnClickListener(v -> optionMatches.performClick());
        radioNone.setOnClickListener(v -> optionNone.performClick());

        dialog.show();
    }
    private void setRadioState(@Nullable String value,
                               @NonNull com.google.android.material.radiobutton.MaterialRadioButton radioEveryone,
                               @NonNull com.google.android.material.radiobutton.MaterialRadioButton radioMatches,
                               @NonNull com.google.android.material.radiobutton.MaterialRadioButton radioNone) {
        String v = (value == null) ? "everyone" : value;

        radioEveryone.setChecked("everyone".equalsIgnoreCase(v));
        radioMatches.setChecked("matches".equalsIgnoreCase(v));
        radioNone.setChecked("nobody".equalsIgnoreCase(v));
    }

    private void showProfileVisibilityDialog() {
        String current = getCachedProfileVisibility();
        if (current == null || current.trim().isEmpty()) current = "everyone";

        showVisibilityPickerDialog(
                getString(R.string.profile_settings_hide_profile),
                current,
                selected -> patchPrivacy(selected, null)
        );
    }

    private void showPhotosVisibilityDialog() {
        String current = getCachedPhotosVisibility();
        if (current == null || current.trim().isEmpty()) current = "matches";

        showVisibilityPickerDialog(
                getString(R.string.profile_settings_hide_photos),
                current,
                selected -> patchPrivacy(null, selected)
        );
    }

    /**
     * PATCH /users/me/privacy
     * null означает "не менять".
     */
    private void patchPrivacy(@Nullable String profileVisibility,
                              @Nullable String photosVisibility) {
        setInteractive(false);

        ProfileRepository.UpdatePrivacyRequest req =
                new ProfileRepository.UpdatePrivacyRequest(
                        profileVisibility,
                        photosVisibility,
                        null,   // online_status_visibility
                        null,   // last_seen_precision
                        null,   // show_age
                        null    // show_distance
                );

        repository.updatePrivacy(req, new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull ProfileRepository.MeResponse meResponse) {
                renderFromMe(meResponse);
                setInteractive(true);
            }

            @Override
            public void onError(@NonNull ProfileRepository.ProfileError error) {
                Log.w(TAG, "updatePrivacy error: " + error.getMessage());

                // Перерисовка из кеша (откат)
                ProfileRepository.MeResponse cached = repository.getCachedProfile();
                if (cached != null) {
                    renderFromMe(cached);
                }

                setInteractive(true);
                toast(error.getMessage());
            }
        });
    }

    private void setInteractive(boolean enabled) {
        biometricSwitch.setEnabled(enabled);
        hideProfileRow.setEnabled(enabled);
        hidePhotosRow.setEnabled(enabled);
        backButton.setEnabled(enabled);
    }

    @NonNull
    private String getVisibilityLabelForProfile(@Nullable String value) {
        if ("matches".equalsIgnoreCase(value)) {
            return getString(R.string.profile_settings_privacy_visibility_matches_only);
        }
        if ("nobody".equalsIgnoreCase(value)) {
            return getString(R.string.profile_settings_privacy_visibility_nobody);
        }
        return getString(R.string.profile_settings_privacy_visibility_everyone);
    }

    @NonNull
    private String getVisibilityLabelForPhotos(@Nullable String value) {
        if ("matches".equalsIgnoreCase(value)) {
            return getString(R.string.profile_settings_privacy_visibility_matches_only);
        }
        if ("nobody".equalsIgnoreCase(value)) {
            return getString(R.string.profile_settings_privacy_visibility_nobody);
        }
        return getString(R.string.profile_settings_privacy_visibility_everyone);
    }

    @Nullable
    private String getCachedProfileVisibility() {
        ProfileRepository.MeResponse cached = repository.getCachedProfile();
        if (cached == null || cached.getPrivacy() == null) {
            return "everyone";
        }
        String v = cached.getPrivacy().getProfileVisibility();
        return (v == null || v.trim().isEmpty()) ? "everyone" : v;
    }

    @Nullable
    private String getCachedPhotosVisibility() {
        ProfileRepository.MeResponse cached = repository.getCachedProfile();
        if (cached == null || cached.getPrivacy() == null) {
            return "matches";
        }
        String v = cached.getPrivacy().getPhotosVisibility();
        return (v == null || v.trim().isEmpty()) ? "matches" : v;
    }

    private int indexOf(@NonNull String[] arr, @Nullable String value) {
        if (value == null) return 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    private void toast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
