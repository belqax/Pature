package app.belqax.pature.data.network;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

import app.belqax.pature.data.storage.AuthStorage;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Общий Retrofit-клиент для всего приложения.
 * Содержит:
 * - заголовки устройства;
 * - Authorization: Bearer ...;
 * - TokenAuthenticator с автоматическим refresh при 401.
 */
public final class ApiClient {

    public static final String API_BASE_URL = "https://api.belqax.xyz/";
    private static final long NETWORK_TIMEOUT_SECONDS = 30L;
    private static final String APP_VERSION = "0.1.0-dev";

    private static ApiClient instance;

    private final Retrofit retrofit;
    private final AuthApi authApi;

    private ApiClient(@NonNull Context context, @NonNull AuthStorage authStorage) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // Технические заголовки устройства/приложения
        Interceptor deviceHeadersInterceptor = chain -> {
            Request original = chain.request();

            String deviceId = authStorage.getOrCreateDeviceId();
            String platform = "android";
            String deviceModel = Build.MANUFACTURER + " " + Build.MODEL;
            String osVersion = Build.VERSION.RELEASE;

            Request request = original.newBuilder()
                    .header("X-Device-Id", deviceId)
                    .header("X-Platform", platform)
                    .header("X-Device-Model", deviceModel)
                    .header("X-OS-Version", osVersion)
                    .header("X-App-Version", APP_VERSION)
                    .build();

            return chain.proceed(request);
        };

        // Authorization: Bearer ...
        Interceptor authHeaderInterceptor = chain -> {
            Request original = chain.request();

            String accessToken = authStorage.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                return chain.proceed(original);
            }

            Request request = original.newBuilder()
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            return chain.proceed(request);
        };

        TokenAuthenticator tokenAuthenticator =
                new TokenAuthenticator(API_BASE_URL, authStorage);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(deviceHeadersInterceptor)
                .addInterceptor(authHeaderInterceptor)
                .authenticator(tokenAuthenticator)
                .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        authApi = retrofit.create(AuthApi.class);
    }

    public static void init(@NonNull Context context, @NonNull AuthStorage authStorage) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext(), authStorage);
        }
    }

    @NonNull
    public static ApiClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ApiClient.init() must be called in Application.onCreate()");
        }
        return instance;
    }

    @NonNull
    public Retrofit getRetrofit() {
        return retrofit;
    }

    @NonNull
    public AuthApi getAuthApi() {
        return authApi;
    }
}
