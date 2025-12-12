package app.belqax.pature.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;

import app.belqax.pature.data.network.ApiClient;
import app.belqax.pature.data.storage.ProfileStorage;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;
import retrofit2.http.Part;

/**
 * Репозиторий для работы с /users/me, /users/me/profile, /users/me/avatar, /users/me/privacy.
 * Не знает о токенах и не создаёт Retrofit: использует готовый ApiClient.
 */
public class ProfileRepository {

    private static final String TAG = "ProfileRepository";

    private final ProfileApi api;
    private final ProfileStorage storage;

    public ProfileRepository(@NonNull ProfileApi api,
                             @NonNull ProfileStorage storage) {
        this.api = api;
        this.storage = storage;
    }

    public static ProfileRepository create(@NonNull Context context) {
        ApiClient apiClient = ApiClient.getInstance();
        Retrofit retrofit = apiClient.getRetrofit();
        ProfileApi api = retrofit.create(ProfileApi.class);
        ProfileStorage storage = new ProfileStorage(context);
        return new ProfileRepository(api, storage);
    }

    @Nullable
    public MeResponse getCachedProfile() {
        return storage.getProfile();
    }

    public void loadProfile(@NonNull final ProfileCallback callback) {
        Log.d(TAG, "loadProfile: sending GET /users/me request");
        api.getMe().enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(@NonNull Call<MeResponse> call,
                                   @NonNull Response<MeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "loadProfile: success, HTTP " + response.code());
                    MeResponse body = response.body();
                    storage.saveProfile(body);
                    callback.onSuccess(body);
                } else {
                    Log.w(TAG, "loadProfile: error, HTTP " + response.code());
                    callback.onError(mapHttpError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "loadProfile: failure", t);
                callback.onError(mapNetworkError(t));
            }
        });
    }

    public void updateProfile(@NonNull UpdateProfileRequest body,
                              @NonNull final ProfileCallback callback) {
        Log.d(TAG, "updateProfile: sending PUT /users/me/profile request");
        api.updateProfile(body).enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(@NonNull Call<MeResponse> call,
                                   @NonNull Response<MeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "updateProfile: success, HTTP " + response.code());
                    MeResponse body = response.body();
                    storage.saveProfile(body);
                    callback.onSuccess(body);
                } else {
                    Log.w(TAG, "updateProfile: error, HTTP " + response.code());
                    callback.onError(mapHttpError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "updateProfile: failure", t);
                callback.onError(mapNetworkError(t));
            }
        });
    }

    public void updatePrivacy(@NonNull UpdatePrivacyRequest body,
                              @NonNull final ProfileCallback callback) {
        Log.d(TAG, "updatePrivacy: sending PATCH /users/me/privacy request");
        api.updatePrivacy(body).enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(@NonNull Call<MeResponse> call,
                                   @NonNull Response<MeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "updatePrivacy: success, HTTP " + response.code());
                    MeResponse body = response.body();
                    storage.saveProfile(body);
                    callback.onSuccess(body);
                } else {
                    Log.w(TAG, "updatePrivacy: error, HTTP " + response.code());
                    callback.onError(mapHttpError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "updatePrivacy: failure", t);
                callback.onError(mapNetworkError(t));
            }
        });
    }

    public void updateSettings(@NonNull UpdateSettingsRequest body,
                               @NonNull final ProfileCallback callback) {
        Log.d(TAG, "updateSettings: sending PATCH /users/me/settings request");
        api.updateSettings(body).enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(@NonNull Call<MeResponse> call,
                                   @NonNull Response<MeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "updateSettings: success, HTTP " + response.code());
                    MeResponse body = response.body();
                    storage.saveProfile(body);
                    callback.onSuccess(body);
                } else {
                    Log.w(TAG, "updateSettings: error, HTTP " + response.code());
                    callback.onError(mapHttpError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MeResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "updateSettings: failure", t);
                callback.onError(mapNetworkError(t));
            }
        });
    }


    public void uploadAvatar(@NonNull MultipartBody.Part filePart,
                             @NonNull final ProfileCallback callback) {
        if (filePart.body() == null) {
            Log.w(TAG, "uploadAvatar: filePart has null body");
            callback.onError(new ProfileError(
                    0,
                    "Не удалось подготовить файл для загрузки.",
                    true,
                    false,
                    null,
                    null
            ));
            return;
        }

        Log.d(TAG, "uploadAvatar: sending POST /users/me/avatar request");
        api.uploadAvatar(filePart).enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(@NonNull Call<MeResponse> call,
                                   @NonNull Response<MeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "uploadAvatar: success, HTTP " + response.code());
                    MeResponse body = response.body();
                    storage.saveProfile(body);
                    callback.onSuccess(body);
                } else {
                    Log.w(TAG, "uploadAvatar: error, HTTP " + response.code());
                    callback.onError(mapHttpError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MeResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "uploadAvatar: failure", t);
                callback.onError(mapNetworkError(t));
            }
        });
    }

    public void deleteAvatar(@NonNull final ProfileCallback callback) {
        Log.d(TAG, "deleteAvatar: sending DELETE /users/me/avatar request");
        api.deleteAvatar().enqueue(new Callback<MeResponse>() {
            @Override
            public void onResponse(@NonNull Call<MeResponse> call,
                                   @NonNull Response<MeResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "deleteAvatar: success, HTTP " + response.code());
                    MeResponse body = response.body();
                    storage.saveProfile(body);
                    callback.onSuccess(body);
                } else {
                    Log.w(TAG, "deleteAvatar: error, HTTP " + response.code());
                    callback.onError(mapHttpError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<MeResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "deleteAvatar: failure", t);
                callback.onError(mapNetworkError(t));
            }
        });
    }

    public void clearProfile() {
        storage.clear();
    }

    private ProfileError mapHttpError(@NonNull Response<?> response) {
        int code = response.code();
        String bodyText = null;

        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                bodyText = errorBody.string();
            } catch (IOException e) {
                Log.w(TAG, "mapHttpError: failed to read errorBody", e);
            }
        }

        boolean isUnauthorized = (code == 401 || code == 403);

        String message;
        if (isUnauthorized) {
            message = "Сессия недействительна. Попробуйте войти снова.";
        } else if (code >= 500) {
            message = "Ошибка сервера. Попробуйте позже.";
        } else if (code == 422) {
            message = "Данные профиля не прошли валидацию.";
        } else {
            message = "Ошибка запроса (" + code + ")";
        }

        return new ProfileError(
                code,
                message,
                false,
                isUnauthorized,
                bodyText,
                null
        );
    }

    private ProfileError mapNetworkError(@NonNull Throwable t) {
        if (t instanceof HttpException) {
            HttpException httpException = (HttpException) t;
            int code = httpException.code();
            boolean isUnauthorized = (code == 401 || code == 403);
            return new ProfileError(
                    code,
                    "Ошибка HTTP " + code,
                    true,
                    isUnauthorized,
                    null,
                    t
            );
        }
        return new ProfileError(
                0,
                "Ошибка сети. Проверьте подключение к интернету.",
                true,
                false,
                null,
                t
        );
    }

    public interface ProfileCallback {
        void onSuccess(@NonNull MeResponse meResponse);
        void onError(@NonNull ProfileError error);
    }

    public static final class ProfileError {
        private final int httpCode;
        @NonNull
        private final String message;
        private final boolean networkError;
        private final boolean unauthorized;
        @Nullable
        private final String rawBody;
        @Nullable
        private final Throwable cause;

        public ProfileError(int httpCode,
                            @NonNull String message,
                            boolean networkError,
                            boolean unauthorized,
                            @Nullable String rawBody,
                            @Nullable Throwable cause) {
            this.httpCode = httpCode;
            this.message = message;
            this.networkError = networkError;
            this.unauthorized = unauthorized;
            this.rawBody = rawBody;
            this.cause = cause;
        }

        public int getHttpCode() {
            return httpCode;
        }

        @NonNull
        public String getMessage() {
            return message;
        }

        public boolean isNetworkError() {
            return networkError;
        }

        public boolean isUnauthorized() {
            return unauthorized;
        }

        @Nullable
        public String getRawBody() {
            return rawBody;
        }

        @Nullable
        public Throwable getCause() {
            return cause;
        }
    }

    public interface ProfileApi {

        @GET("users/me")
        Call<MeResponse> getMe();

        @PUT("users/me/profile")
        Call<MeResponse> updateProfile(@Body UpdateProfileRequest body);

        @PATCH("users/me/privacy")
        Call<MeResponse> updatePrivacy(@Body UpdatePrivacyRequest body);

        @PATCH("users/me/settings")
        Call<MeResponse> updateSettings(@Body UpdateSettingsRequest body);

        @Multipart
        @POST("users/me/avatar")
        Call<MeResponse> uploadAvatar(@Part MultipartBody.Part file);

        @DELETE("users/me/avatar")
        Call<MeResponse> deleteAvatar();
    }

    public static final class MeResponse {

        @Nullable
        @SerializedName("user")
        @Expose
        private UserDto user;

        @Nullable
        @SerializedName("profile")
        @Expose
        private ProfileDto profile;

        @Nullable
        @SerializedName("privacy")
        @Expose
        private PrivacyDto privacy;

        @Nullable
        @SerializedName("settings")
        @Expose
        private SettingsDto settings;

        @Nullable
        public UserDto getUser() {
            return user;
        }

        @Nullable
        public ProfileDto getProfile() {
            return profile;
        }

        @Nullable
        public PrivacyDto getPrivacy() {
            return privacy;
        }

        @Nullable
        public SettingsDto getSettings() {
            return settings;
        }
    }

    public static final class UserDto {

        @SerializedName("id")
        @Expose
        private int id;

        @Nullable
        @SerializedName("phone")
        @Expose
        private String phone;

        @Nullable
        @SerializedName("email")
        @Expose
        private String email;

        @SerializedName("is_active")
        @Expose
        private boolean is_active;

        public int getId() {
            return id;
        }

        @Nullable
        public String getPhone() {
            return phone;
        }

        @Nullable
        public String getEmail() {
            return email;
        }

        public boolean isActive() {
            return is_active;
        }
    }

    public static final class ProfileDto {

        @Nullable
        @SerializedName("display_name")
        @Expose
        private String display_name;

        @Nullable
        @SerializedName("age")
        @Expose
        private Integer age;

        @Nullable
        @SerializedName("about")
        @Expose
        private String about;

        @Nullable
        @SerializedName("location_formatted")
        @Expose
        private String location_formatted;

        @Nullable
        @SerializedName("location_city")
        @Expose
        private String location_city;

        @Nullable
        @SerializedName("location_state")
        @Expose
        private String location_state;

        @Nullable
        @SerializedName("location_country")
        @Expose
        private String location_country;

        @Nullable
        @SerializedName("location_postcode")
        @Expose
        private String location_postcode;

        @Nullable
        @SerializedName("location_lat")
        @Expose
        private Double location_lat;

        @Nullable
        @SerializedName("location_lon")
        @Expose
        private Double location_lon;

        @Nullable
        @SerializedName("location_result_type")
        @Expose
        private String location_result_type;

        @Nullable
        @SerializedName("location_confidence")
        @Expose
        private Double location_confidence;

        @Nullable
        @SerializedName("avatar_url")
        @Expose
        private String avatar_url;

        @Nullable
        public String getDisplayName() {
            return display_name;
        }

        @Nullable
        public Integer getAge() {
            return age;
        }

        @Nullable
        public String getAbout() {
            return about;
        }

        @Nullable
        public String getLocationFormatted() {
            return location_formatted;
        }

        @Nullable
        public String getLocationCity() {
            return location_city;
        }

        @Nullable
        public String getLocationState() {
            return location_state;
        }

        @Nullable
        public String getLocationCountry() {
            return location_country;
        }

        @Nullable
        public String getLocationPostcode() {
            return location_postcode;
        }

        @Nullable
        public Double getLocationLat() {
            return location_lat;
        }

        @Nullable
        public Double getLocationLon() {
            return location_lon;
        }

        @Nullable
        public String getLocationResultType() {
            return location_result_type;
        }

        @Nullable
        public Double getLocationConfidence() {
            return location_confidence;
        }

        @Nullable
        public String getAvatarUrl() {
            return avatar_url;
        }
    }

    public static final class PrivacyDto {

        @Nullable
        @SerializedName("profile_visibility")
        @Expose
        private String profile_visibility;

        @Nullable
        @SerializedName("photos_visibility")
        @Expose
        private String photos_visibility;

        @Nullable
        @SerializedName("online_status_visibility")
        @Expose
        private String online_status_visibility;

        @Nullable
        @SerializedName("last_seen_precision")
        @Expose
        private String last_seen_precision;

        @SerializedName("show_age")
        @Expose
        private boolean show_age;

        @SerializedName("show_distance")
        @Expose
        private boolean show_distance;

        @Nullable
        public String getProfileVisibility() {
            return profile_visibility;
        }

        @Nullable
        public String getPhotosVisibility() {
            return photos_visibility;
        }

        @Nullable
        public String getOnlineStatusVisibility() {
            return online_status_visibility;
        }

        @Nullable
        public String getLastSeenPrecision() {
            return last_seen_precision;
        }

        public boolean isShowAge() {
            return show_age;
        }

        public boolean isShowDistance() {
            return show_distance;
        }
    }

    public static final class SettingsDto {

        @Nullable
        @SerializedName("language_code")
        @Expose
        private String language_code;

        @Nullable
        @SerializedName("timezone")
        @Expose
        private String timezone;

        @SerializedName("biometric_login_enabled")
        @Expose
        private boolean biometric_login_enabled;

        @SerializedName("push_enabled")
        @Expose
        private boolean push_enabled;

        @SerializedName("push_new_messages")
        @Expose
        private boolean push_new_messages;

        @SerializedName("push_events")
        @Expose
        private boolean push_events;

        @SerializedName("push_news")
        @Expose
        private boolean push_news;

        @Nullable
        public String getLanguageCode() {
            return language_code;
        }

        @Nullable
        public String getTimezone() {
            return timezone;
        }

        public boolean isBiometricLoginEnabled() {
            return biometric_login_enabled;
        }

        public boolean isPushEnabled() {
            return push_enabled;
        }

        public boolean isPushNewMessages() {
            return push_new_messages;
        }

        public boolean isPushEvents() {
            return push_events;
        }

        public boolean isPushNews() {
            return push_news;
        }
    }

    public static final class UpdatePrivacyRequest {

        @Nullable
        @SerializedName("profile_visibility")
        @Expose
        private String profile_visibility;

        @Nullable
        @SerializedName("photos_visibility")
        @Expose
        private String photos_visibility;

        @Nullable
        @SerializedName("online_status_visibility")
        @Expose
        private String online_status_visibility;

        @Nullable
        @SerializedName("last_seen_precision")
        @Expose
        private String last_seen_precision;

        @Nullable
        @SerializedName("show_age")
        @Expose
        private Boolean show_age;

        @Nullable
        @SerializedName("show_distance")
        @Expose
        private Boolean show_distance;

        public UpdatePrivacyRequest(
                @Nullable String profileVisibility,
                @Nullable String photosVisibility,
                @Nullable String onlineStatusVisibility,
                @Nullable String lastSeenPrecision,
                @Nullable Boolean showAge,
                @Nullable Boolean showDistance
        ) {
            this.profile_visibility = profileVisibility;
            this.photos_visibility = photosVisibility;
            this.online_status_visibility = onlineStatusVisibility;
            this.last_seen_precision = lastSeenPrecision;
            this.show_age = showAge;
            this.show_distance = showDistance;
        }

        @Nullable
        public String getProfileVisibility() {
            return profile_visibility;
        }

        @Nullable
        public String getPhotosVisibility() {
            return photos_visibility;
        }

        @Nullable
        public String getOnlineStatusVisibility() {
            return online_status_visibility;
        }

        @Nullable
        public String getLastSeenPrecision() {
            return last_seen_precision;
        }

        @Nullable
        public Boolean getShowAge() {
            return show_age;
        }

        @Nullable
        public Boolean getShowDistance() {
            return show_distance;
        }
    }
    public static final class UpdateSettingsRequest {

        @Nullable
        @SerializedName("language_code")
        @Expose
        private String language_code;

        @Nullable
        @SerializedName("timezone")
        @Expose
        private String timezone;

        @Nullable
        @SerializedName("biometric_login_enabled")
        @Expose
        private Boolean biometric_login_enabled;

        @Nullable
        @SerializedName("push_enabled")
        @Expose
        private Boolean push_enabled;

        @Nullable
        @SerializedName("push_new_messages")
        @Expose
        private Boolean push_new_messages;

        @Nullable
        @SerializedName("push_events")
        @Expose
        private Boolean push_events;

        @Nullable
        @SerializedName("push_news")
        @Expose
        private Boolean push_news;

        public UpdateSettingsRequest(
                @Nullable String languageCode,
                @Nullable String timezone,
                @Nullable Boolean biometricLoginEnabled,
                @Nullable Boolean pushEnabled,
                @Nullable Boolean pushNewMessages,
                @Nullable Boolean pushEvents,
                @Nullable Boolean pushNews
        ) {
            this.language_code = languageCode;
            this.timezone = timezone;
            this.biometric_login_enabled = biometricLoginEnabled;
            this.push_enabled = pushEnabled;
            this.push_new_messages = pushNewMessages;
            this.push_events = pushEvents;
            this.push_news = pushNews;
        }

        @Nullable public String getLanguageCode() { return language_code; }
        @Nullable public String getTimezone() { return timezone; }
        @Nullable public Boolean getBiometricLoginEnabled() { return biometric_login_enabled; }
        @Nullable public Boolean getPushEnabled() { return push_enabled; }
        @Nullable public Boolean getPushNewMessages() { return push_new_messages; }
        @Nullable public Boolean getPushEvents() { return push_events; }
        @Nullable public Boolean getPushNews() { return push_news; }
    }

    public static final class UpdateProfileRequest {

        @Nullable
        @SerializedName("display_name")
        @Expose
        private String display_name;

        @Nullable
        @SerializedName("age")
        @Expose
        private Integer age;

        @Nullable
        @SerializedName("about")
        @Expose
        private String about;

        @Nullable
        @SerializedName("location_formatted")
        @Expose
        private String location_formatted;

        @Nullable
        @SerializedName("location_city")
        @Expose
        private String location_city;

        @Nullable
        @SerializedName("location_state")
        @Expose
        private String location_state;

        @Nullable
        @SerializedName("location_country")
        @Expose
        private String location_country;

        @Nullable
        @SerializedName("location_postcode")
        @Expose
        private String location_postcode;

        @Nullable
        @SerializedName("location_lat")
        @Expose
        private Double location_lat;

        @Nullable
        @SerializedName("location_lon")
        @Expose
        private Double location_lon;

        @Nullable
        @SerializedName("location_result_type")
        @Expose
        private String location_result_type;

        @Nullable
        @SerializedName("location_confidence")
        @Expose
        private Double location_confidence;

        public UpdateProfileRequest(
                @Nullable String displayName,
                @Nullable Integer age,
                @Nullable String about,
                @Nullable String locationFormatted,
                @Nullable String locationCity,
                @Nullable String locationState,
                @Nullable String locationCountry,
                @Nullable String locationPostcode,
                @Nullable Double locationLat,
                @Nullable Double locationLon,
                @Nullable String locationResultType,
                @Nullable Double locationConfidence
        ) {
            this.display_name = displayName;
            this.age = age;
            this.about = about;

            this.location_formatted = locationFormatted;
            this.location_city = locationCity;
            this.location_state = locationState;
            this.location_country = locationCountry;
            this.location_postcode = locationPostcode;

            this.location_lat = locationLat;
            this.location_lon = locationLon;

            this.location_result_type = locationResultType;
            this.location_confidence = locationConfidence;
        }

        @Nullable
        public String getDisplayName() {
            return display_name;
        }

        @Nullable
        public Integer getAge() {
            return age;
        }

        @Nullable
        public String getAbout() {
            return about;
        }

        @Nullable
        public String getLocationCity() {
            return location_city;
        }

        @Nullable
        public String getLocationState() {
            return location_state;
        }
    }

}
