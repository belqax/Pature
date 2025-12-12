package app.belqax.pature.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import app.belqax.pature.data.network.ApiClient;
import app.belqax.pature.data.network.AuthApi;
import app.belqax.pature.data.storage.AuthStorage;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Слой бизнес-логики авторизации поверх общего сетевого клиента.
 *
 * Ничего не знает про OkHttp/Retrofit-конфигурацию:
 * - не создаёт OkHttpClient;
 * - не создаёт Retrofit;
 * - не содержит TokenAuthenticator.
 *
 * Всё это уже должно быть реализовано в общем сетевом слое (например ApiClient + TokenAuthenticator).
 */
public class AuthRepository {

    private final AuthStorage authStorage;
    private final AuthApi authApi;

    public interface LoginCallback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    public interface SimpleCallback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    public interface RefreshCallback {
        void onSuccess();

        void onError(@NonNull String message);
    }

    public AuthRepository(@NonNull AuthStorage authStorage) {
        this.authStorage = authStorage;
        // Предполагается, что ApiClient уже инициализирован в Application.onCreate()
        this.authApi = ApiClient.getInstance().getAuthApi();
    }

    /**
     * Проверяет, есть ли сохранённые access/refresh токены.
     */
    public boolean isLoggedIn() {
        String access = authStorage.getAccessToken();
        String refresh = authStorage.getRefreshToken();
        return access != null && !access.isEmpty()
                && refresh != null && !refresh.isEmpty();
    }

