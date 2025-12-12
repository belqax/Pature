package app.belqax.pature.data.storage;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.UUID;

public class AuthStorage {

    private static final String PREFS_NAME = "pature_auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_LOGIN = "login";
    private static final String KEY_DEVICE_ID = "device_id";

    private final SharedPreferences prefs;

    public AuthStorage(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(String accessToken, String refreshToken) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public void saveLogin(String login) {
        prefs.edit()
                .putString(KEY_LOGIN, login)
                .apply();
    }

    public String getLogin() {
        return prefs.getString(KEY_LOGIN, null);
    }

    public void clearAll() {
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_LOGIN)
                .apply();
    }

    public String getOrCreateDeviceId() {
        String existing = prefs.getString(KEY_DEVICE_ID, null);
        if (!TextUtils.isEmpty(existing)) {
            return existing;
        }
        String newId = UUID.randomUUID().toString();
        prefs.edit()
                .putString(KEY_DEVICE_ID, newId)
                .apply();
        return newId;
    }
}