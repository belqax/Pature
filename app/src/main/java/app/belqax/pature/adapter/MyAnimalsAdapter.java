package app.belqax.pature.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import app.belqax.pature.R;
import app.belqax.pature.data.repository.AnimalRepository;

public final class MyAnimalsAdapter extends RecyclerView.Adapter<MyAnimalsAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull AnimalRepository.AnimalDto item);
    }

    private final Context context;
    private final List<AnimalRepository.AnimalDto> items = new ArrayList<>();
    @Nullable
    private OnItemClickListener listener;

    public MyAnimalsAdapter(@NonNull Context context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    public void setListener(@Nullable OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<AnimalRepository.AnimalDto> newItems) {
        items.clear();
        items.addAll(Objects.requireNonNull(newItems, "newItems"));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_my_animal, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AnimalRepository.AnimalDto dto = items.get(position);

        String title = dto.name != null && !dto.name.trim().isEmpty() ? dto.name.trim() : "Без имени";

        String species = dto.species != null ? dto.species.trim() : "";
        String city = dto.city != null ? dto.city.trim() : "";

        StringBuilder subtitle = new StringBuilder();
        if (!species.isEmpty()) subtitle.append(species);
        if (!city.isEmpty()) {
            if (subtitle.length() > 0) subtitle.append(" • ");
            subtitle.append(city);
        }
        String subtitleText = subtitle.length() > 0 ? subtitle.toString() : " ";

        h.title.setText(title);
        h.subtitle.setText(subtitleText);

        String photoUrl = null;
        if (dto.photos != null && !dto.photos.isEmpty()) {
            AnimalRepository.AnimalPhotoDto best = null;
            for (AnimalRepository.AnimalPhotoDto p : dto.photos) {
                if (p != null && p.isPrimary) {
                    best = p;
                    break;
                }
            }
            if (best == null) best = dto.photos.get(0);
            if (best != null) {
                if (best.thumbUrl != null && !best.thumbUrl.trim().isEmpty()) {
                    photoUrl = best.thumbUrl.trim();
                } else if (best.url != null && !best.url.trim().isEmpty()) {
                    photoUrl = best.url.trim();
                }
            }
        }

        Glide.with(context)
                .load(photoUrl)
                .centerCrop()
                .placeholder(R.drawable.photo)
                .into(h.photo);

        h.itemView.setOnClickListener(v -> {
            OnItemClickListener l = listener;
            if (l != null) {
                l.onItemClick(dto);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class VH extends RecyclerView.ViewHolder {

        final ImageView photo;
        final TextView title;
        final TextView subtitle;

        VH(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.myAnimalPhoto);
            title = itemView.findViewById(R.id.myAnimalTitle);
            subtitle = itemView.findViewById(R.id.myAnimalSubtitle);
        }
    }
}
