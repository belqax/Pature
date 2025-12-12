package app.belqax.pature.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import app.belqax.pature.R;
import app.belqax.pature.model.AnimalCardItem;

public final class CardAdapter {

    public interface CardActionListener {
        void onLikeClicked(@NonNull AnimalCardItem item);
        void onDislikeClicked(@NonNull AnimalCardItem item);
        void onCardClicked(@NonNull AnimalCardItem item);
    }

    private final Context context;
    private final List<AnimalCardItem> items = new ArrayList<>();
    @Nullable
    private CardActionListener listener;

    public CardAdapter(@NonNull Context ctx) {
        this.context = Objects.requireNonNull(ctx, "ctx");
    }

    public void setListener(@Nullable CardActionListener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<AnimalCardItem> newItems) {
        items.clear();
        items.addAll(Objects.requireNonNull(newItems, "newItems"));
    }

    public void addItems(@NonNull List<AnimalCardItem> more) {
        items.addAll(Objects.requireNonNull(more, "more"));
    }

    public int getCount() {
        return items.size();
    }

    @NonNull
    public AnimalCardItem getItem(int position) {
        return items.get(position);
    }

    public void removeAt(int position) {
        if (position < 0 || position >= items.size()) {
            return;
        }
        items.remove(position);
    }

    @NonNull
    public View getView(int position, @NonNull ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_animal_card, parent, false);

        AnimalCardItem item = items.get(position);

        ImageView image = v.findViewById(R.id.cardImage);
        TextView name = v.findViewById(R.id.cardName);
        TextView desc = v.findViewById(R.id.cardDescription);
        ImageButton like = v.findViewById(R.id.cardLike);
        ImageButton dislike = v.findViewById(R.id.cardDislike);
        ImageButton contact = v.findViewById(R.id.cardContact);

        name.setText(item.getName());
        desc.setText(item.getDescription() != null ? item.getDescription() : "");

        Glide.with(context)
                .load(item.getImageUrl())
                .centerCrop()
                .placeholder(R.drawable.photo)
                .into(image);

        v.setOnClickListener(view -> {
            CardActionListener l = listener;
            if (l != null) {
                l.onCardClicked(item);
            }
        });

        like.setOnClickListener(view -> {
            CardActionListener l = listener;
            if (l != null) {
                l.onLikeClicked(item);
            }
        });

        dislike.setOnClickListener(view -> {
            CardActionListener l = listener;
            if (l != null) {
                l.onDislikeClicked(item);
            }
        });

        // Пока нет contactUrl с бэка: оставляем кнопку, но не падаем.
        contact.setOnClickListener(view -> {
            CardActionListener l = listener;
            if (l != null) {
                l.onCardClicked(item);
            }
        });

        return v;
    }
}
