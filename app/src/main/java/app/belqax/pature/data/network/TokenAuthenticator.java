package app.belqax.pature.data.network;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import app.belqax.pature.data.storage.AuthStorage;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public final class TokenAuthenticator implements Authenticator {

    private static final String TAG = "TokenAuthenticator";

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final int MAX_AUTH_RETRIES = 2;

    private static final long NETWORK_TIMEOUT_SECONDS = 30L;
    private static final String APP_VERSION = "0.1.0-dev";

    private final AuthStorage authStorage;
    private final RefreshApi refreshApi;

    private final ReentrantLock refreshLock = new ReentrantLock(true);

    public TokenAuthenticator(
            @NonNull AuthStorage authStorage,
            @NonNull String baseUrl
    ) {
        this.authStorage = Objects.requireNonNull(authStorage, "authStorage");

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

        OkHttpClient refreshClient = new OkHttpClient.Builder()
                .addInterceptor(deviceHeadersInterceptor)
                .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Objects.requireNonNull(baseUrl, "baseUrl"))
                .client(refreshClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.refreshApi = retrofit.create(RefreshApi.class);
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) throws IOException {
        if (responseCount(response) >= MAX_AUTH_RETRIES) {
            Log.w(TAG, "authenticate: too many retries, giving up. url=" + response.request().url());
            return null;
        }

        String path = response.request().url().encodedPath();
        if (path != null && (path.contains("/auth/login") || path.contains("/auth/refresh") || path.contains("/auth/logout"))) {
            Log.w(TAG, "authenticate: 401 on auth endpoint, not retrying. url=" + response.request().url());
            return null;
        }

        String requestAccess = extractBearerToken(response.request());
        String currentAccess = authStorage.getAccessToken();

        if (currentAccess != null
                && !currentAccess.trim().isEmpty()
                && requestAccess != null
                && !requestAccess.equals(currentAccess)) {
            Log.i(TAG, "authenticate: token already refreshed by another call, retrying with current token. url="
                    + response.request().url());
            return response.request().newBuilder()
                    .header(AUTH_HEADER, BEARER_PREFIX + currentAccess)
                    .build();
        }

        refreshLock.lock();
        try {
            String accessAfterWait = authStorage.getAccessToken();
            if (accessAfterWait != null
                    && !accessAfterWait.trim().isEmpty()
                    && requestAccess != null
                    && !requestAccess.equals(accessAfterWait)) {
                Log.i(TAG, "authenticate: token refreshed while waiting lock, retrying. url=" + response.request().url());
                return response.request().newBuilder()
                        .header(AUTH_HEADER, BEARER_PREFIX + accessAfterWait)
                        .build();
            }

            String refreshToken = authStorage.getRefreshToken();
            String login = authStorage.getLogin();

            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                Log.e(TAG, "authenticate: no refresh token, cannot refresh. url=" + response.request().url());
                return null;
            }
            if (login == null || login.trim().isEmpty()) {
                Log.e(TAG, "authenticate: no login saved, cannot refresh. url=" + response.request().url());
                return null;
            }

            Log.i(TAG, "authenticate: refreshing tokens, url=" + response.request().url());

            RefreshRequest body = new RefreshRequest(refreshToken, login);

            retrofit2.Response<TokenPairDto> refreshResp;
            try {
                refreshResp = refreshApi.refresh(body).execute();
            } catch (IOException io) {
                Log.w(TAG, "authenticate: refresh network error: " + io);
                return null;
            } catch (Exception ex) {
                Log.e(TAG, "authenticate: refresh unexpected error: " + ex);
                return null;
            }

            if (refreshResp.isSuccessful() && refreshResp.body() != null) {
                TokenPairDto pair = refreshResp.body();

                if (pair.access_token == null || pair.access_token.trim().isEmpty()) {
                    Log.e(TAG, "authenticate: refresh success but access_token empty");
                    return null;
                }
                if (pair.refresh_token == null || pair.refresh_token.trim().isEmpty()) {
                    Log.e(TAG, "authenticate: refresh success but refresh_token empty");
                    return null;
                }

                authStorage.saveTokens(pair.access_token, pair.refresh_token);

                Log.i(TAG, "authenticate: refresh success, retrying original request");

                return response.request().newBuilder()
                        .header(AUTH_HEADER, BEARER_PREFIX + pair.access_token)
                        .build();
            }

            int code = refreshResp.code();
            if (code == 401) {
                Log.e(TAG, "authenticate: refresh rejected (401). Clearing session.");
                authStorage.clearAll();
                return null;
            }

            Log.e(TAG, "authenticate: refresh failed code=" + code);
            return null;

        } finally {
            refreshLock.unlock();
        }
    }

    private static int responseCount(@NonNull Response response) {
        int result = 1;
        Response prior = response.priorResponse();
        while (prior != null) {
            result++;
            prior = prior.priorResponse();
        }
        return result;
    }

    @Nullable
    private static String extractBearerToken(@NonNull Request request) {
        String h = request.header(AUTH_HEADER);
        if (h == null) {
            return null;
        }
        if (!h.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return h.substring(BEARER_PREFIX.length()).trim();
    }

    private interface RefreshApi {
        @POST("auth/refresh")
        Call<TokenPairDto> refresh(@Body RefreshRequest body);
    }

    private static final class RefreshRequest {
        @SerializedName("refresh_token")
        final String refresh_token;

        @SerializedName("login")
        final String login;

        RefreshRequest(@NonNull String refreshToken, @NonNull String login) {
            this.refresh_token = refreshToken;
            this.login = login;
        }
    }

    public static final class TokenPairDto {
        @SerializedName("access_token")
        public String access_token;

        @SerializedName("refresh_token")
        public String refresh_token;
    }
}
