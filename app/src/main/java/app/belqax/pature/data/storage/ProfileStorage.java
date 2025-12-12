package app.belqax.pature.data.storage;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import app.belqax.pature.data.repository.ProfileRepository;

/**
 * Хранит последний успешный ответ /me.
 * Позволяет быстро показать профиль из кеша до загрузки с сервера.
 */
public class ProfileStorage {

    private static final String PREFS_NAME = "profile_storage_prefs";
    private static final String KEY_ME_RESPONSE = "key_me_response";

    private final SharedPreferences preferences;
    private final Gson gson;

    public ProfileStorage(@NonNull Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public void saveProfile(@NonNull ProfileRepository.MeResponse meResponse) {
        String json = gson.toJson(meResponse, new TypeToken<ProfileRepository.MeResponse>() {}.getType());
        preferences.edit()
                .putString(KEY_ME_RESPONSE, json)
                .apply();
    }

    @Nullable
    public ProfileRepository.MeResponse getProfile() {
        String json = preferences.getString(KEY_ME_RESPONSE, null);
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return gson.fromJson(json, new TypeToken<ProfileRepository.MeResponse>() {}.getType());
        } catch (JsonSyntaxException e) {
            // Если формат сломан, просто очищает запись.
            preferences.edit().remove(KEY_ME_RESPONSE).apply();
            return null;
        }
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}

