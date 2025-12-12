package app.belqax.pature.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public final class AnimalCardItem {

    private final long id;
    @NonNull
    private final String name;
    @Nullable
    private final String description;
    @Nullable
    private final String imageUrl;

    public AnimalCardItem(
            long id,
            @NonNull String name,
            @Nullable String description,
            @Nullable String imageUrl
    ) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
        this.description = description;
        this.imageUrl = imageUrl;
    }

    public long getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }
}
