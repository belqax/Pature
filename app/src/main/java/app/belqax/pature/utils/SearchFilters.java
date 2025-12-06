package app.belqax.pature.utils;

import androidx.annotation.NonNull;

public class SearchFilters {

    public static final String SPECIES_ANY = "any";
    public static final String SPECIES_DOG = "dog";
    public static final String SPECIES_CAT = "cat";

    public static final String GENDER_ANY = "any";
    public static final String GENDER_MALE = "male";
    public static final String GENDER_FEMALE = "female";

    public String species = SPECIES_ANY;
    public String gender = GENDER_ANY;
    public int minAgeYears = 0;
    public int maxAgeYears = 20;
    public boolean withPhotoOnly = false;
    public boolean favoritesOnly = false;

    @NonNull
    public SearchFilters copy() {
        SearchFilters f = new SearchFilters();
        f.species = this.species;
        f.gender = this.gender;
        f.minAgeYears = this.minAgeYears;
        f.maxAgeYears = this.maxAgeYears;
        f.withPhotoOnly = this.withPhotoOnly;
        f.favoritesOnly = this.favoritesOnly;
        return f;
    }
}
