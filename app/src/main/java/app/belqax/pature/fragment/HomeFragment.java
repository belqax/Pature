package app.belqax.pature.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import app.belqax.pature.R;
import app.belqax.pature.adapter.CardAdapter;
import app.belqax.pature.model.Animal;
import app.belqax.pature.ui.PatureStackLayout;

public class HomeFragment extends Fragment {

    private PatureStackLayout patureStackLayout;

    public HomeFragment() {
        // пустой публичный конструктор обязателен для Fragment
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // fragment_home.xml:
        // <FrameLayout ...>
        //     <app.belqax.pature.ui.PatureStackLayout
        //         android:id="@+id/patureStack"
        //         android:layout_width="match_parent"
        //         android:layout_height="match_parent" />
        // </FrameLayout>
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        patureStackLayout = view.findViewById(R.id.patureStack);

        List<Animal> animals = createAnimals();

        CardAdapter adapter = new CardAdapter(requireContext(), animals);
        patureStackLayout.setAdapter(adapter);
    }

    @NonNull
    private List<Animal> createAnimals() {
        List<Animal> animals = new ArrayList<>();

        // Здесь поставь реальные данные и ссылки
        animals.add(new Animal(
                "1",
                "Снежа",
                "Ищем жениха",
                "https://example.com/images/snezha.jpg",
                "https://t.me/owner_snezha"
        ));

        animals.add(new Animal(
                "2",
                "Мила",
                "Добрая и спокойная",
                "https://example.com/images/mila.jpg",
                "https://t.me/owner_mila"
        ));

        animals.add(new Animal(
                "3",
                "Боня",
                "Очень ласковый и игривый",
                "https://example.com/images/bonya.jpg",
                "https://t.me/owner_bonya"
        ));

        animals.add(new Animal(
                "4",
                "Рыжик",
                "Обожает гулять и играть с мячом",
                "https://example.com/images/ryzhik.jpg",
                "https://t.me/owner_ryzhik"
        ));

        animals.add(new Animal(
                "5",
                "Луна",
                "Тихая и домашняя, любит лежать рядом",
                "https://example.com/images/luna.jpg",
                "https://t.me/owner_luna"
        ));

        animals.add(new Animal(
                "6",
                "Тайсон",
                "Активный, подойдёт в частный дом",
                "https://example.com/images/tyson.jpg",
                "https://t.me/owner_tyson"
        ));

        animals.add(new Animal(
                "7",
                "Ника",
                "Очень умная и быстро привыкает к людям",
                "https://example.com/images/nika.jpg",
                "https://t.me/owner_nika"
        ));

        return animals;
    }
}
