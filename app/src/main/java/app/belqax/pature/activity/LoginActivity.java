package app.belqax.pature.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import app.belqax.pature.R;

public class LoginActivity extends AppCompatActivity {

    private boolean isLogin = true;


    private TextInputEditText emailField;
    private TextInputEditText passwordField;
    private MaterialButton actionButton;
    private TextView switchMode;
    private TextView footerLinks;
    ImageView googleIcon;
    ImageView telegramIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        emailField = findViewById(R.id.inputEmail);
        passwordField = findViewById(R.id.inputPassword);
        actionButton = findViewById(R.id.buttonAction);
        switchMode = findViewById(R.id.switchMode);
        footerLinks = findViewById(R.id.footerLinks);
        googleIcon = findViewById(R.id.googleIcon);
        telegramIcon = findViewById(R.id.telegramIcon);

        actionButton.setOnClickListener(v -> handleAction());
        switchMode.setOnClickListener(v -> toggleMode());
        googleIcon.setOnClickListener(v -> handleSocialLogin("google"));
        telegramIcon.setOnClickListener(v -> handleSocialLogin("telegram"));
        setFooterLinks(footerLinks);

    }

    private void handleAction() {
        String email = emailField.getText() != null ? emailField.getText().toString().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText().toString().trim() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Введите данные", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLogin) {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
            finish();

        } else {
            startActivity(new Intent(LoginActivity.this, OnboardingActivity.class));
        }
    }

    private void toggleMode() {
        isLogin = !isLogin;

        animateSwitch(actionButton);
        animateSwitch(switchMode);

        if (isLogin) {
            actionButton.setText("Войти");
            switchMode.setText("Нет аккаунта? Создать");
        } else {
            actionButton.setText("Создать аккаунт");
            switchMode.setText("Уже есть аккаунт? Войти");
        }
    }

    private void animateSwitch(View v) {
        // Загружаем анимацию пульсации
        Animation pulseOut = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);

        // Добавляем слушатель для анимации
        pulseOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // После завершения первой анимации меняем текст
                if (isLogin) {
                    actionButton.setText("Войти");
                    switchMode.setText("Нет аккаунта? Создать");
                } else {
                    actionButton.setText("Создать аккаунт");
                    switchMode.setText("Уже есть аккаунт? Войти");
                }

                // Запускаем обратную анимацию
                Animation pulseIn = AnimationUtils.loadAnimation(LoginActivity.this, R.anim.pulse_animation);
                v.startAnimation(pulseIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });

        v.startAnimation(pulseOut);
    }

    private void handleSocialLogin(String provider) {
        switch (provider) {
            case "google":
                Toast.makeText(this, "Авторизация через Google", Toast.LENGTH_SHORT).show();
                // Здесь добавить логику авторизации через Google
                break;
            case "telegram":
                Toast.makeText(this, "Авторизация через Telegram", Toast.LENGTH_SHORT).show();
                // Здесь добавить логику авторизации через Telegram
                break;
        }
    }

    private void setFooterLinks(TextView tv) {
        String fullText = "Политика • Поддержка";

        SpannableString ss = new SpannableString(fullText);

        // Первая ссылка: Политика
        ClickableSpan policyClick = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // ОТКРЫТЬ ссылку / экран политики
                // Например, открыть веб-страницу:
                // startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/policy")));

                Toast.makeText(LoginActivity.this, "Открыть Политику", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getColor(R.color.pature_secondary));
                ds.setUnderlineText(false);
            }
        };

        // Вторая ссылка: Поддержка
        ClickableSpan supportClick = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Открытие Telegram или email
                // startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/...")));

                Toast.makeText(LoginActivity.this, "Открыть Поддержку", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                ds.setColor(getColor(R.color.pature_secondary));
                ds.setUnderlineText(false);
            }
        };

        // Индексы
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


}
