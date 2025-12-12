package app.belqax.pature.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

import app.belqax.pature.R;
import app.belqax.pature.data.repository.AuthRepository;
import app.belqax.pature.data.storage.AuthStorage;
import app.belqax.pature.ui.ForgotPasswordBottomSheet;
import app.belqax.pature.ui.RegisterBottomSheet;

public class LoginActivity extends AppCompatActivity {

    private static final int PHONE_MIN_LENGTH = 10;
    private static final int PHONE_MAX_LENGTH = 15;

    private TextInputEditText inputLogin;
    private TextInputEditText inputPassword;
    private MaterialButton buttonLogin;
    private TextView textRegister;
    private TextView textForgotPassword;
    private TextView footerLinks;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        AuthStorage authStorage = new AuthStorage(this);
        authRepository = new AuthRepository(authStorage);

        inputLogin = findViewById(R.id.inputLogin);
        inputPassword = findViewById(R.id.inputPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        textRegister = findViewById(R.id.textRegister);
        textForgotPassword = findViewById(R.id.textForgotPassword);
        footerLinks = findViewById(R.id.footerLinks);

        buttonLogin.setOnClickListener(v -> performLogin());
        textRegister.setOnClickListener(v -> openRegisterSheet());
        textForgotPassword.setOnClickListener(v -> openForgotPasswordSheet());

        if (footerLinks != null) {
            setFooterLinks(footerLinks);
        }
    }

    private void performLogin() {
        clearFieldErrors();

        String login = inputLogin.getText() != null ? inputLogin.getText().toString().trim() : "";
        String password = inputPassword.getText() != null ? inputPassword.getText().toString() : "";

        if (TextUtils.isEmpty(login)) {
            inputLogin.setError("Введите e-mail или телефон");
            return;
        }

        // Определяет тип логина и валидирует, если это e-mail или телефон.
        if (login.contains("@")) {
            if (!isValidEmail(login)) {
                inputLogin.setError("Некорректный e-mail");
                return;
            }
        } else if (isDigitsOrPlus(login)) {
            if (!isValidPhone(login)) {
                inputLogin.setError("Некорректный номер телефона");
                return;
            }
        } // иначе считаем, что это username и не валидируем формат

        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("Введите пароль");
            return;
        }

        buttonLogin.setEnabled(false);
        buttonLogin.setText("Входим...");

        authRepository.login(login, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    buttonLogin.setEnabled(true);
                    buttonLogin.setText("Войти");
                    Toast.makeText(LoginActivity.this, "Добро пожаловать!", Toast.LENGTH_SHORT).show();
                    openHome();
                });
            }

            @Override
            public void onError(@NonNull String message) {
                runOnUiThread(() -> {
                    buttonLogin.setEnabled(true);
                    buttonLogin.setText("Войти");
                    handleLoginErrorUi(message);
                });
            }
        });
    }

    private void clearFieldErrors() {
        inputLogin.setError(null);
        inputPassword.setError(null);
    }

    private void handleLoginErrorUi(@NonNull String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        boolean isNetwork = lower.contains("сетев");
        if (isNetwork) {
            Toast.makeText(this, "Проблема с подключением: " + message, Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("incorrect login or password")
                || lower.contains("неверный логин или пароль")) {
            inputLogin.setError("Неверный логин или пароль");
            inputPassword.setError("Неверный логин или пароль");
            Toast.makeText(this, "Проверьте логин и пароль", Toast.LENGTH_LONG).show();
            return;
        }

        if (lower.contains("email is not verified")
                || lower.contains("email не подтвержден")
                || lower.contains("email_not_verified")) {
            inputLogin.setError("Email не подтвержден");
            Toast.makeText(
                    this,
                    "Email не подтвержден. Проверьте почту или запросите новый код в регистрации.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void openRegisterSheet() {
        new RegisterBottomSheet(authRepository)
                .show(getSupportFragmentManager(), "register");
    }

    private void openForgotPasswordSheet() {
        new ForgotPasswordBottomSheet(authRepository)
                .show(getSupportFragmentManager(), "forgot");
    }

    public void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setFooterLinks(TextView tv) {
        String fullText = "Политика • Поддержка";
        SpannableString ss = new SpannableString(fullText);

        ClickableSpan policyClick = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Toast.makeText(LoginActivity.this, "Открыть Политику", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(getColor(R.color.pature_secondary));
                ds.setUnderlineText(false);
            }
        };

        ClickableSpan supportClick = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Toast.makeText(LoginActivity.this, "Открыть Поддержку", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(getColor(R.color.pature_secondary));
                ds.setUnderlineText(false);
            }
        };

        int policyStart = 0;
        int policyEnd = "Политика".length();
        int supportStart = fullText.indexOf("Поддержка");
        int supportEnd = supportStart + "Поддержка".length();

        ss.setSpan(policyClick, policyStart, policyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(supportClick, supportStart, supportEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tv.setText(ss);
        tv.setMovementMethod(LinkMovementMethod.getInstance());
        tv.setHighlightColor(Color.TRANSPARENT);
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

    private boolean isDigitsOrPlus(@NonNull String value) {
        if (value.isEmpty()) {
            return false;
        }
        int start = 0;
        if (value.charAt(0) == '+') {
            if (value.length() == 1) {
                return false;
            }
            start = 1;
        }
        for (int i = start; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
