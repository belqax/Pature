package app.belqax.pature.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import app.belqax.pature.R;
import app.belqax.pature.activity.EditProfileFieldActivity;
import app.belqax.pature.activity.ProfileSettingsActivity;
import app.belqax.pature.data.repository.ProfileRepository;
import app.belqax.pature.data.repository.ProfileRepository.MeResponse;
import app.belqax.pature.data.repository.ProfileRepository.ProfileDto;
import app.belqax.pature.data.repository.ProfileRepository.ProfileError;
import app.belqax.pature.data.repository.ProfileRepository.UpdateProfileRequest;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Экран профиля:
 *  - показывает кешированный профиль из storage;
 *  - обновляет данные с сервера;
 *  - позволяет редактировать текстовые поля и аватар;
 *  - для адреса сохраняет структурные поля (formatted/lat/lon/city...) в БД через /users/me/profile.
 *
 * ВАЖНО: для камеры нужен runtime-permission android.permission.CAMERA.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // Настройки кропа аватара
    private static final int AVATAR_CROP_MAX_WIDTH = 800;
    private static final int AVATAR_CROP_MAX_HEIGHT = 800;

    // Базовый URL нужен только если backend отдаёт относительные пути типа "/media/..."
    private static final String API_BASE_URL = "https://api.belqax.xyz";

    private ScrollView profileScroll;
    private ImageView profileAvatarImage;
    private TextView profileNameText;
    private TextView profileAgeText;
    private TextView profileAboutText;
    private TextView profileLocationText;

    private ImageButton profileEditNameButton;
    private ImageButton profileEditAgeButton;
    private ImageButton profileEditAboutButton;
    private ImageButton profileEditLocationButton;
    private ImageButton settingsButton;
    private MaterialButton myAnimalsButton;


    private ProfileRepository profileRepository;

    @Nullable
    private MeResponse currentMe;

    @Nullable
    private Uri pendingCameraPhotoUri;

    private ActivityResultLauncher<Uri> takePhotoLauncher;
    private ActivityResultLauncher<String> pickFromGalleryLauncher;
    private ActivityResultLauncher<Intent> cropImageLauncher;
    private ActivityResultLauncher<Intent> editFieldLauncher;

    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileRepository = ProfileRepository.create(requireContext());

        initPermissionLaunchers();
        initAvatarActivityResultLaunchers();
        initEditFieldLauncher();
    }

    private void initPermissionLaunchers() {
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (granted) {
                        Log.d(TAG, "Camera permission granted");
                        launchCameraCapture();
                    } else {
                        Log.w(TAG, "Camera permission denied");
                        Toast.makeText(
                                requireContext(),
                                R.string.profile_camera_permission_denied,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void initAvatarActivityResultLaunchers() {
        takePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (!success) {
                        Log.d(TAG, "TakePicture cancelled or failed");
                        return;
                    }
                    if (pendingCameraPhotoUri != null) {
                        startAvatarCrop(pendingCameraPhotoUri);
                    } else {
                        Log.w(TAG, "TakePicture success but pendingCameraPhotoUri is null");
                    }
                }
        );

        pickFromGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        startAvatarCrop(uri);
                    }
                }
        );

        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    int resultCode = result.getResultCode();
                    Intent data = result.getData();

                    if (resultCode == Activity.RESULT_OK && data != null) {
                        Uri resultUri = UCrop.getOutput(data);
                        if (resultUri != null) {
                            uploadAvatar(resultUri);
                        } else {
                            Log.w(TAG, "UCrop RESULT_OK but output uri is null");
                            Toast.makeText(
                                    requireContext(),
                                    R.string.profile_avatar_crop_failed,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    } else if (resultCode == UCrop.RESULT_ERROR && data != null) {
                        Throwable error = UCrop.getError(data);
                        Log.e(TAG, "UCrop error", error);
                        Toast.makeText(
                                requireContext(),
                                R.string.profile_avatar_crop_failed,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
    }

    private void initEditFieldLauncher() {
        editFieldLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK) {
                        return;
                    }

                    Intent data = result.getData();
                    if (data == null) {
                        return;
                    }

                    String fieldType = data.getStringExtra(EditProfileFieldActivity.EXTRA_FIELD_TYPE);
                    if (fieldType == null) {
                        return;
                    }

                    if (EditProfileFieldActivity.FIELD_TYPE_LOCATION.equals(fieldType)) {
                        applyLocationResult(data);
                        return;
                    }

                    String value = data.getStringExtra(EditProfileFieldActivity.EXTRA_RESULT_VALUE);
                    if (value == null) {
                        return;
                    }

                    switch (fieldType) {
                        case EditProfileFieldActivity.FIELD_TYPE_NAME:
                            updateName(value);
                            break;
                        case EditProfileFieldActivity.FIELD_TYPE_AGE:
                            updateAgeFromString(value);
                            break;
                        case EditProfileFieldActivity.FIELD_TYPE_ABOUT:
                            updateAbout(value);
                            break;
                        default:
                            Log.w(TAG, "Unknown field type from EditProfileFieldActivity: " + fieldType);
                            break;
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        setupClickListeners();

        MeResponse cached = profileRepository.getCachedProfile();
        if (cached != null) {
            currentMe = cached;
            bindProfileToUi(cached);
        } else {
            setPlaceholders();
        }

        loadProfile();
    }

    private void bindViews(@NonNull View root) {
        profileScroll = root.findViewById(R.id.profileScroll);
        profileAvatarImage = root.findViewById(R.id.profileAvatarImage);

        profileNameText = root.findViewById(R.id.profileNameText);
        profileAgeText = root.findViewById(R.id.profileAgeText);
        profileAboutText = root.findViewById(R.id.profileAboutText);
        profileLocationText = root.findViewById(R.id.profileLocationText);

        profileEditNameButton = root.findViewById(R.id.profileEditNameButton);
        profileEditAgeButton = root.findViewById(R.id.profileEditAgeButton);
        profileEditAboutButton = root.findViewById(R.id.profileEditAboutButton);
        profileEditLocationButton = root.findViewById(R.id.profileEditLocationButton);
        settingsButton = root.findViewById(R.id.profileSettingsButton);
        myAnimalsButton = root.findViewById(R.id.profileMyAnimalsButton);

    }

    private void setupClickListeners() {
        profileEditNameButton.setOnClickListener(v -> {
            String currentName = profileNameText.getText() != null
                    ? profileNameText.getText().toString()
                    : "";
            openEditFieldActivity(
                    EditProfileFieldActivity.FIELD_TYPE_NAME,
                    currentName
            );
        });

        profileEditAgeButton.setOnClickListener(v -> {
            String currentAge = profileAgeText.getText() != null
                    ? profileAgeText.getText().toString()
                    : "";
            openEditFieldActivity(
                    EditProfileFieldActivity.FIELD_TYPE_AGE,
                    currentAge
            );
        });

        profileEditAboutButton.setOnClickListener(v -> {
            String currentAbout = profileAboutText.getText() != null
                    ? profileAboutText.getText().toString()
                    : "";
            openEditFieldActivity(
                    EditProfileFieldActivity.FIELD_TYPE_ABOUT,
                    currentAbout
            );
        });

        profileEditLocationButton.setOnClickListener(v -> {
            String currentLocation = profileLocationText.getText() != null
                    ? profileLocationText.getText().toString()
                    : "";
            openEditFieldActivity(
                    EditProfileFieldActivity.FIELD_TYPE_LOCATION,
                    currentLocation
            );
        });

        myAnimalsButton.setOnClickListener(v -> openMyAnimals());

        settingsButton.setOnClickListener(v -> openSettings());

        profileAvatarImage.setOnClickListener(v -> showAvatarActionsBottomSheet());
    }

    private void openEditFieldActivity(@NonNull String fieldType,
                                       @NonNull String currentValue) {
        Intent intent = new Intent(requireContext(), EditProfileFieldActivity.class);
        intent.putExtra(EditProfileFieldActivity.EXTRA_FIELD_TYPE, fieldType);
        intent.putExtra(EditProfileFieldActivity.EXTRA_INITIAL_VALUE, currentValue);
        editFieldLauncher.launch(intent);
    }
    private void openSettings() {
        if (getContext() == null) {
            return;
        }
        Intent intent = new Intent(getContext(), ProfileSettingsActivity.class);
        startActivity(intent);
    }

    private void openMyAnimals() {
        if (!isAdded()) {
            return;
        }
        Intent intent = new Intent(requireContext(), app.belqax.pature.activity.MyAnimalsActivity.class);
        startActivity(intent);
    }

    private void showAvatarActionsBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View contentView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_avatar_actions, null, false);
        dialog.setContentView(contentView);

        LinearLayout takePhotoRow = contentView.findViewById(R.id.avatarTakePhotoRow);
        LinearLayout pickFromGalleryRow = contentView.findViewById(R.id.avatarPickFromGalleryRow);
        LinearLayout removePhotoRow = contentView.findViewById(R.id.avatarRemovePhotoRow);

        if (takePhotoRow != null) {
            takePhotoRow.setOnClickListener(v -> {
                dialog.dismiss();
                onTakePhotoClicked();
            });
        }

        if (pickFromGalleryRow != null) {
            pickFromGalleryRow.setOnClickListener(v -> {
                dialog.dismiss();
                onPickFromGalleryClicked();
            });
        }

        if (removePhotoRow != null) {
            removePhotoRow.setOnClickListener(v -> {
                dialog.dismiss();
                onRemoveAvatarClicked();
            });
        }

        dialog.show();
    }

    private void onTakePhotoClicked() {
        if (!isAdded()) {
            return;
        }

        if (hasCameraPermission()) {
            launchCameraCapture();
            return;
        }

        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private boolean hasCameraPermission() {
        if (!isAdded()) {
            return false;
        }
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void launchCameraCapture() {
        try {
            Uri outputUri = createTempImageUri();
            pendingCameraPhotoUri = outputUri;
            takePhotoLauncher.launch(outputUri);
        } catch (SecurityException se) {
            Log.e(TAG, "launchCameraCapture: SecurityException", se);
            if (isAdded()) {
                Toast.makeText(
                        requireContext(),
                        R.string.profile_camera_permission_denied,
                        Toast.LENGTH_LONG
                ).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "launchCameraCapture: failed", e);
            if (isAdded()) {
                Toast.makeText(
                        requireContext(),
                        R.string.profile_camera_permission_denied,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void onPickFromGalleryClicked() {
        pickFromGalleryLauncher.launch("image/*");
    }

    private void onRemoveAvatarClicked() {
        setUiEnabled(false);
        profileRepository.deleteAvatar(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull MeResponse meResponse) {
                if (!isAdded()) {
                    return;
                }
                currentMe = meResponse;
                bindProfileToUi(meResponse);
                setUiEnabled(true);
                Toast.makeText(
                        requireContext(),
                        R.string.profile_avatar_remove_success,
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onError(@NonNull ProfileError error) {
                if (!isAdded()) {
                    return;
                }
                setUiEnabled(true);
                Toast.makeText(
                        requireContext(),
                        error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
                if (error.isUnauthorized()) {
                    handleUnauthorized();
                }
            }
        });
    }

    @NonNull
    private Uri createTempImageUri() {
        File imagesDir = new File(requireContext().getCacheDir(), "images");
        if (!imagesDir.exists() && !imagesDir.mkdirs()) {
            Log.w(TAG, "createTempImageUri: failed to create images dir");
        }
        File tempFile = new File(
                imagesDir,
                "avatar_source_" + System.currentTimeMillis() + ".jpg"
        );
        return FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                tempFile
        );
    }

    private void startAvatarCrop(@NonNull Uri sourceUri) {
        File destFile = new File(
                requireContext().getCacheDir(),
                "avatar_cropped_" + System.currentTimeMillis() + ".jpg"
        );
        Uri destUri = Uri.fromFile(destFile);

        UCrop uCrop = UCrop.of(sourceUri, destUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(
                        AVATAR_CROP_MAX_WIDTH,
                        AVATAR_CROP_MAX_HEIGHT
                );

        Intent intent = uCrop.getIntent(requireContext());
        cropImageLauncher.launch(intent);
    }

    // region Load / bind

    private void loadProfile() {
        setUiEnabled(false);
        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull MeResponse meResponse) {
                if (!isAdded()) {
                    return;
                }
                currentMe = meResponse;
                bindProfileToUi(meResponse);
                setUiEnabled(true);
            }

            @Override
            public void onError(@NonNull ProfileError error) {
                if (!isAdded()) {
                    return;
                }
                setUiEnabled(true);
                Log.w(TAG, "loadProfile: error " + error.getMessage());
                Toast.makeText(
                        requireContext(),
                        error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();

                if (error.isUnauthorized()) {
                    handleUnauthorized();
                }
            }
        });
    }

    private void bindProfileToUi(@NonNull MeResponse meResponse) {
        ProfileDto profile = meResponse.getProfile();
        if (profile == null) {
            setPlaceholders();
            return;
        }

        String name = profile.getDisplayName();
        if (isNullOrBlank(name)) {
            name = getString(R.string.profile_name_placeholder);
        }
        profileNameText.setText(name);

        if (profile.getAge() != null && profile.getAge() > 0) {
            profileAgeText.setText(String.valueOf(profile.getAge()));
        } else {
            profileAgeText.setText(getString(R.string.profile_age_placeholder));
        }

        if (!isNullOrBlank(profile.getAbout())) {
            profileAboutText.setText(profile.getAbout());
        } else {
            profileAboutText.setText(getString(R.string.profile_about_placeholder));
        }

        String locationText = readLocationForUi(profile);
        if (!isNullOrBlank(locationText)) {
            profileLocationText.setText(locationText);
        } else {
            profileLocationText.setText(getString(R.string.profile_location_placeholder));
        }

        String avatarUrl = profile.getAvatarUrl();
        if (!isNullOrBlank(avatarUrl)) {
            String fullUrl = normalizeToAbsoluteUrl(avatarUrl);
            Glide.with(this)
                    .load(fullUrl)
                    .placeholder(R.drawable.ic_cat)
                    .error(R.drawable.ic_cat)
                    .into(profileAvatarImage);
        } else {
            profileAvatarImage.setImageResource(R.drawable.ic_cat);
        }
    }

    @NonNull
    private String normalizeToAbsoluteUrl(@NonNull String url) {
        String trimmed = url.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        if (trimmed.startsWith("/")) {
            return API_BASE_URL + trimmed;
        }
        return API_BASE_URL + "/" + trimmed;
    }

    @Nullable
    private String readLocationForUi(@NonNull ProfileDto profile) {

        String city = null;
        try {
            city = profile.getLocationCity();
        } catch (Throwable ignored) {
        }

        if (!isNullOrBlank(city)) {
            return city;
        }

        return null;
    }

    private void setPlaceholders() {
        profileNameText.setText(getString(R.string.profile_name_placeholder));
        profileAgeText.setText(getString(R.string.profile_age_placeholder));
        profileAboutText.setText(getString(R.string.profile_about_placeholder));
        profileLocationText.setText(getString(R.string.profile_location_placeholder));
        profileAvatarImage.setImageResource(R.drawable.ic_cat);
    }

    private void setUiEnabled(boolean enabled) {
        if (profileScroll != null) {
            profileScroll.setAlpha(enabled ? 1.0f : 0.6f);
        }
        profileEditNameButton.setEnabled(enabled);
        profileEditAgeButton.setEnabled(enabled);
        profileEditAboutButton.setEnabled(enabled);
        profileEditLocationButton.setEnabled(enabled);
        profileAvatarImage.setEnabled(enabled);
    }

    // endregion

    // region Update profile fields

    private void updateName(@NonNull String newName) {
        MeResponse me = currentMe;
        if (me == null || me.getProfile() == null) {
            return;
        }
        String trimmed = newName.trim();
        if (trimmed.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    R.string.profile_name_empty_error,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        UpdateProfileRequest body = new UpdateProfileRequest(
                trimmed,
                null,
                null,

                null,
                null,
                null,
                null,
                null,

                null,
                null,

                null,
                null
        );

        sendUpdateProfile(body);
    }

    private void updateAgeFromString(@NonNull String ageString) {
        MeResponse me = currentMe;
        if (me == null || me.getProfile() == null) {
            return;
        }

        Integer age = null;
        String trimmed = ageString.trim();
        if (!trimmed.isEmpty()) {
            try {
                int value = Integer.parseInt(trimmed);
                if (value <= 0 || value > 120) {
                    Toast.makeText(
                            requireContext(),
                            R.string.profile_age_invalid_error,
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
                age = value;
            } catch (NumberFormatException e) {
                Toast.makeText(
                        requireContext(),
                        R.string.profile_age_invalid_error,
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
        }

        UpdateProfileRequest body = new UpdateProfileRequest(
                null,
                age,
                null,

                null,
                null,
                null,
                null,
                null,

                null,
                null,

                null,
                null
        );

        sendUpdateProfile(body);
    }

    private void updateAbout(@NonNull String newAbout) {
        MeResponse me = currentMe;
        if (me == null || me.getProfile() == null) {
            return;
        }

        String trimmed = newAbout.trim();
        if (trimmed.length() > 512) {
            Toast.makeText(
                    requireContext(),
                    R.string.profile_about_too_long_error,
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        UpdateProfileRequest body = new UpdateProfileRequest(
                null,
                null,
                trimmed.isEmpty() ? null : trimmed,

                null,
                null,
                null,
                null,
                null,

                null,
                null,

                null,
                null
        );

        sendUpdateProfile(body);
    }

    private void applyLocationResult(@NonNull Intent data) {
        MeResponse me = currentMe;
        if (me == null || me.getProfile() == null) {
            return;
        }

        EditProfileFieldActivity.LocationPayload payload = null;
        try {
            payload = (EditProfileFieldActivity.LocationPayload)
                    data.getSerializableExtra(EditProfileFieldActivity.EXTRA_RESULT_LOCATION);
        } catch (Throwable t) {
            Log.w(TAG, "applyLocationResult: failed to read EXTRA_RESULT_LOCATION", t);
        }

        if (payload != null) {
            UpdateProfileRequest body = new UpdateProfileRequest(
                    null,
                    null,
                    null,

                    normalizeNullable(payload.location_formatted),
                    normalizeNullable(payload.location_city),
                    normalizeNullable(payload.location_state),
                    normalizeNullable(payload.location_country),
                    normalizeNullable(payload.location_postcode),

                    payload.location_lat,
                    payload.location_lon,

                    normalizeNullable(payload.location_result_type),
                    payload.location_confidence
            );

            Log.d(TAG, "applyLocationResult(payload): formatted=" + payload.location_formatted
                    + " city=" + payload.location_city
                    + " state=" + payload.location_state
                    + " country=" + payload.location_country
                    + " postcode=" + payload.location_postcode
                    + " lat=" + payload.location_lat
                    + " lon=" + payload.location_lon
                    + " result_type=" + payload.location_result_type
                    + " confidence=" + payload.location_confidence);

            sendUpdateProfile(body);
            return;
        }

        String fallbackText = data.getStringExtra(EditProfileFieldActivity.EXTRA_RESULT_VALUE);
        String typed = normalizeNullable(fallbackText);

        UpdateProfileRequest body = new UpdateProfileRequest(
                null,
                null,
                null,

                typed,
                null,
                null,
                null,
                null,

                null,
                null,

                null,
                null
        );

        Log.d(TAG, "applyLocationResult(fallback): formatted=" + typed);
        sendUpdateProfile(body);
    }

    @Nullable
    private String normalizeNullable(@Nullable String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void sendUpdateProfile(@NonNull UpdateProfileRequest body) {
        setUiEnabled(false);
        Log.d(TAG, "sendUpdateProfile: calling PUT /users/me/profile");

        profileRepository.updateProfile(body, new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull MeResponse meResponse) {
                if (!isAdded()) {
                    return;
                }
                currentMe = meResponse;
                bindProfileToUi(meResponse);
                setUiEnabled(true);
            }

            @Override
            public void onError(@NonNull ProfileError error) {
                if (!isAdded()) {
                    return;
                }
                setUiEnabled(true);
                Toast.makeText(
                        requireContext(),
                        error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
                if (error.isUnauthorized()) {
                    handleUnauthorized();
                }
            }
        });
    }

    // endregion

    // region Avatar upload

    private void uploadAvatar(@NonNull Uri uri) {
        MultipartBody.Part filePart;
        try {
            filePart = createAvatarPartFromUri(uri);
        } catch (IOException e) {
            Log.e(TAG, "uploadAvatar: failed to create part", e);
            Toast.makeText(
                    requireContext(),
                    R.string.profile_avatar_read_error,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        setUiEnabled(false);
        profileRepository.uploadAvatar(filePart, new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(@NonNull MeResponse meResponse) {
                if (!isAdded()) {
                    return;
                }
                currentMe = meResponse;
                bindProfileToUi(meResponse);
                setUiEnabled(true);
            }

            @Override
            public void onError(@NonNull ProfileError error) {
                if (!isAdded()) {
                    return;
                }
                setUiEnabled(true);
                Toast.makeText(
                        requireContext(),
                        error.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
                if (error.isUnauthorized()) {
                    handleUnauthorized();
                }
            }
        });
    }

    @NonNull
    private MultipartBody.Part createAvatarPartFromUri(@NonNull Uri uri) throws IOException {
        ContentResolver contentResolver = requireContext().getContentResolver();
        String mimeType = contentResolver.getType(uri);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }

        String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (InputStream inputStream = contentResolver.openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Cannot open input stream for uri: " + uri);
            }
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }

        byte[] bytes = outputStream.toByteArray();
        RequestBody requestBody = RequestBody.create(bytes, MediaType.parse(mimeType));
        return MultipartBody.Part.createFormData("file", fileName, requestBody);
    }

    // endregion

    // region Helpers

    private boolean isNullOrBlank(@Nullable String value) {
        return value == null || value.trim().isEmpty();
    }

    private void handleUnauthorized() {
        Log.w(TAG, "handleUnauthorized: unauthorized user, should logout");

        // Здесь вызывается общий logout:
        // 1. authStorage.clear()
        // 2. profileRepository.clearProfile()
        // 3. переход на экран логина
    }

    // endregion
}
