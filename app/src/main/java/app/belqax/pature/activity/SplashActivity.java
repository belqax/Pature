package app.belqax.pature.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import app.belqax.pature.R;
import app.belqax.pature.data.repository.AuthRepository;
import app.belqax.pature.data.storage.AuthStorage;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1000L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        AuthStorage authStorage = new AuthStorage(this);
        AuthRepository authRepository = new AuthRepository(authStorage);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (authRepository.isLoggedIn()) {
                openHome();
            } else {
                openLogin();
            }
        }, SPLASH_DELAY_MS);
    }

    private void openHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
