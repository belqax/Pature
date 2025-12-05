package app.belqax.pature.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import app.belqax.pature.R;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        @SuppressLint("MissingInflatedId") BottomNavigationView nav = findViewById(R.id.bottomNav);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // TODO: загрузить фрагмент Home
            } else if (id == R.id.nav_search) {
                // TODO: Search
            } else if (id == R.id.nav_pets) {
                // TODO: Pets
            } else if (id == R.id.nav_profile) {
                // TODO: Profile
            }

            return true;
        });
    }
}
