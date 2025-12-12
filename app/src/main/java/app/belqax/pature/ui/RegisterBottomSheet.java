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
import app.belqax.pature.activity.LoginActivity;
import app.belqax.pature.data.repository.AuthRepository;

public class RegisterBottomSheet extends BottomSheetDialogFragment {

    private static final int PHONE_MIN_LENGTH = 10;
    private static final int PHONE_MAX_LENGTH = 15;

    private final AuthRepository authRepository;

    private View stepRegister;
    private View stepCode;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPassword;
    private TextInputEditText inputPhone;
    private MaterialButton buttonContinue;

    private TextInputEditText inputCode;
    private MaterialButton buttonConfirm;
    private MaterialButton buttonResend;

    private String email;
    private String password;
    private String phone;

    public RegisterBottomSheet(@NonNull AuthRepository repository) {
        this.authRepository = repository;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.bottomsheet_register, container, false);

        stepRegister = view.findViewById(R.id.layoutRegisterStep);
        stepCode = view.findViewById(R.id.layoutCodeStep);

        inputEmail = view.findViewById(R.id.inputEmail);
        inputPassword = view.findViewById(R.id.inputPassword);
        inputPhone = view.findViewById(R.id.inputPhone);
        buttonContinue = view.findViewById(R.id.buttonContinue);

        inputCode = view.findViewById(R.id.inputCode);
        buttonConfirm = view.findViewById(R.id.buttonConfirm);
        buttonResend = view.findViewById(R.id.buttonResend);

        stepRegister.setVisibility(View.VISIBLE);
        stepCode.setVisibility(View.GONE);

        buttonContinue.setOnClickListener(v -> startRegister());
        buttonConfirm.setOnClickListener(v -> confirmCode());
        buttonResend.setOnClickListener(v -> resendCode());

        return view;
    }

    private void startRegister() {
        clearRegisterErrors();

        email = getTrimmed(inputEmail);
        password = getTrimmed(inputPassword);
        phone = getTrimmed(inputPhone);

        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("Введите e-mail");
            return;
        }
        if (!isValidEmail(email)) {
            inputEmail.setError("Некорректный e-mail");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("Введите пароль");
            return;
        }

        if (!TextUtils.isEmpty(phone) && !isValidPhone(phone)) {
            inputPhone.setError("Некорректный номер телефона");
            return;
        }

        buttonContinue.setEnabled(false);
        buttonContinue.setText("Отправка...");

        authRepository.register(email, password, phone, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    buttonContinue.setEnabled(true);
                    buttonContinue.setText("Продолжить");
                    Toast.makeText(getContext(), "Письмо с кодом отправлено", Toast.LENGTH_SHORT).show();
                    stepRegister.setVisibility(View.GONE);
                    stepCode.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(@NonNull String message) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    buttonContinue.setEnabled(true);
                    buttonContinue.setText("Продолжить");
                    handleRegisterErrorUi(message);
                });
            }
        });
    }

    private void confirmCode() {
        clearCodeErrors();

        String code = getTrimmed(inputCode);
        if (TextUtils.isEmpty(code)) {
            inputCode.setError("Введите код");
            return;
        }

        buttonConfirm.setEnabled(false);
        buttonResend.setEnabled(false);
        buttonConfirm.setText("Проверяем...");

        authRepository.confirmEmail(email, code, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    buttonConfirm.setText("Входим...");
                    autoLoginAfterConfirmation();
                });
            }

            @Override
            public void onError(@NonNull String message) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    buttonConfirm.setEnabled(true);
                    buttonResend.setEnabled(true);
                    buttonConfirm.setText("Подтвердить");
                    handleConfirmErrorUi(message);
                });
            }
        });
    }

    private void autoLoginAfterConfirmation() {
        authRepository.login(email, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    buttonConfirm.setEnabled(true);
                    buttonResend.setEnabled(true);
                    buttonConfirm.setText("Подтвердить");

                    Toast.makeText(getContext(), "Регистрация завершена", Toast.LENGTH_SHORT).show();

                    if (getActivity() instanceof LoginActivity) {
                        ((LoginActivity) getActivity()).openHome();
                    }

                    dismiss();
                });
            }

            @Override
            public void onError(@NonNull String message) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                    buttonConfirm.setEnabled(true);
                    buttonResend.setEnabled(true);
                    buttonConfirm.setText("Подтвердить");
                    handleAutoLoginErrorUi(message);
                });
            }
        });
    }

    private void resendCode() {
        authRepository.resendVerification(email, new AuthRepository.SimpleCallback() {
            @Override
            public void onSuccess() {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Код повторно отправлен", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onError(@NonNull String message) {
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> handleResendErrorUi(message));
            }
        });
    }

    private void clearRegisterErrors() {
        inputEmail.setError(null);
        inputPassword.setError(null);
        inputPhone.setError(null);
    }

    private void clearCodeErrors() {
        inputCode.setError(null);
    }

    private void handleRegisterErrorUi(@NonNull String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("сетев")) {
            Toast.makeText(getContext(), "Проблема с подключением: " + message, Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("user with this email already registered")
                || lower.contains("already registered")) {
            inputEmail.setError("Пользователь с таким email уже зарегистрирован");
            Toast.makeText(getContext(), "Такой email уже используется", Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("password") || lower.contains("парол")) {
            inputPassword.setError("Пароль не подходит: " + message);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void handleConfirmErrorUi(@NonNull String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("сетев")) {
            Toast.makeText(getContext(), "Проблема с подключением: " + message, Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("invalid verification code")
                || lower.contains("invalid code")
                || lower.contains("неверный код")) {
            inputCode.setError("Неверный код");
            Toast.makeText(getContext(), "Проверьте введённый код", Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("no active verification code")) {
            inputCode.setError("Срок действия кода истёк");
            Toast.makeText(
                    getContext(),
                    "Срок действия кода истёк, запросите новый код.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (lower.contains("maximum verification attempts exceeded")) {
            inputCode.setError("Превышено количество попыток");
            Toast.makeText(
                    getContext(),
                    "Превышено количество попыток. Запросите новый код или попробуйте позже.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (lower.contains("email_already_verified")) {
            Toast.makeText(getContext(), "Email уже подтверждён, можно входить", Toast.LENGTH_LONG).show();
            dismiss();
            return;
        }

        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void handleResendErrorUi(@NonNull String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("сетев")) {
            Toast.makeText(getContext(), "Проблема с подключением: " + message, Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("email already verified")
                || lower.contains("email_already_verified")) {
            Toast.makeText(getContext(), "Email уже подтверждён", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void handleAutoLoginErrorUi(@NonNull String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("сетев")) {
            Toast.makeText(
                    getContext(),
                    "Email подтверждён, но возникла проблема с подключением при входе: " + message,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if (lower.contains("incorrect login or password")) {
            Toast.makeText(
                    getContext(),
                    "Email подтверждён, но не удалось войти.\nПопробуйте войти вручную.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Toast.makeText(
                getContext(),
                "Email подтверждён, но не удалось автоматически войти: " + message,
                Toast.LENGTH_LONG
        ).show();
    }

    private String getTrimmed(TextInputEditText edit) {
        return edit.getText() != null ? edit.getText().toString().trim() : "";
    }

    private boolean isValidEmail(@NonNull String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPhone(@NonNull String phone) {
        String digits = phone.startsWith("+") ? phone.substring(1) : phone;
        if (digits.length() < PHONE_MIN_LENGTH || digits.length() > PHONE_MAX_LENGTH) {
            return false;
        }
        for (int i = 0; i < digits.length(); i++) {
            if (!Character.isDigit(digits.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
