package app.belqax.pature.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;

import java.util.List;

import app.belqax.pature.R;
import app.belqax.pature.model.Animal;

public class CardAdapter {

    private final Context context;
    private final List<Animal> animals;

    public CardAdapter(@NonNull Context ctx, @NonNull List<Animal> items) {
        this.context = ctx;          // важен контекст активности, не applicationContext
        this.animals = items;
    }

    public int getCount() {
        return animals.size();
    }

    @NonNull
    public View getView(int position, @NonNull ViewGroup parent) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_animal_card, parent, false);

        Animal animal = animals.get(position);

        ImageView image = v.findViewById(R.id.cardImage);
        TextView name = v.findViewById(R.id.cardName);
        TextView desc = v.findViewById(R.id.cardDescription);
        ImageButton like = v.findViewById(R.id.cardLike);
        ImageButton dislike = v.findViewById(R.id.cardDislike);
        ImageButton contact = v.findViewById(R.id.cardContact);

        name.setText(animal.getName());
        desc.setText(animal.getDescription());

        Glide.with(context)
                .load(animal.getImageUrl())
                .centerCrop()
                .placeholder(R.drawable.photo)
                .into(image);

        like.setOnClickListener(view -> {
            // обработка лайка, если нужно
        });

        dislike.setOnClickListener(view -> {
            // обработка дизлайка, если нужно
        });

        contact.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(animal.getContactUrl()));
            context.startActivity(intent);
        });

        return v;
    }
}
