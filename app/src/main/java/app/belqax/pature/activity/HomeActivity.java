package app.belqax.pature.activity;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import app.belqax.pature.R;
import app.belqax.pature.fragment.ChatsFragment;
import app.belqax.pature.fragment.EventsFragment;
import app.belqax.pature.fragment.HomeFragment;
import app.belqax.pature.fragment.ProfileFragment;
import app.belqax.pature.fragment.SearchFragment;

public class HomeActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_ITEM = "home_selected_nav_item";

    private static final String TAG_HOME = "tab_home";
    private static final String TAG_SEARCH = "tab_search";
    private static final String TAG_EVENT = "tab_event";
    private static final String TAG_FAVORITES = "tab_favorites";
    private static final String TAG_PROFILE = "tab_profile";

    private BottomNavigationView bottomNav;

    @Nullable
    private Fragment homeFragment;
    @Nullable
    private Fragment searchFragment;
    @Nullable
    private Fragment eventFragment;
    @Nullable
    private Fragment favoritesFragment;
    @Nullable
    private Fragment profileFragment;

    @Nullable
    private Fragment activeFragment;

    @IdRes
    private int selectedItemId = R.id.nav_home;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState != null) {
            selectedItemId = savedInstanceState.getInt(KEY_SELECTED_ITEM, R.id.nav_home);
        } else {
            selectedItemId = R.id.nav_home;
        }

        setupFragments(selectedItemId);

        bottomNav.setOnItemSelectedListener(item -> {
            int newItemId = item.getItemId();
            if (newItemId == selectedItemId) {
                return true;
            }
            switchToTab(newItemId);
            selectedItemId = newItemId;
            return true;
        });

        bottomNav.setSelectedItemId(selectedItemId);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_ITEM, selectedItemId);
    }

    private void setupFragments(@IdRes int initialItemId) {
        FragmentManager fm = getSupportFragmentManager();

        homeFragment = fm.findFragmentByTag(TAG_HOME);
        searchFragment = fm.findFragmentByTag(TAG_SEARCH);
        eventFragment = fm.findFragmentByTag(TAG_EVENT);
        favoritesFragment = fm.findFragmentByTag(TAG_FAVORITES);
        profileFragment = fm.findFragmentByTag(TAG_PROFILE);

        FragmentTransaction tx = fm.beginTransaction();

        if (homeFragment == null) {
            homeFragment = new HomeFragment();
            tx.add(R.id.fragmentContainer, homeFragment, TAG_HOME);
        }

        if (searchFragment == null) {
            // SearchFragment ты уже создал
            searchFragment = new SearchFragment();
            tx.add(R.id.fragmentContainer, searchFragment, TAG_SEARCH);
        }

        if (eventFragment == null) {
            // Временно заглушка; потом заменишь на реальный фрагмент избранного
            eventFragment = new EventsFragment();
            tx.add(R.id.fragmentContainer, eventFragment, TAG_EVENT);
        }

        if (favoritesFragment == null) {
            // Временно заглушка; потом заменишь на реальный фрагмент избранного
            favoritesFragment = new ChatsFragment();
            tx.add(R.id.fragmentContainer, favoritesFragment, TAG_FAVORITES);
        }

        if (profileFragment == null) {
            // Тоже заглушка; позже заменишь на ProfileFragment
            profileFragment = new ProfileFragment();
            tx.add(R.id.fragmentContainer, profileFragment, TAG_PROFILE);
        }

        activeFragment = getFragmentForNavId(initialItemId);
        if (activeFragment == null) {
            activeFragment = homeFragment;
        }

        if (homeFragment != null && homeFragment != activeFragment) {
            tx.hide(homeFragment);
        }
        if (searchFragment != null && searchFragment != activeFragment) {
            tx.hide(searchFragment);
        }
        if (eventFragment != null && eventFragment != activeFragment) {
            tx.hide(eventFragment);
        }
        if (favoritesFragment != null && favoritesFragment != activeFragment) {
            tx.hide(favoritesFragment);
        }
        if (profileFragment != null && profileFragment != activeFragment) {
            tx.hide(profileFragment);
        }

        if (activeFragment != null) {
            tx.show(activeFragment);
        }

        tx.commitNow();
    }

    private void switchToTab(@IdRes int newItemId) {
        Fragment target = getFragmentForNavId(newItemId);
        if (target == null || target == activeFragment) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();

        boolean forward = isForward(selectedItemId, newItemId);

        if (forward) {
            tx.setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
            );
        } else {
            tx.setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right,
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
            );
        }

        if (activeFragment != null) {
            tx.hide(activeFragment);
        }
        tx.show(target);

        tx.commit();
        activeFragment = target;
    }

    @Nullable
    private Fragment getFragmentForNavId(@IdRes int itemId) {
        if (itemId == R.id.nav_home) {
            return homeFragment;
        } else if (itemId == R.id.nav_search) {
            return searchFragment;
        } else if (itemId == R.id.nav_event) {
            return eventFragment;
        } else if (itemId == R.id.nav_message) {
            return favoritesFragment;
        } else if (itemId == R.id.nav_profile) {
            return profileFragment;
        } else {
            return null;
        }
    }

    private boolean isForward(@IdRes int oldId, @IdRes int newId) {
        return getPositionForNavId(newId) > getPositionForNavId(oldId);
    }

    private int getPositionForNavId(@IdRes int itemId) {
        if (itemId == R.id.nav_home) {
            return 0;
        }
        if (itemId == R.id.nav_search) {
            return 1;
        }
        if (itemId == R.id.nav_event) {
            return 2;
        }
        if (itemId == R.id.nav_message) {
            return 3;
        }
        if (itemId == R.id.nav_profile) {
            return 4;
        }
        return 0;
    }
}
