package app.belqax.pature.data.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.SecureRandom;
import java.util.Locale;

public final class AuthStorage {

    private static final String TAG = "AuthStorage";

    private static final String PREFS_NAME = "pature_auth_storage_v1";

    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_LOGIN = "login";
    private static final String KEY_DEVICE_ID = "device_id";

    private final SharedPreferences prefs;

    public AuthStorage(@NonNull Context context) {
        this.prefs = createBestPrefs(context.getApplicationContext());
    }

    @Nullable
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    @Nullable
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    @Nullable
    public String getLogin() {
        return prefs.getString(KEY_LOGIN, null);
    }

    public void saveLogin(@NonNull String login) {
        boolean ok = prefs.edit().putString(KEY_LOGIN, login).commit();
        if (!ok) {
            Log.e(TAG, "saveLogin: commit failed");
        }
    }

    public void saveTokens(@NonNull String accessToken, @NonNull String refreshToken) {
        boolean ok = prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .commit();

        if (!ok) {
            Log.e(TAG, "saveTokens: commit failed");
            return;
        }

        String a = prefs.getString(KEY_ACCESS_TOKEN, null);
        String r = prefs.getString(KEY_REFRESH_TOKEN, null);
        if (a == null || r == null || !a.equals(accessToken) || !r.equals(refreshToken)) {
            Log.e(TAG, "saveTokens: verification read-back mismatch, storage may be broken");
        }
    }

    public void clearAll() {
        boolean ok = prefs.edit().clear().commit();
        if (!ok) {
            Log.e(TAG, "clearAll: commit failed");
        }
    }

    @NonNull
    public String getOrCreateDeviceId() {
        String existing = prefs.getString(KEY_DEVICE_ID, null);
        if (existing != null && !existing.trim().isEmpty()) {
            return existing;
        }

        String created = generateDeviceId();
        boolean ok = prefs.edit().putString(KEY_DEVICE_ID, created).commit();
        if (!ok) {
            Log.e(TAG, "getOrCreateDeviceId: commit failed, returning volatile id");
            return created;
        }
        return created;
    }

    @NonNull
    private static String generateDeviceId() {
        SecureRandom rnd = new SecureRandom();
        byte[] b = new byte[16];
        rnd.nextBytes(b);
        StringBuilder sb = new StringBuilder(32);
        for (byte value : b) {
            sb.append(String.format(Locale.US, "%02x", value));
        }
        return sb.toString();
    }

    @NonNull
    private static SharedPreferences createBestPrefs(@NonNull Context context) {
        try {
            // Используется reflection, чтобы проект компилился даже если зависимости security-crypto нет.
            // При наличии зависимости будет использовано шифрованное хранилище.
            Class<?> espClass = Class.forName("androidx.security.crypto.EncryptedSharedPreferences");
            Class<?> mkClass = Class.forName("androidx.security.crypto.MasterKey");

            Object masterKeyBuilder = mkClass.getDeclaredClasses()[0]
                    .getConstructor(Context.class)
                    .newInstance(context);

            Object keyScheme = mkClass.getDeclaredClasses()[1]
                    .getField("AES256_GCM")
                    .get(null);

            Object masterKey = masterKeyBuilder.getClass()
                    .getMethod("setKeyScheme", keyScheme.getClass())
                    .invoke(masterKeyBuilder, keyScheme);

            Object builtMasterKey = masterKey.getClass().getMethod("build").invoke(masterKey);

            Object prefKeyEncScheme = espClass.getDeclaredClasses()[0]
                    .getField("AES256_SIV")
                    .get(null);

            Object prefValueEncScheme = espClass.getDeclaredClasses()[1]
                    .getField("AES256_GCM")
                    .get(null);

            Object encryptedPrefs = espClass.getMethod(
                            "create",
                            Context.class,
                            String.class,
                            mkClass,
                            prefKeyEncScheme.getClass(),
                            prefValueEncScheme.getClass()
                    )
                    .invoke(
                            null,
                            context,
                            PREFS_NAME,
                            builtMasterKey,
                            prefKeyEncScheme,
                            prefValueEncScheme
                    );

            if (encryptedPrefs instanceof SharedPreferences) {
                Log.i(TAG, "createBestPrefs: EncryptedSharedPreferences enabled");
                return (SharedPreferences) encryptedPrefs;
            }
        } catch (Throwable t) {
            Log.w(TAG, "createBestPrefs: encrypted prefs unavailable, fallback to normal prefs. " + t);
        }

        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
