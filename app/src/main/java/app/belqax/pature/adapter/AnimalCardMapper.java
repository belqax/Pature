package app.belqax.pature.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import app.belqax.pature.data.repository.AnimalRepository;
import app.belqax.pature.model.AnimalCardItem;

public final class AnimalCardMapper {

    private AnimalCardMapper() {
    }

    @NonNull
    public static AnimalCardItem fromDto(@NonNull AnimalRepository.AnimalDto dto) {
        String title = dto.name != null && !dto.name.trim().isEmpty() ? dto.name.trim() : "Без имени";
        String desc = dto.description;

        String image = pickBestPhotoUrl(dto.photos);

        return new AnimalCardItem(
                dto.id,
                title,
                desc,
                image
        );
    }

    @Nullable
    private static String pickBestPhotoUrl(@Nullable List<AnimalRepository.AnimalPhotoDto> photos) {
        if (photos == null || photos.isEmpty()) {
            return null;
        }

        AnimalRepository.AnimalPhotoDto primary = null;
        for (AnimalRepository.AnimalPhotoDto p : photos) {
            if (p != null && p.isPrimary) {
                primary = p;
                break;
            }
        }

        AnimalRepository.AnimalPhotoDto chosen = primary != null ? primary : photos.get(0);
        if (chosen == null) {
            return null;
        }

        if (chosen.thumbUrl != null && !chosen.thumbUrl.trim().isEmpty()) {
            return chosen.thumbUrl.trim();
        }
        if (chosen.url != null && !chosen.url.trim().isEmpty()) {
            return chosen.url.trim();
        }
        return null;
    }
}
