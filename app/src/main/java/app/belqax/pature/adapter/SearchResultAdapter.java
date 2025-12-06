package app.belqax.pature.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import app.belqax.pature.R;
import app.belqax.pature.model.Animal;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.TileViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull Animal animal);
        void onFavoriteToggle(@NonNull Animal animal);
    }

    private final Context context;
    private final LayoutInflater inflater;
    private final List<Animal> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public SearchResultAdapter(@NonNull Context context,
                               @NonNull OnItemClickListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void setItems(@NonNull List<Animal> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.item_search_result, parent, false);
        return new TileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
        Animal item = items.get(position);
        holder.bind(item, listener, context);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TileViewHolder extends RecyclerView.ViewHolder {

        private final ImageView tileImage;
        private final ImageView favoriteIcon;
        private final TextView tileName;
        private final TextView tileMeta;

        public TileViewHolder(@NonNull View itemView) {
            super(itemView);
            tileImage = itemView.findViewById(R.id.tileImage);
            favoriteIcon = itemView.findViewById(R.id.tileFavoriteIcon);
            tileName = itemView.findViewById(R.id.tileName);
            tileMeta = itemView.findViewById(R.id.tileMeta);
        }

        void bind(@NonNull Animal animal,
                  @NonNull OnItemClickListener listener,
                  @NonNull Context context) {

            tileName.setText(animal.getName());

            String speciesLabel = animal.getSpecies(); // добавь поле species в Animal, если нужно
            if (speciesLabel == null || speciesLabel.isEmpty()) {
                speciesLabel = context.getString(R.string.search_filter_any);
            }

            String genderLabel = animal.getGender(); // добавь поле gender в Animal по мере необходимости

            String meta = speciesLabel;
            if (animal.getAgeYears() > 0) {
                meta += " · " + animal.getAgeYears() + " " + context.getString(R.string.search_age_years_short);
            }
            if (genderLabel != null && !genderLabel.isEmpty()) {
                meta += " · " + genderLabel;
            }

            tileMeta.setText(meta);

            Glide.with(context)
                    .load(animal.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.photo)
                    .into(tileImage);

            // Если есть флаг избранного, можно подсвечивать:
            if (animal.isFavorite()) {
                favoriteIcon.setAlpha(1f);
            } else {
                favoriteIcon.setAlpha(0f);
            }

            itemView.setOnClickListener(v -> listener.onItemClick(animal));
            favoriteIcon.setOnClickListener(v -> listener.onFavoriteToggle(animal));
        }
    }
}
