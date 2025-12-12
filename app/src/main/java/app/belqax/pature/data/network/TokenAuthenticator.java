package app.belqax.pature.data.network;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import app.belqax.pature.data.storage.AuthStorage;
import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Route;
import okhttp3.Response;

/**
 * Authenticator:
 * - при 401 один раз пытается обновить access_token через /auth/refresh;
 * - если refresh недействителен (400/401 или нет токенов в ответе) — чистит AuthStorage;
 * - если проблема только с сетью или 5xx — не чистит сессию, просто не обновляет токен.
 */
public class TokenAuthenticator implements Authenticator {

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.get("application/json; charset=utf-8");

    private static final long NETWORK_TIMEOUT_SECONDS = 30L;

    private final String baseUrl;
    private final AuthStorage authStorage;
    private final OkHttpClient refreshClient;

    public TokenAuthenticator(String baseUrl, AuthStorage authStorage) {
        this.baseUrl = baseUrl;
        this.authStorage = authStorage;
        this.refreshClient = new OkHttpClient.Builder()
                .connectTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    @Nullable
    @Override
    public Request authenticate(@Nullable Route route, Response response) throws IOException {
        // Защищает от зацикливания одного и того же запроса.
        if (responseCount(response) >= 2) {
            return null;
        }

        String path = response.request().url().encodedPath();
        if (path != null) {
            // Не пытается рефрешить сами auth-эндпоинты.
            if (path.startsWith("/auth/login")
                    || path.startsWith("/auth/register")
                    || path.startsWith("/auth/refresh")
                    || path.startsWith("/auth/password")
                    || path.startsWith("/auth/email")) {
                return null;
            }
        }

        String refreshToken = authStorage.getRefreshToken();
        String login = authStorage.getLogin();

        // Если нет данных для refresh — выходим из сессии и не пытаемся обновляться.
        if (refreshToken == null || refreshToken.isEmpty()
                || login == null || login.isEmpty()) {
            authStorage.clearAll();
            return null;
        }

        // Собирает JSON для /auth/refresh.
        JsonObject json = new JsonObject();
        json.addProperty("refresh_token", refreshToken);
        json.addProperty("login", login);
        String bodyString = json.toString();

        RequestBody body = RequestBody.create(bodyString, JSON_MEDIA_TYPE);

        Request refreshRequest = new Request.Builder()
                .url(baseUrl + "auth/refresh")
                .post(body)
                .build();

        Response refreshResponse = null;
        try {
            refreshResponse = refreshClient.newCall(refreshRequest).execute();

            int code = refreshResponse.code();

            // Если /auth/refresh вернул 400/401 — refresh недействителен, чистит сессию.
            if ((code == 400 || code == 401)) {
                authStorage.clearAll();
                return null;
            }

            // Любой неуспешный код, кроме 400/401: не удалось обновить,
            // но это может быть временная проблема сервера, сессию не трогает.
            if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
                return null;
            }

            String raw = refreshResponse.body().string();
            JsonElement element = JsonParser.parseString(raw);
            if (!element.isJsonObject()) {
                // Ответ не похож на валидный JSON с токенами, считаем refresh сломанным.
                authStorage.clearAll();
                return null;
            }

            JsonObject root = element.getAsJsonObject();
            JsonElement accessEl = root.get("access_token");
            JsonElement refreshEl = root.get("refresh_token");
            if (accessEl == null || refreshEl == null
                    || accessEl.isJsonNull() || refreshEl.isJsonNull()) {
                // Нет токенов в ответе — refresh недействителен.
                authStorage.clearAll();
                return null;
            }

            String newAccess = accessEl.getAsString();
            String newRefresh = refreshEl.getAsString();

            if (newAccess == null || newAccess.isEmpty()
                    || newRefresh == null || newRefresh.isEmpty()) {
                authStorage.clearAll();
                return null;
            }

            // Успешно обновляет токены.
            authStorage.saveTokens(newAccess, newRefresh);

            // Пересобирает исходный запрос с новым access_token.
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newAccess)
                    .build();

        } catch (Exception e) {
            // Любая сетевая/парсинг-ошибка при обращении к /auth/refresh.
            // Сессию НЕ чистит: просто не обновляет токены.
            return null;
        } finally {
            if (refreshResponse != null && refreshResponse.body() != null) {
                refreshResponse.close();
            }
        }
    }

    private int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }
}
