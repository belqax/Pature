package app.belqax.pature.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import app.belqax.pature.data.network.ApiClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.HttpException;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Репозиторий для работы с подсказками адресов:
 * GET /addresses/autocomplete
 */
public class AddressRepository {

    private static final String TAG = "AddressRepository";

    private final AddressApi api;

    public AddressRepository(@NonNull AddressApi api) {
        this.api = api;
    }

    /**
     * Фабрика на основе уже существующего ApiClient.
     */
    @NonNull
    public static AddressRepository create(@NonNull Context context) {
        ApiClient apiClient = ApiClient.getInstance();
        Retrofit retrofit = apiClient.getRetrofit();
        AddressApi api = retrofit.create(AddressApi.class);
        return new AddressRepository(api);
    }

    // region Public API

    /**
     * Получает подсказки по адресу.
     *
     * @param text   обязательный текст поиска (min 3 символа)
     * @param limit  максимум подсказок (1–20), можно null для значения по умолчанию на бэке
     * @param lang   язык (например, "ru"), можно null
     * @param type   тип Geoapify (building, amenity и т.п.), можно null
     */
    public void autocomplete(
            @NonNull String text,
            @Nullable Integer limit,
            @Nullable String lang,
            @Nullable String type,
            @NonNull final SuggestionsCallback callback
    ) {
        Log.d(TAG, "autocomplete: text=" + text + " limit=" + limit + " lang=" + lang + " type=" + type);

        api.autocomplete(text, limit, lang, type).enqueue(new Callback<AutocompleteResponse>() {
            @Override
            public void onResponse(@NonNull Call<AutocompleteResponse> call,
                                   @NonNull Response<AutocompleteResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AutocompleteResponse body = response.body();
                    List<SuggestionDto> suggestions = body.getSuggestions();
                    if (suggestions == null) {
                        suggestions = Collections.emptyList();
                    }
                    Log.d(TAG, "autocomplete: success, got " + suggestions.size() + " suggestions");
                    callback.onSuccess(suggestions);
                } else {
                    Log.w(TAG, "autocomplete: http error " + response.code());
                    callback.onError(mapHttpError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AutocompleteResponse> call,
                                  @NonNull Throwable t) {
                Log.e(TAG, "autocomplete: failure", t);
                callback.onError(mapNetworkError(t));
            }
        });
    }

    // endregion

    // region Error mapping

    @NonNull
    private AddressError mapHttpError(@NonNull Response<?> response) {
        int code = response.code();
        String rawBody = null;

        ResponseBody errorBody = response.errorBody();
        if (errorBody != null) {
            try {
                rawBody = errorBody.string();
            } catch (IOException e) {
                Log.w(TAG, "mapHttpError: failed to read errorBody", e);
            }
        }

        String message;
        if (code == 400) {
            message = "Некорректный запрос к подсказкам адресов.";
        } else if (code == 401 || code == 403) {
            message = "Сессия недействительна. Попробуйте войти снова.";
        } else if (code >= 500) {
            message = "Сервис подсказок адресов временно недоступен.";
        } else {
            message = "Ошибка подсказок адреса (" + code + ")";
        }

        boolean unauthorized = (code == 401 || code == 403);

        return new AddressError(
                code,
                message,
                false,
                unauthorized,
                rawBody,
                null
        );
    }

    @NonNull
    private AddressError mapNetworkError(@NonNull Throwable t) {
        if (t instanceof HttpException) {
            HttpException httpException = (HttpException) t;
            int code = httpException.code();
            boolean unauthorized = (code == 401 || code == 403);
            return new AddressError(
                    code,
                    "Ошибка HTTP " + code,
                    true,
                    unauthorized,
                    null,
                    t
            );
        }

        return new AddressError(
                0,
                "Ошибка сети при запросе подсказок адреса. Проверьте подключение к интернету.",
                true,
                false,
                null,
                t
        );
    }

    // endregion

    // region DTO / API

    public interface SuggestionsCallback {
        void onSuccess(@NonNull List<SuggestionDto> suggestions);

        void onError(@NonNull AddressError error);
    }

    public static final class AddressError {
        private final int httpCode;
        @NonNull
        private final String message;
        private final boolean networkError;
        private final boolean unauthorized;
        @Nullable
        private final String rawBody;
        @Nullable
        private final Throwable cause;

        public AddressError(int httpCode,
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

    public interface AddressApi {

        @GET("addresses/autocomplete")
        Call<AutocompleteResponse> autocomplete(
                @Query("text") @NonNull String text,
                @Query("limit") @Nullable Integer limit,
                @Query("lang") @Nullable String lang,
                @Query("type") @Nullable String type
        );
    }

    public static final class AutocompleteResponse {

        @Nullable
        private String query_text;

        @Nullable
        private List<SuggestionDto> suggestions;

        @Nullable
        public String getQueryText() {
            return query_text;
        }

        @Nullable
        public List<SuggestionDto> getSuggestions() {
            return suggestions;
        }
    }

    /**
     * Один элемент из suggestions.
     * Полей много, но на клиенте минимум нужен formatted.
     */
    public static final class SuggestionDto {

        @Nullable
        private String formatted;
        @Nullable
        private Double lat;
        @Nullable
        private Double lon;
        @Nullable
        private String country;
        @Nullable
        private String state;
        @Nullable
        private String region;
        @Nullable
        private String county;
        @Nullable
        private String city;
        @Nullable
        private String district;
        @Nullable
        private String neighbourhood;
        @Nullable
        private String postcode;
        @Nullable
        private String street;
        @Nullable
        private String housenumber;
        @Nullable
        private String plus_code;
        @Nullable
        private String timezone;
        @Nullable
        private String result_type;
        @Nullable
        private Double confidence;

        @Nullable
        public String getFormatted() {
            return formatted;
        }

        @Nullable
        public Double getLat() {
            return lat;
        }

        @Nullable
        public Double getLon() {
            return lon;
        }

        @Nullable
        public String getCountry() {
            return country;
        }

        @Nullable
        public String getState() {
            return state;
        }

        @Nullable
        public String getRegion() {
            return region;
        }

        @Nullable
        public String getCounty() {
            return county;
        }

        @Nullable
        public String getCity() {
            return city;
        }

        @Nullable
        public String getDistrict() {
            return district;
        }

        @Nullable
        public String getNeighbourhood() {
            return neighbourhood;
        }

        @Nullable
        public String getPostcode() {
            return postcode;
        }

        @Nullable
        public String getStreet() {
            return street;
        }

        @Nullable
        public String getHousenumber() {
            return housenumber;
        }

        @Nullable
        public String getPlusCode() {
            return plus_code;
        }

        @Nullable
        public String getTimezone() {
            return timezone;
        }

        @Nullable
        public String getResultType() {
            return result_type;
        }

        @Nullable
        public Double getConfidence() {
            return confidence;
        }
    }

    // endregion
}
