package app.belqax.pature.data.network;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

import app.belqax.pature.data.storage.AuthStorage;
import okhttp3.Authenticator;
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

    private final AuthStorage authStorage;
    private final RefreshApi refreshApi;

    private final ReentrantLock refreshLock = new ReentrantLock(true);

    public TokenAuthenticator(
            @NonNull AuthStorage authStorage,
            @NonNull String baseUrl
    ) {
        this.authStorage = Objects.requireNonNull(authStorage, "authStorage");

        // Важно: refresh-клиент должен быть "чистым": без этого Authenticator, иначе можно закольцеваться.
        OkHttpClient refreshClient = new OkHttpClient.Builder()
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

        String requestAccess = extractBearerToken(response.request());
        String currentAccess = authStorage.getAccessToken();

        // 1) Если другой поток уже обновил access, просто повторяем запрос с актуальным токеном.
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

        // 2) Сериализация refresh: только один поток делает refresh, остальные ждут.
        refreshLock.lock();
        try {
            // Повторная проверка после ожидания lock: вдруг пока ждали, токен обновился.
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
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                Log.e(TAG, "authenticate: no refresh token, cannot refresh. url=" + response.request().url());
                return null;
            }

            Log.i(TAG, "authenticate: refreshing tokens, url=" + response.request().url());

            RefreshRequest body = new RefreshRequest(refreshToken);
            Call<TokenPair> call = refreshApi.refresh(body);

            retrofit2.Response<TokenPair> refreshResp;
            try {
                refreshResp = call.execute();
            } catch (IOException io) {
                // Сеть/таймауты не должны чистить сессию.
                Log.w(TAG, "authenticate: refresh network error: " + io);
                return null;
            } catch (Exception ex) {
                Log.e(TAG, "authenticate: refresh unexpected error: " + ex);
                return null;
            }

            if (refreshResp.isSuccessful() && refreshResp.body() != null) {
                TokenPair pair = refreshResp.body();
                if (pair.accessToken == null || pair.accessToken.trim().isEmpty()) {
                    Log.e(TAG, "authenticate: refresh success but access token empty");
                    return null;
                }

                // Сохраняем и access, и refresh (если сервер ротирует refresh — это критично).
                authStorage.saveTokens(pair.accessToken, pair.refreshToken);

                Log.i(TAG, "authenticate: refresh success, retrying original request");

                return response.request().newBuilder()
                        .header(AUTH_HEADER, BEARER_PREFIX + pair.accessToken)
                        .build();
            }

            // 401 на refresh означает недействительный refresh (или он уже был ротирован конкурентным запросом).
            // Мы уже сделали двойную проверку "токен обновился ли другим потоком", так что здесь можно логаутить.
            int code = refreshResp.code();
            if (code == 401) {
                Log.e(TAG, "authenticate: refresh rejected (code=401). Clearing session.");
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

    // ===== Retrofit API =====

    private interface RefreshApi {
        @POST("auth/refresh")
        Call<TokenPair> refresh(@Body RefreshRequest body);
    }

    private static final class RefreshRequest {
        final String refresh_token;

        RefreshRequest(@NonNull String refreshToken) {
            this.refresh_token = refreshToken;
        }
    }

    public static final class TokenPair {
        public String accessToken;
        public String refreshToken;
    }
}
