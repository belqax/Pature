package app.belqax.pature.data.repository;


import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import app.belqax.pature.data.network.ApiClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public final class AnimalRepository {

    private static final String TAG = "AnimalsRepository";

    // Конфиг репозитория (явно и легко меняется)
    private static final int DEFAULT_LIMIT = 50;
    private static final int DEFAULT_OFFSET = 0;
    private static final String DEFAULT_STATUS = "active";
    private static final String DEFAULT_ORDER_BY = "created_at_desc";

    private final AnimalApi api;

    public AnimalRepository() {
        Retrofit retrofit = ApiClient.getInstance().getRetrofit();
        this.api = retrofit.create(AnimalApi.class);
    }

    public AnimalRepository(@NonNull AnimalApi api) {
        this.api = Objects.requireNonNull(api, "api");
    }

    // ======================================================================
    // Public API: CRUD
    // ======================================================================

    public void createAnimal(
            @NonNull AnimalCreateRequestDto body,
            @NonNull RepoCallback<AnimalDto> cb
    ) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(cb, "cb");
        enqueue(api.createAnimal(body), cb, "createAnimal");
    }

    public void getAnimalById(
            long animalId,
            @NonNull RepoCallback<AnimalDto> cb
    ) {
        Objects.requireNonNull(cb, "cb");
        enqueue(api.getAnimalById(animalId), cb, "getAnimalById");
    }

    public void listMyAnimals(@NonNull RepoCallback<List<AnimalDto>> cb) {
        Objects.requireNonNull(cb, "cb");
        enqueue(api.listMyAnimals(), cb, "listMyAnimals");
    }

    public void updateAnimal(
            long animalId,
            @NonNull AnimalUpdateRequestDto body,
            @NonNull RepoCallback<AnimalDto> cb
    ) {
        Objects.requireNonNull(body, "body");
        Objects.requireNonNull(cb, "cb");
        enqueue(api.updateAnimal(animalId, body), cb, "updateAnimal");
    }

    public void deleteAnimal(
            long animalId,
            @NonNull RepoCallback<Void> cb
    ) {
        Objects.requireNonNull(cb, "cb");
        enqueue(api.deleteAnimal(animalId), cb, "deleteAnimal");
    }

    public void updateAnimalStatus(
            long animalId,
            @NonNull String newStatus,
            @NonNull RepoCallback<AnimalDto> cb
    ) {
        Objects.requireNonNull(newStatus, "newStatus");
        Objects.requireNonNull(cb, "cb");

        AnimalStatusUpdateRequestDto body = new AnimalStatusUpdateRequestDto();
        body.status = newStatus;

        enqueue(api.updateAnimalStatus(animalId, body), cb, "updateAnimalStatus");
    }

    // ======================================================================
    // Public API: FEED
    // ======================================================================

    public void getFeed(
            @Nullable String species,
            @Nullable String city,
            @Nullable String sex,
            @Nullable Integer ageFromYears,
            @Nullable Integer ageToYears,
            @Nullable Boolean hasPhotos,
            @Nullable Integer limit,
            @Nullable Integer offset,
            @NonNull RepoCallback<List<AnimalDto>> cb
    ) {
        Objects.requireNonNull(cb, "cb");

        int safeLimit = (limit != null) ? limit : DEFAULT_LIMIT;
        int safeOffset = (offset != null) ? offset : DEFAULT_OFFSET;

        enqueue(
                api.getFeed(
                        species,
                        city,
                        sex,
                        ageFromYears,
                        ageToYears,
                        hasPhotos,
                        DEFAULT_STATUS,
                        safeLimit,
                        safeOffset
                ),
                cb,
                "getFeed"
        );
    }

    public void getPublic(
            @Nullable String species,
            @Nullable String city,
            @Nullable String sex,
            @Nullable Integer ageFromYears,
            @Nullable Integer ageToYears,
            @Nullable Boolean hasPhotos,
            @Nullable Integer limit,
            @Nullable Integer offset,
            @Nullable String orderBy,
            @NonNull RepoCallback<List<AnimalDto>> cb
    ) {
        Objects.requireNonNull(cb, "cb");

        int safeLimit = (limit != null) ? limit : DEFAULT_LIMIT;
        int safeOffset = (offset != null) ? offset : DEFAULT_OFFSET;
        String safeOrderBy = (orderBy != null) ? orderBy : DEFAULT_ORDER_BY;

        enqueue(
                api.getPublic(
                        species,
                        city,
                        sex,
                        ageFromYears,
                        ageToYears,
                        hasPhotos,
                        DEFAULT_STATUS,
                        safeLimit,
                        safeOffset,
                        safeOrderBy
                ),
                cb,
                "getPublic"
        );
    }

    // ======================================================================
    // Public API: LIKES
    // ======================================================================

    public void likeAnimal(
            long animalId,
            boolean isLike,
            @NonNull RepoCallback<AnimalLikeResultDto> cb
    ) {
        Objects.requireNonNull(cb, "cb");

        AnimalLikeRequestDto body = new AnimalLikeRequestDto();
        body.result = isLike ? "like" : "dislike";

        enqueue(api.likeAnimal(animalId, body), cb, "likeAnimal");
    }

    // ======================================================================
    // Public API: PHOTOS
    // ======================================================================

    public void uploadAnimalPhoto(
            long animalId,
            @NonNull byte[] bytes,
            @Nullable String fileName,
            @Nullable String mimeType,
            @NonNull RepoCallback<AnimalDto> cb
    ) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(cb, "cb");

        if (bytes.length == 0) {
            cb.onComplete(RepoResult.networkError("uploadAnimalPhoto: file bytes are empty"));
            return;
        }

        String safeName = (fileName == null || fileName.trim().isEmpty()) ? "photo.jpg" : fileName.trim();
        String safeMime = (mimeType == null || mimeType.trim().isEmpty()) ? "image/jpeg" : mimeType.trim();

        MediaType mt = MediaType.parse(safeMime);
        if (mt == null) {
            cb.onComplete(RepoResult.networkError("uploadAnimalPhoto: invalid mimeType=" + safeMime));
            return;
        }

        RequestBody rb = RequestBody.create(bytes, mt);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", safeName, rb);

        enqueue(api.uploadAnimalPhoto(animalId, part), cb, "uploadAnimalPhoto");
    }

    public void deletePhoto(long photoId, @NonNull RepoCallback<Void> cb) {
        Objects.requireNonNull(cb, "cb");
        enqueue(api.deletePhoto(photoId), cb, "deletePhoto");
    }

    public void reorderPhotos(
            long animalId,
            @NonNull List<Long> orderedIds,
            @NonNull RepoCallback<Void> cb
    ) {
        Objects.requireNonNull(orderedIds, "orderedIds");
        Objects.requireNonNull(cb, "cb");

        if (orderedIds.isEmpty()) {
            cb.onComplete(RepoResult.networkError("reorderPhotos: photo_ids are empty"));
            return;
        }

        AnimalPhotosReorderRequestDto body = new AnimalPhotosReorderRequestDto();
        body.photoIds = orderedIds;

        enqueue(api.reorderPhotos(animalId, body), cb, "reorderPhotos");
    }

    public void setPrimaryPhoto(long animalId, long photoId, @NonNull RepoCallback<Void> cb) {
        Objects.requireNonNull(cb, "cb");
        enqueue(api.setPrimaryPhoto(animalId, photoId), cb, "setPrimaryPhoto");
    }

    // ======================================================================
    // Internal: enqueue helper
    // ======================================================================

    private static <T> void enqueue(
            @NonNull Call<T> call,
            @NonNull RepoCallback<T> cb,
            @NonNull String opName
    ) {
        Objects.requireNonNull(call, "call");
        Objects.requireNonNull(cb, "cb");
        Objects.requireNonNull(opName, "opName");

        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
                if (response.isSuccessful()) {
                    cb.onComplete(RepoResult.success(response.body()));
                    return;
                }

                String errBody = null;
                try {
                    if (response.errorBody() != null) {
                        errBody = response.errorBody().string();
                    }
                } catch (Exception e) {
                    Log.w(TAG, opName + ": failed to read errorBody: " + e);
                }

                cb.onComplete(
                        RepoResult.httpError(
                                response.code(),
                                opName + ": http error",
                                errBody
                        )
                );
            }

            @Override
            public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
                String msg = (t.getMessage() != null) ? t.getMessage() : "network error";
                if (t instanceof IOException) {
                    cb.onComplete(RepoResult.networkError(opName + ": " + msg));
                    return;
                }
                cb.onComplete(RepoResult.networkError(opName + ": " + msg));
            }
        });
    }

    // ======================================================================
    // Retrofit API (эндпоинты)
    // ======================================================================

    private interface AnimalApi {

        @GET("feed")
        Call<List<AnimalDto>> getFeed(
                @Query("species") String species,
                @Query("city") String city,
                @Query("sex") String sex,
                @Query("age_from_years") Integer ageFromYears,
                @Query("age_to_years") Integer ageToYears,
                @Query("has_photos") Boolean hasPhotos,
                @Query("status") String status,
                @Query("limit") Integer limit,
                @Query("offset") Integer offset
        );
        // CRUD
        @POST("animals")
        Call<AnimalDto> createAnimal(@Body AnimalCreateRequestDto body);

        @GET("animals/{id}")
        Call<AnimalDto> getAnimalById(@Path("id") long animalId);

        @GET("animals/my")
        Call<List<AnimalDto>> listMyAnimals();

        @PATCH("animals/{id}")
        Call<AnimalDto> updateAnimal(@Path("id") long animalId, @Body AnimalUpdateRequestDto body);

        @DELETE("animals/{id}")
        Call<Void> deleteAnimal(@Path("id") long animalId);

        @PATCH("animals/{id}/status")
        Call<AnimalDto> updateAnimalStatus(@Path("id") long animalId, @Body AnimalStatusUpdateRequestDto body);

        // Feed


        @GET("animals/public")
        Call<List<AnimalDto>> getPublic(
                @Query("species") String species,
                @Query("city") String city,
                @Query("sex") String sex,
                @Query("age_from_years") Integer ageFromYears,
                @Query("age_to_years") Integer ageToYears,
                @Query("has_photos") Boolean hasPhotos,
                @Query("status") String status,
                @Query("limit") Integer limit,
                @Query("offset") Integer offset,
                @Query("order_by") String orderBy
        );

        // Likes
        @POST("animals/{id}/like")
        Call<AnimalLikeResultDto> likeAnimal(@Path("id") long animalId, @Body AnimalLikeRequestDto body);

        // Photos
        @Multipart
        @POST("animals/{id}/photos")
        Call<AnimalDto> uploadAnimalPhoto(@Path("id") long animalId, @Part MultipartBody.Part file);

        @DELETE("animal-photos/{photoId}")
        Call<Void> deletePhoto(@Path("photoId") long photoId);

        @POST("animals/{id}/photos/reorder")
        Call<Void> reorderPhotos(@Path("id") long animalId, @Body AnimalPhotosReorderRequestDto body);

        @POST("animals/{id}/photos/{photoId}/primary")
        Call<Void> setPrimaryPhoto(@Path("id") long animalId, @Path("photoId") long photoId);
    }

    // ======================================================================
    // Result & Callback types
    // ======================================================================

    public static final class RepoResult<T> {

        public final boolean isSuccess;
        public final T data;

        public final Integer httpCode;
        public final String errorMessage;
        public final String errorBody;

        private RepoResult(boolean isSuccess, T data, Integer httpCode, String errorMessage, String errorBody) {
            this.isSuccess = isSuccess;
            this.data = data;
            this.httpCode = httpCode;
            this.errorMessage = errorMessage;
            this.errorBody = errorBody;
        }

        public static <T> RepoResult<T> success(T data) {
            return new RepoResult<>(true, data, null, null, null);
        }

        public static <T> RepoResult<T> httpError(int httpCode, String errorMessage, String errorBody) {
            return new RepoResult<>(false, null, httpCode, errorMessage, errorBody);
        }

        public static <T> RepoResult<T> networkError(String errorMessage) {
            return new RepoResult<>(false, null, null, errorMessage, null);
        }
    }

    public interface RepoCallback<T> {
        void onComplete(RepoResult<T> result);
    }

    // ======================================================================
    // DTO (модели)
    // ======================================================================

    public static final class AnimalPhotoDto {

        @SerializedName("id")
        public long id;

        @SerializedName("url")
        public String url;

        @SerializedName("thumb_url")
        public String thumbUrl;

        @SerializedName("is_primary")
        public boolean isPrimary;

        @SerializedName("position")
        public int position;
    }

    public static final class AnimalDto {

        @SerializedName("id")
        public long id;

        @SerializedName("owner_user_id")
        public long ownerUserId;

        @SerializedName("name")
        public String name;

        @SerializedName("species")
        public String species;

        @SerializedName("breed")
        public String breed;

        @SerializedName("sex")
        public String sex;

        @SerializedName("date_of_birth")
        public String dateOfBirth;

        @SerializedName("approx_age_years")
        public Integer approxAgeYears;

        @SerializedName("approx_age_months")
        public Integer approxAgeMonths;

        @SerializedName("weight_kg")
        public Double weightKg;

        @SerializedName("height_cm")
        public Double heightCm;

        @SerializedName("color")
        public String color;

        @SerializedName("pattern")
        public String pattern;

        @SerializedName("is_neutered")
        public Boolean isNeutered;

        @SerializedName("is_vaccinated")
        public Boolean isVaccinated;

        @SerializedName("is_chipped")
        public Boolean isChipped;

        @SerializedName("chip_number")
        public String chipNumber;

        @SerializedName("temperament_note")
        public String temperamentNote;

        @SerializedName("description")
        public String description;

        @SerializedName("status")
        public String status;

        @SerializedName("city")
        public String city;

        @SerializedName("geo_lat")
        public Double geoLat;

        @SerializedName("geo_lng")
        public Double geoLng;

        @SerializedName("created_at")
        public String createdAt;

        @SerializedName("updated_at")
        public String updatedAt;

        @SerializedName("photos")
        public List<AnimalPhotoDto> photos;
    }

    public static final class AnimalCreateRequestDto {

        @SerializedName("name")
        public String name;

        @SerializedName("species")
        public String species;

        @SerializedName("breed")
        public String breed;

        @SerializedName("sex")
        public String sex;

        @SerializedName("date_of_birth")
        public String dateOfBirth;

        @SerializedName("approx_age_years")
        public Integer approxAgeYears;

        @SerializedName("approx_age_months")
        public Integer approxAgeMonths;

        @SerializedName("weight_kg")
        public Double weightKg;

        @SerializedName("height_cm")
        public Double heightCm;

        @SerializedName("color")
        public String color;

        @SerializedName("pattern")
        public String pattern;

        @SerializedName("is_neutered")
        public Boolean isNeutered;

        @SerializedName("is_vaccinated")
        public Boolean isVaccinated;

        @SerializedName("is_chipped")
        public Boolean isChipped;

        @SerializedName("chip_number")
        public String chipNumber;

        @SerializedName("temperament_note")
        public String temperamentNote;

        @SerializedName("description")
        public String description;

        @SerializedName("status")
        public String status;

        @SerializedName("city")
        public String city;

        @SerializedName("geo_lat")
        public Double geoLat;

        @SerializedName("geo_lng")
        public Double geoLng;
    }

    public static final class AnimalUpdateRequestDto {

        @SerializedName("name")
        public String name;

        @SerializedName("species")
        public String species;

        @SerializedName("breed")
        public String breed;

        @SerializedName("sex")
        public String sex;

        @SerializedName("date_of_birth")
        public String dateOfBirth;

        @SerializedName("approx_age_years")
        public Integer approxAgeYears;

        @SerializedName("approx_age_months")
        public Integer approxAgeMonths;

        @SerializedName("weight_kg")
        public Double weightKg;

        @SerializedName("height_cm")
        public Double heightCm;

        @SerializedName("color")
        public String color;

        @SerializedName("pattern")
        public String pattern;

        @SerializedName("is_neutered")
        public Boolean isNeutered;

        @SerializedName("is_vaccinated")
        public Boolean isVaccinated;

        @SerializedName("is_chipped")
        public Boolean isChipped;

        @SerializedName("chip_number")
        public String chipNumber;

        @SerializedName("temperament_note")
        public String temperamentNote;

        @SerializedName("description")
        public String description;

        @SerializedName("status")
        public String status;

        @SerializedName("city")
        public String city;

        @SerializedName("geo_lat")
        public Double geoLat;

        @SerializedName("geo_lng")
        public Double geoLng;
    }

    public static final class AnimalStatusUpdateRequestDto {
        @SerializedName("status")
        public String status;
    }

    public static final class AnimalPhotosReorderRequestDto {
        @SerializedName("photo_ids")
        public List<Long> photoIds;
    }

    public static final class AnimalLikeRequestDto {
        @SerializedName("result")
        public String result;
    }

    public static final class AnimalLikeResultDto {

        @SerializedName("animal_id")
        public long animalId;

        @SerializedName("from_user_id")
        public long fromUserId;

        @SerializedName("result")
        public String result;

        @SerializedName("created_at")
        public String createdAt;

        @SerializedName("match_created")
        public boolean matchCreated;

        @SerializedName("match_user_id")
        public Long matchUserId;

        @SerializedName("match_id")
        public Long matchId;
    }

    // ======================================================================
    // Example usage (в коде)
    // ======================================================================

    public static void exampleUsage() {
        AnimalRepository repo = new AnimalRepository();
        repo.getFeed(
                "Кошка",
                "Москва",
                null,
                1,
                10,
                true,
                50,
                0,
                result -> {
                    if (result.isSuccess) {
                        List<AnimalDto> animals = result.data;
                        Log.i(TAG, "feed size=" + (animals != null ? animals.size() : 0));
                    } else {
                        Log.e(TAG, "feed error code=" + result.httpCode + " body=" + result.errorBody);
                    }
                }
        );
    }
}