    /**
     * Логин по login/email/phone + паролю.
     * При успехе сохраняет токены и login.
     */
    public void login(
            @NonNull String login,
            @NonNull String password,
            @NonNull LoginCallback callback
    ) {
        UserLoginRequestDto body = new UserLoginRequestDto(login, password);

        authApi.login(body).enqueue(new Callback<TokenPairDto>() {
            @Override
            public void onResponse(
                    @NonNull Call<TokenPairDto> call,
                    @NonNull Response<TokenPairDto> response
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    String message = extractErrorMessage(response, "Ошибка авторизации: " + response.code());
                    callback.onError(message);
                    return;
                }

                TokenPairDto tokens = response.body();
                authStorage.saveTokens(tokens.access_token, tokens.refresh_token);
                authStorage.saveLogin(login);

                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<TokenPairDto> call, @NonNull Throwable t) {
                callback.onError("Сетевая ошибка при авторизации: " + safeMessage(t));
            }
        });
    }

    /**
     * Регистрация: создаёт пользователя и отправляет код подтверждения на email.
     */
    public void register(
            @NonNull String email,
            @NonNull String password,
            @Nullable String phone,
            @NonNull SimpleCallback callback
    ) {
        EmailRegisterRequestDto body = new EmailRegisterRequestDto(email, password, phone);

        authApi.register(body).enqueue(new Callback<SimpleDetailResponseDto>() {
            @Override
            public void onResponse(
                    @NonNull Call<SimpleDetailResponseDto> call,
                    @NonNull Response<SimpleDetailResponseDto> response
            ) {
                if (!response.isSuccessful()) {
                    String message = extractErrorMessage(response, "Ошибка регистрации: " + response.code());
                    callback.onError(message);
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<SimpleDetailResponseDto> call, @NonNull Throwable t) {
                callback.onError("Сетевая ошибка при регистрации: " + safeMessage(t));
            }
        });
    }

    /**
     * Подтверждает email кодом из письма.
     */
    public void confirmEmail(
            @NonNull String email,
            @NonNull String code,
            @NonNull SimpleCallback callback
    ) {
        EmailVerificationConfirmRequestDto body = new EmailVerificationConfirmRequestDto(email, code);

        authApi.confirmEmail(body).enqueue(new Callback<SimpleDetailResponseDto>() {
            @Override
            public void onResponse(
                    @NonNull Call<SimpleDetailResponseDto> call,
                    @NonNull Response<SimpleDetailResponseDto> response
            ) {
                if (!response.isSuccessful()) {
                    String message = extractErrorMessage(response, "Ошибка подтверждения email: " + response.code());
                    callback.onError(message);
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<SimpleDetailResponseDto> call, @NonNull Throwable t) {
                callback.onError("Сетевая ошибка при подтверждении email: " + safeMessage(t));
            }
        });
    }

    /**
     * Повторно отправляет код подтверждения email.
     */
    public void resendVerification(
            @NonNull String email,
            @NonNull SimpleCallback callback
    ) {
        ResendVerificationEmailRequestDto body = new ResendVerificationEmailRequestDto(email);

        authApi.resendVerificationEmail(body).enqueue(new Callback<SimpleDetailResponseDto>() {
            @Override
            public void onResponse(
                    @NonNull Call<SimpleDetailResponseDto> call,
                    @NonNull Response<SimpleDetailResponseDto> response
            ) {
                if (!response.isSuccessful()) {
                    String message = extractErrorMessage(response, "Ошибка при повторной отправке кода: " + response.code());
                    callback.onError(message);
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<SimpleDetailResponseDto> call, @NonNull Throwable t) {
                callback.onError("Сетевая ошибка при повторной отправке кода: " + safeMessage(t));
            }
        });
    }

    /**
     * Запускает процесс восстановления пароля (отправляет код на email).
     */
    public void forgotPassword(
            @NonNull String email,
            @NonNull SimpleCallback callback
    ) {
        PasswordForgotRequestDto body = new PasswordForgotRequestDto(email);

        authApi.passwordForgot(body).enqueue(new Callback<SimpleDetailResponseDto>() {
            @Override
            public void onResponse(
                    @NonNull Call<SimpleDetailResponseDto> call,
                    @NonNull Response<SimpleDetailResponseDto> response
            ) {
                if (!response.isSuccessful()) {
                    String message = extractErrorMessage(response, "Ошибка при запросе сброса пароля: " + response.code());
                    callback.onError(message);
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<SimpleDetailResponseDto> call, @NonNull Throwable t) {
                callback.onError("Сетевая ошибка при запросе сброса пароля: " + safeMessage(t));
            }
        });
    }

    /**
     * Подтверждает код сброса пароля и задаёт новый пароль.
     * После успешного сброса локальные токены очищаются, т.к. сервер ревокирует сессии.
     */
    public void resetPassword(
            @NonNull String email,
            @NonNull String code,
            @NonNull String newPassword,
            @NonNull SimpleCallback callback
    ) {
        PasswordResetRequestDto body = new PasswordResetRequestDto(email, code, newPassword);

        authApi.passwordReset(body).enqueue(new Callback<SimpleDetailResponseDto>() {
            @Override
            public void onResponse(
                    @NonNull Call<SimpleDetailResponseDto> call,
                    @NonNull Response<SimpleDetailResponseDto> response
            ) {
                if (!response.isSuccessful()) {
                    String message = extractErrorMessage(response, "Ошибка при смене пароля: " + response.code());
                    callback.onError(message);
                    return;
                }
                authStorage.clearAll();
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<SimpleDetailResponseDto> call, @NonNull Throwable t) {
                callback.onError("Сетевая ошибка при смене пароля: " + safeMessage(t));
            }
        });
    }

    /**
     * Logout: ревокирует текущую refresh-сессию на сервере и чистит локальное хранилище.
     */
    public void logout(@NonNull SimpleCallback callback) {
        String refresh = authStorage.getRefreshToken();
        if (refresh == null || refresh.isEmpty()) {
            authStorage.clearAll();
            callback.onSuccess();
            return;
        }

        LogoutRequestDto body = new LogoutRequestDto(refresh);

        authApi.logout(body).enqueue(new Callback<SimpleDetailResponseDto>() {
            @Override
            public void onResponse(
                    @NonNull Call<SimpleDetailResponseDto> call,
                    @NonNull Response<SimpleDetailResponseDto> response
            ) {
                authStorage.clearAll();
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<SimpleDetailResponseDto> call, @NonNull Throwable t) {
                authStorage.clearAll();
                callback.onError("Ошибка при выходе: " + safeMessage(t));
            }
        });
    }

    /**
     * Явный refresh по сохранённому refresh_token + login.
     * При ошибке токены очищаются (выход из сессии).
     *
     * Авто-refresh при 401 по-прежнему должен делать твой TokenAuthenticator в общем сетевом слое.
     */
    public void refreshTokens(@NonNull RefreshCallback callback) {
        String refreshToken = authStorage.getRefreshToken();
        String login = authStorage.getLogin();

        if (refreshToken == null || refreshToken.isEmpty() || login == null || login.isEmpty()) {
            callback.onError("Нет сохранённых данных для обновления сессии");
            return;
        }

        UserRefreshRequestDto body = new UserRefreshRequestDto(
                refreshToken,
                login,
                null,
                null
        );

        authApi.refresh(body).enqueue(new Callback<TokenPairDto>() {
            @Override
            public void onResponse(
                    @NonNull Call<TokenPairDto> call,
                    @NonNull Response<TokenPairDto> response
            ) {
                if (!response.isSuccessful() || response.body() == null) {
                    String message = extractErrorMessage(response, "Не удалось обновить сессию: " + response.code());
                    authStorage.clearAll();
                    callback.onError(message);
                    return;
                }

                TokenPairDto tokens = response.body();
                authStorage.saveTokens(tokens.access_token, tokens.refresh_token);
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<TokenPairDto> call, @NonNull Throwable t) {
                authStorage.clearAll();
                callback.onError("Сетевая ошибка при обновлении сессии: " + safeMessage(t));
            }
        });
    }

    // =========================
    // Утилиты
    // =========================

    private String safeMessage(Throwable t) {
        String msg = t.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            return "неизвестная ошибка";
        }
        return msg;
    }

    /**
     * Разбирает тело ошибки FastAPI:
     * - {"detail": "string"}
     * - {"detail": {"message": "...", "code": "..."}}
     */
    private String extractErrorMessage(
            @NonNull Response<?> response,
            @NonNull String fallback
    ) {
        try {
            if (response.errorBody() == null) {
                return fallback;
            }
            String raw = response.errorBody().string();
            if (raw == null || raw.isEmpty()) {
                return fallback;
            }

            JsonElement element = JsonParser.parseString(raw);
            if (!element.isJsonObject()) {
                return fallback;
            }

            JsonObject root = element.getAsJsonObject();
            JsonElement detailElement = root.get("detail");
            if (detailElement == null || detailElement.isJsonNull()) {
                return fallback;
            }

            if (detailElement.isJsonPrimitive()) {
                return detailElement.getAsString();
            }

            if (detailElement.isJsonObject()) {
                JsonObject detailObj = detailElement.getAsJsonObject();
                if (detailObj.has("message") && detailObj.get("message").isJsonPrimitive()) {
                    return detailObj.get("message").getAsString();
                }
                if (detailObj.has("code") && detailObj.get("code").isJsonPrimitive()) {
                    return detailObj.get("code").getAsString();
                }
            }

            return fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    // =========================
    // DTO под /auth (делает их доступными и для AuthApi)
    // =========================

    public static final class TokenPairDto {
        public String access_token;
        public String refresh_token;
    }

    public static final class UserLoginRequestDto {
        public final String login;
        public final String password;

        public UserLoginRequestDto(String login, String password) {
            this.login = login;
            this.password = password;
        }
    }

    public static final class EmailRegisterRequestDto {
        public final String email;
        public final String password;
        @Nullable
        public final String phone;

        public EmailRegisterRequestDto(String email, String password, @Nullable String phone) {
            this.email = email;
            this.password = password;
            this.phone = phone;
        }
    }

    public static final class EmailVerificationConfirmRequestDto {
        public final String email;
        public final String code;

        public EmailVerificationConfirmRequestDto(String email, String code) {
            this.email = email;
            this.code = code;
        }
    }

    public static final class ResendVerificationEmailRequestDto {
        public final String email;

        public ResendVerificationEmailRequestDto(String email) {
            this.email = email;
        }
    }

    public static final class PasswordForgotRequestDto {
        public final String email;

        public PasswordForgotRequestDto(String email) {
            this.email = email;
        }
    }

    public static final class PasswordResetRequestDto {
        public final String email;
        public final String code;
        public final String new_password;

        public PasswordResetRequestDto(String email, String code, String newPassword) {
            this.email = email;
            this.code = code;
            this.new_password = newPassword;
        }
    }

    public static final class LogoutRequestDto {
        public final String refresh_token;

        public LogoutRequestDto(String refreshToken) {
            this.refresh_token = refreshToken;
        }
    }

    public static final class UserRefreshRequestDto {
        public final String refresh_token;
        @Nullable
        public final String login;
        @Nullable
        public final String email;
        @Nullable
        public final String phone;

        public UserRefreshRequestDto(
                @NonNull String refreshToken,
                @Nullable String login,
                @Nullable String email,
                @Nullable String phone
        ) {
            this.refresh_token = refreshToken;
            this.login = login;
            this.email = email;
            this.phone = phone;
        }
    }

    public static final class SimpleDetailResponseDto {
        public String detail;
    }
}
