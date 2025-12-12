package app.belqax.pature.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import app.belqax.pature.R;
import app.belqax.pature.data.repository.AuthRepository;

public class ForgotPasswordBottomSheet extends BottomSheetDialogFragment {

    private final AuthRepository authRepository;

    private View stepEmail;
    private View stepCode;
    private TextInputEditText inputEmail;
    private MaterialButton buttonSend;

    private TextInputEditText inputCode;
    private TextInputEditText inputNewPassword;
    private MaterialButton buttonReset;

    private String email;

    public ForgotPasswordBottomSheet(@NonNull AuthRepository repository) {
        this.authRepository = repository;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.bottomsheet_forgot_password, container, false);

        stepEmail = view.findViewById(R.id.layoutEmailStep);
        stepCode = view.findViewById(R.id.layoutCodeStep);

        inputEmail = view.findViewById(R.id.inputEmail);
        buttonSend = view.findViewById(R.id.buttonSend);
        inputCode = view.findViewById(R.id.inputCode);
        inputNewPassword = view.findViewById(R.id.inputNewPassword);
        buttonReset = view.findViewById(R.id.buttonReset);

        stepEmail.setVisibility(View.VISIBLE);
        stepCode.setVisibility(View.GONE);

        buttonSend.setOnClickListener(v -> sendEmail());
        buttonReset.setOnClickListener(v -> resetPassword());
        return view;
    }

    private void sendEmail() {
        clearEmailErrors();

        email = getTrimmed(inputEmail);
        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("Введите e-mail");
            return;
        }
        if (!isValidEmail(email)) {
            inputEmail.setError("Некорректный e-mail");
            return;
        }

        buttonSend.setEnabled(false);
        buttonSend.setText("Отправляем...");

        authRepository.forgotPassword(email, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    buttonSend.setEnabled(true);
                    buttonSend.setText("Отправить код");
                    Toast.makeText(getContext(), "Код отправлен на почту", Toast.LENGTH_SHORT).show();
                    stepEmail.setVisibility(View.GONE);
                    stepCode.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(@NonNull String message) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    buttonSend.setEnabled(true);
                    buttonSend.setText("Отправить код");
                    handleForgotErrorUi(message);
                });
            }
        });
    }

    private void resetPassword() {
        clearResetErrors();

        String code = getTrimmed(inputCode);
        String newPassword = getTrimmed(inputNewPassword);

        if (TextUtils.isEmpty(code)) {
            inputCode.setError("Введите код");
            return;
        }
        if (TextUtils.isEmpty(newPassword)) {
            inputNewPassword.setError("Введите новый пароль");
            return;
        }

        buttonReset.setEnabled(false);
        buttonReset.setText("Сбрасываем...");

        authRepository.resetPassword(email, code, newPassword, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    buttonReset.setEnabled(true);
                    buttonReset.setText("Сбросить пароль");
                    Toast.makeText(getContext(), "Пароль изменён!", Toast.LENGTH_SHORT).show();
                    dismiss();
                });
            }

            @Override
            public void onError(@NonNull String message) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    buttonReset.setEnabled(true);
                    buttonReset.setText("Сбросить пароль");
                    handleResetErrorUi(message);
                });
            }
        });
    }

    private void clearEmailErrors() {
        inputEmail.setError(null);
    }

    private void clearResetErrors() {
        inputCode.setError(null);
        inputNewPassword.setError(null);
    }

    private void handleForgotErrorUi(@NonNull String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("сетев")) {
            Toast.makeText(getContext(), "Проблема с подключением: " + message, Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("email") && lower.contains("required")) {
            inputEmail.setError("Введите email");
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void handleResetErrorUi(@NonNull String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("сетев")) {
            Toast.makeText(getContext(), "Проблема с подключением: " + message, Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("invalid reset code")
                || lower.contains("invalid code")
                || lower.contains("неверный код")) {
            inputCode.setError("Неверный код");
            Toast.makeText(getContext(), "Проверьте код из письма", Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("no active reset code")) {
            inputCode.setError("Срок действия кода истёк");
            Toast.makeText(
                    getContext(),
                    "Срок действия кода истёк. Запросите новый код.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (lower.contains("maximum reset attempts exceeded")) {
            inputCode.setError("Превышено количество попыток");
            Toast.makeText(
                    getContext(),
                    "Превышено количество попыток ввода кода. Попробуйте позже.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (lower.contains("user not found")) {
            inputEmail.setError("Пользователь не найден");
            Toast.makeText(getContext(), "Проверьте адрес email", Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("password") || lower.contains("парол")) {
            inputNewPassword.setError("Пароль не подходит: " + message);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private String getTrimmed(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private boolean isValidEmail(@NonNull String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}
