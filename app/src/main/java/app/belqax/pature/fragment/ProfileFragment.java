package app.belqax.pature.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;

import app.belqax.pature.R;
import app.belqax.pature.activity.EditProfileFieldActivity;
import app.belqax.pature.activity.ProfileSettingsActivity;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // Заглушки профиля до подключения реальных данных
    private static final String DEFAULT_NAME = "Имя пользователя";
    private static final String DEFAULT_AGE_TEXT = "Возраст не указан";
    private static final String DEFAULT_ABOUT =
            "Расскажите о себе, кого вы ищете и с какими животными живёте.";
    private static final String DEFAULT_LOCATION = "Местоположение не указано";

    // Настройки кадрирования
    private static final float AVATAR_CROP_ASPECT_X = 1f;
    private static final float AVATAR_CROP_ASPECT_Y = 1f;
    private static final int AVATAR_CROP_MAX_SIZE = 512;

    // Работа с аватаром
    @Nullable
    private ImageView avatarImageView;
    @Nullable
    private Uri currentAvatarUri;
    @Nullable
    private Uri pendingCameraAvatarUri;

    @Nullable
    private ActivityResultLauncher<Uri> takePictureLauncher;
    @Nullable
    private ActivityResultLauncher<String> pickImageLauncher;
    @Nullable
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    @Nullable
    private ActivityResultLauncher<Intent> cropImageLauncher;

    @Nullable
    private ProfileAvatarStore avatarStore;

    public ProfileFragment() {
    }

    @NonNull
    public static ProfileFragment newInstance() {
        return new ProfileFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        avatarStore = new ProfileAvatarStore(context.getApplicationContext());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initActivityResultLaunchers();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View root,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(root, savedInstanceState);
        initHeader(root);
        initProfileContent(root);
    }

    private void initHeader(@NonNull View root) {
        TextView title = root.findViewById(R.id.profileTitle);
        ImageButton settingsButton = root.findViewById(R.id.profileSettingsButton);

        if (title != null) {
            title.setText(R.string.profile_title);
        }

        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> openSettingsActivity());
        }
    }

    private void initProfileContent(@NonNull View root) {
        avatarImageView = root.findViewById(R.id.profileAvatarImage);

        TextView nameText = root.findViewById(R.id.profileNameText);
        TextView ageText = root.findViewById(R.id.profileAgeText);
        TextView aboutText = root.findViewById(R.id.profileAboutText);
        TextView locationText = root.findViewById(R.id.profileLocationText);

        ImageButton editNameButton = root.findViewById(R.id.profileEditNameButton);
        ImageButton editAgeButton = root.findViewById(R.id.profileEditAgeButton);
        ImageButton editAboutButton = root.findViewById(R.id.profileEditAboutButton);
        ImageButton editLocationButton = root.findViewById(R.id.profileEditLocationButton);

        // Аватар
        if (avatarImageView != null) {
            if (avatarStore != null) {
                currentAvatarUri = avatarStore.getAvatarUri();
            }
            applyAvatarImage();

            avatarImageView.setOnClickListener(v -> showAvatarActionsBottomSheet());
        }

        // Имя / возраст / о себе / местоположение — пока заглушки
        if (nameText != null) {
            nameText.setText(DEFAULT_NAME);
        }
        if (ageText != null) {
            ageText.setText(DEFAULT_AGE_TEXT);
        }
        if (aboutText != null) {
            aboutText.setText(DEFAULT_ABOUT);
        }
        if (locationText != null) {
            locationText.setText(DEFAULT_LOCATION);
        }

        // Кнопки редактирования
        if (editNameButton != null) {
            editNameButton.setOnClickListener(
                    v -> openEditFieldActivity(EditProfileFieldActivity.FIELD_TYPE_NAME));
        }
        if (editAgeButton != null) {
            editAgeButton.setOnClickListener(
                    v -> openEditFieldActivity(EditProfileFieldActivity.FIELD_TYPE_AGE));
        }
        if (editAboutButton != null) {
            editAboutButton.setOnClickListener(
                    v -> openEditFieldActivity(EditProfileFieldActivity.FIELD_TYPE_ABOUT));
        }
        if (editLocationButton != null) {
            editLocationButton.setOnClickListener(
                    v -> openEditFieldActivity(EditProfileFieldActivity.FIELD_TYPE_LOCATION));
        }
    }

    private void initActivityResultLaunchers() {
        // Камера
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && pendingCameraAvatarUri != null) {
                        // Сначала кадрирование
                        startAvatarCrop(pendingCameraAvatarUri);
                        pendingCameraAvatarUri = null;
                    } else {
                        Log.d(TAG, "Camera capture cancelled or failed.");
                        pendingCameraAvatarUri = null;
                    }
                }
        );

        // Галерея
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        startAvatarCrop(uri);
                    } else {
                        Log.d(TAG, "Gallery pick cancelled.");
                    }
                }
        );

        // Разрешение камеры
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        startCameraCaptureInternal();
                    } else {
                        showToastSafe(R.string.profile_camera_permission_denied);
                    }
                }
        );

        // Результат uCrop
        cropImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri outputUri = UCrop.getOutput(result.getData());
                        if (outputUri != null) {
                            handleNewAvatar(outputUri);
                        } else {
                            Log.w(TAG, "UCrop result has null output Uri");
                            showToastSafe(R.string.profile_avatar_crop_failed);
                        }
                    } else if (result.getResultCode() == UCrop.RESULT_ERROR && result.getData() != null) {
                        Throwable error = UCrop.getError(result.getData());
                        Log.e(TAG, "UCrop error", error);
                        showToastSafe(R.string.profile_avatar_crop_failed);
                    } else {
                        Log.d(TAG, "UCrop cancelled or no data.");
                    }
                }
        );
    }

    private void openSettingsActivity() {
        try {
            startActivity(ProfileSettingsActivity.class);
        } catch (Exception e) {
            Log.e(TAG, "Error starting ProfileSettingsActivity", e);
            showToastSafe(R.string.profile_open_settings_error);
        }
    }

    private void openEditFieldActivity(@NonNull String fieldType) {
        try {
            if (!isAdded()) {
                return;
            }
            Intent intent =
                    new Intent(requireContext(), EditProfileFieldActivity.class);
            intent.putExtra(EditProfileFieldActivity.EXTRA_FIELD_TYPE, fieldType);
            startActivity(intent);
            applySlideUpAnimation();
        } catch (Exception e) {
            Log.e(TAG, "Error starting EditProfileFieldActivity, type=" + fieldType, e);
            showToastSafe(R.string.profile_open_edit_error);
        }
    }

    private void startActivity(@NonNull Class<?> activityClass) {
        if (!isAdded()) {
            return;
        }
        Intent intent = new Intent(requireContext(), activityClass);
        startActivity(intent);
        applySlideUpAnimation();
    }

    private void applySlideUpAnimation() {
        if (!isAdded()) {
            return;
        }
        try {
            requireActivity().overridePendingTransition(
                    R.anim.slide_in_up,
                    R.anim.stay
            );
        } catch (Exception e) {
            Log.w(TAG, "overridePendingTransition failed", e);
        }
    }

    // --------- Работа с аватаром ---------

    private void showAvatarActionsBottomSheet() {
        if (!isAdded()) {
            return;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_avatar_actions, null, false);
        dialog.setContentView(sheetView);

        View takePhotoRow = sheetView.findViewById(R.id.avatarTakePhotoRow);
        View pickFromGalleryRow = sheetView.findViewById(R.id.avatarPickFromGalleryRow);
        View removePhotoRow = sheetView.findViewById(R.id.avatarRemovePhotoRow);

        if (takePhotoRow != null) {
            takePhotoRow.setOnClickListener(v -> {
                dialog.dismiss();
                startCameraCapture();
            });
        }

        if (pickFromGalleryRow != null) {
            pickFromGalleryRow.setOnClickListener(v -> {
                dialog.dismiss();
                startGalleryPick();
            });
        }

        if (removePhotoRow != null) {
            removePhotoRow.setOnClickListener(v -> {
                dialog.dismiss();
                deleteAvatar();
            });
        }

        dialog.show();
    }

    private void startCameraCapture() {
        if (!isAdded()) {
            return;
        }
        Context context = requireContext();

        boolean hasCamera = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY);
        if (!hasCamera) {
            showToastSafe(R.string.profile_camera_not_available);
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            if (requestCameraPermissionLauncher != null) {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
            return;
        }

        startCameraCaptureInternal();
    }

    private void startCameraCaptureInternal() {
        if (!isAdded()) {
            return;
        }
        if (takePictureLauncher == null) {
            Log.w(TAG, "takePictureLauncher is null");
            return;
        }

        try {
            Uri uri = createAvatarImageUri();
            pendingCameraAvatarUri = uri;
            takePictureLauncher.launch(uri);
        } catch (IOException e) {
            Log.e(TAG, "Failed to create image file for avatar", e);
            showToastSafe(R.string.profile_camera_create_file_error);
        }
    }

    private void startGalleryPick() {
        if (pickImageLauncher == null) {
            Log.w(TAG, "pickImageLauncher is null");
            return;
        }
        pickImageLauncher.launch("image/*");
    }

    private Uri createAvatarImageUri() throws IOException {
        if (!isAdded()) {
            throw new IOException("Fragment is not attached.");
        }
        Context context = requireContext();
        File imagesDir = new File(context.getFilesDir(), "avatar_images");
        if (!imagesDir.exists() && !imagesDir.mkdirs()) {
            throw new IOException("Failed to create avatar_images directory.");
        }

        String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
        File imageFile = new File(imagesDir, fileName);

        String authority = context.getPackageName() + ".fileprovider";

        return FileProvider.getUriForFile(
                context,
                authority,
                imageFile
        );
    }

    private void startAvatarCrop(@NonNull Uri sourceUri) {
        if (!isAdded()) {
            return;
        }
        if (cropImageLauncher == null) {
            Log.w(TAG, "cropImageLauncher is null");
            return;
        }

        Context context = requireContext();
        File destFile = new File(
                context.getCacheDir(),
                "avatar_cropped_" + System.currentTimeMillis() + ".jpg"
        );
        Uri destUri = Uri.fromFile(destFile);

        UCrop.Options options = new UCrop.Options();
        // Можно подстроить цвета под тему при необходимости:
        // options.setToolbarColor(...);
        // options.setStatusBarColor(...);
        // options.setActiveControlsWidgetColor(...);

        options.setHideBottomControls(false);      // оставить кнопки поворота
        options.setFreeStyleCropEnabled(false);    // фиксированный квадрат

        Intent cropIntent = UCrop.of(sourceUri, destUri)
                .withAspectRatio(AVATAR_CROP_ASPECT_X, AVATAR_CROP_ASPECT_Y)
                .withMaxResultSize(AVATAR_CROP_MAX_SIZE, AVATAR_CROP_MAX_SIZE)
                .withOptions(options)
                .getIntent(context);

        cropImageLauncher.launch(cropIntent);
    }

    private void handleNewAvatar(@NonNull Uri uri) {
        currentAvatarUri = uri;
        if (avatarStore != null) {
            avatarStore.saveAvatarUri(uri);
        }
        applyAvatarImage();
    }

    private void deleteAvatar() {
        currentAvatarUri = null;
        if (avatarStore != null) {
            avatarStore.saveAvatarUri(null);
        }
        applyAvatarImage();
        showToastSafe(R.string.profile_avatar_removed);
    }

    private void applyAvatarImage() {
        if (avatarImageView == null || !isAdded()) {
            return;
        }

        if (currentAvatarUri != null) {
            Glide.with(this)
                    .load(currentAvatarUri)
                    .placeholder(R.drawable.ic_cat)
                    .error(R.drawable.ic_cat)
                    .centerCrop()
                    .into(avatarImageView);
        } else {
            avatarImageView.setImageResource(R.drawable.ic_cat);
        }
    }

    private void showToastSafe(int resId) {
        if (!isAdded()) {
            return;
        }
        Toast.makeText(requireContext(), resId, Toast.LENGTH_SHORT).show();
    }

    // Хранилище для URI аватара в SharedPreferences
    private static final class ProfileAvatarStore {

        private static final String PREFS_NAME = "profile_prefs";
        private static final String KEY_AVATAR_URI = "avatar_uri";

        @NonNull
        private final SharedPreferences prefs;

        ProfileAvatarStore(@NonNull Context appContext) {
            this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        @Nullable
        Uri getAvatarUri() {
            String value = prefs.getString(KEY_AVATAR_URI, null);
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                return Uri.parse(value);
            } catch (Exception e) {
                return null;
            }
        }

        void saveAvatarUri(@Nullable Uri uri) {
            SharedPreferences.Editor editor = prefs.edit();
            if (uri == null) {
                editor.remove(KEY_AVATAR_URI);
            } else {
                editor.putString(KEY_AVATAR_URI, uri.toString());
            }
            editor.apply();
        }
    }
}
