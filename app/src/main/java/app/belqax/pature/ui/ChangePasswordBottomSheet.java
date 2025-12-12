package app.belqax.pature.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import app.belqax.pature.R;
import app.belqax.pature.data.repository.AuthRepository;
import app.belqax.pature.data.storage.AuthStorage;

public class ChangePasswordBottomSheet extends BottomSheetDialogFragment {

    private static final String TAG = "ChangePasswordSheet";
    private static final int PASSWORD_MIN_LEN = 8;

    private AuthRepository authRepository;

    private TextInputLayout oldPassLayout;
    private TextInputLayout newPassLayout;
    private TextInputLayout confirmPassLayout;

    private TextInputEditText oldPassInput;
    private TextInputEditText newPassInput;
    private TextInputEditText confirmPassInput;

    private MaterialButton cancelButton;
    private MaterialButton saveButton;

    public static ChangePasswordBottomSheet newInstance() {
        return new ChangePasswordBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        AuthStorage authStorage = new AuthStorage(requireContext().getApplicationContext());
        authRepository = new AuthRepository(authStorage);

        bindViews(view);
        bindActions();
    }

    private void bindViews(@NonNull View root) {
        oldPassLayout = root.findViewById(R.id.changePasswordOldLayout);
        newPassLayout = root.findViewById(R.id.changePasswordNewLayout);
        confirmPassLayout = root.findViewById(R.id.changePasswordConfirmLayout);

        oldPassInput = root.findViewById(R.id.changePasswordOldInput);
        newPassInput = root.findViewById(R.id.changePasswordNewInput);
        confirmPassInput = root.findViewById(R.id.changePasswordConfirmInput);

        cancelButton = root.findViewById(R.id.changePasswordCancelButton);
        saveButton = root.findViewById(R.id.changePasswordSaveButton);
    }

    private void bindActions() {
        cancelButton.setOnClickListener(v -> dismissAllowingStateLoss());
        saveButton.setOnClickListener(v -> submit());
    }

    private void submit() {
        clearErrors();

        String oldPass = safeText(oldPassInput);
        String newPass = safeText(newPassInput);
        String confirm = safeText(confirmPassInput);

        if (!validate(oldPass, newPass, confirm)) {
            return;
        }

        setLoading(true);

        AuthRepository.PasswordChangeRequest req =
                new AuthRepository.PasswordChangeRequest(oldPass, newPass);

        authRepository.changePassword(req, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);

                String detail = getString(R.string.change_password_success);

                Toast.makeText(requireContext(), detail, Toast.LENGTH_SHORT).show();
                dismissAllowingStateLoss();
            }

            @Override
            public void onError(@NonNull String message) {
                setLoading(false);

                Log.w(TAG, "changePassword error: " + message);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

            }
        });
    }

    private boolean validate(@NonNull String oldPass,
                             @NonNull String newPass,
                             @NonNull String confirm) {
        boolean ok = true;

        if (oldPass.length() < PASSWORD_MIN_LEN) {
            oldPassLayout.setError(getString(R.string.change_password_error_old_min));
            ok = false;
        }
        if (newPass.length() < PASSWORD_MIN_LEN) {
            newPassLayout.setError(getString(R.string.change_password_error_new_min));
            ok = false;
        }
        if (newPass.equals(oldPass)) {
            newPassLayout.setError(getString(R.string.change_password_error_same));
            ok = false;
        }
        if (!newPass.equals(confirm)) {
            confirmPassLayout.setError(getString(R.string.change_password_error_confirm));
            ok = false;
        }

        return ok;
    }

    private void clearErrors() {
        oldPassLayout.setError(null);
        newPassLayout.setError(null);
        confirmPassLayout.setError(null);
    }

    private void setLoading(boolean loading) {
        saveButton.setEnabled(!loading);
        cancelButton.setEnabled(!loading);

        oldPassInput.setEnabled(!loading);
        newPassInput.setEnabled(!loading);
        confirmPassInput.setEnabled(!loading);

        saveButton.setText(loading
                ? getString(R.string.change_password_saving)
                : getString(R.string.change_password_save));
    }

    @NonNull
    private String safeText(@Nullable TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }
}