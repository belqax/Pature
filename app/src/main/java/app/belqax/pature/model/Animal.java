package app.belqax.pature.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Animal {

    // Базовые поля
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private String contactUrl;

    // Дополнительные поля для поиска/фильтров
    // "dog" / "cat" / и т.п.
    @Nullable
    private String species;

    // "male" / "female" / и т.п.
    @Nullable
    private String gender;

    // Возраст в годах (0, если неизвестен)
    private int ageYears;

    // Флаг избранного
    private boolean favorite;

    /**
     * Старый конструктор – всё, что уже написано в проекте, продолжит работать.
     * Дополнительные поля инициализируются значениями по умолчанию.
     */
    public Animal(String id,
                  String name,
                  String description,
                  String imageUrl,
                  String contactUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.contactUrl = contactUrl;

        this.species = null;
        this.gender = null;
        this.ageYears = 0;
        this.favorite = false;
    }

    /**
     * Расширенный конструктор – удобно использовать там, где нужны фильтры.
     */
    public Animal(String id,
                  String name,
                  String description,
                  String imageUrl,
                  String contactUrl,
                  @Nullable String species,
                  @Nullable String gender,
                  int ageYears,
                  boolean favorite) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.contactUrl = contactUrl;
        this.species = species;
        this.gender = gender;
        this.ageYears = ageYears;
        this.favorite = favorite;
    }

    // region getters/setters базовых полей

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(@Nullable String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Nullable
    public String getContactUrl() {
        return contactUrl;
    }

    public void setContactUrl(@Nullable String contactUrl) {
        this.contactUrl = contactUrl;
    }

    // endregion

    // region getters/setters дополнительных полей

    @Nullable
    public String getSpecies() {
        return species;
    }

    public void setSpecies(@Nullable String species) {
        this.species = species;
    }

    @Nullable
    public String getGender() {
        return gender;
    }

    public void setGender(@Nullable String gender) {
        this.gender = gender;
    }

    public int getAgeYears() {
        return ageYears;
    }

    public void setAgeYears(int ageYears) {
        this.ageYears = ageYears;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    // endregion
}
