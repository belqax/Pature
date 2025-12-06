package app.belqax.pature.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import app.belqax.pature.R;
import app.belqax.pature.adapter.SearchResultAdapter;
import app.belqax.pature.model.Animal;
import app.belqax.pature.ui.SearchFilterBottomSheet;
import app.belqax.pature.utils.SearchFilters;


public class SearchFragment extends Fragment implements SearchFilterBottomSheet.OnFiltersAppliedListener {

    private static final int GRID_SPAN_COUNT = 2;

    private TextInputEditText searchEditText;
    private ImageButton filterButton;
    private RecyclerView resultsRecyclerView;
    private View emptyStateContainer;

    private SearchResultAdapter adapter;

    private final List<Animal> allAnimals = new ArrayList<>();
    private final List<Animal> filteredAnimals = new ArrayList<>();

    private SearchFilters currentFilters = new SearchFilters();
    private String currentQuery = "";

    public SearchFragment() {
        // обязателен пустой конструктор
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchEditText = view.findViewById(R.id.searchEditText);
        filterButton = view.findViewById(R.id.filterButton);
        resultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer);

        initRecyclerView();
        initSearchInput();
        initFilterButton();

        initDummyAnimals();
        applyFiltersAndQuery();
    }

    private void initRecyclerView() {
        GridLayoutManager layoutManager =
                new GridLayoutManager(requireContext(), GRID_SPAN_COUNT);
        resultsRecyclerView.setLayoutManager(layoutManager);

        adapter = new SearchResultAdapter(requireContext(),
                new SearchResultAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(@NonNull Animal animal) {
                        // здесь потом можно открыть детальный экран животного
                    }

                    @Override
                    public void onFavoriteToggle(@NonNull Animal animal) {
                        // здесь позже можно реализовать избранное через отдельное хранилище
                    }
                });

        resultsRecyclerView.setAdapter(adapter);
    }

    private void initSearchInput() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after) {
            }

            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s != null ? s.toString().trim() : "";
                applyFiltersAndQuery();
            }
        });
    }

    private void initFilterButton() {
        filterButton.setOnClickListener(v -> {
            SearchFilterBottomSheet sheet =
                    SearchFilterBottomSheet.newInstance(currentFilters);
            sheet.setOnFiltersAppliedListener(this);
            sheet.show(getParentFragmentManager(), "SearchFilterBottomSheet");
        });
    }

    private void initDummyAnimals() {
        allAnimals.clear();

        allAnimals.add(new Animal(
                "1",
                "Снежа",
                "Ищем жениха",
                "https://example.com/snezha.jpg",
                "https://t.me/owner_snezha"
        ));

        allAnimals.add(new Animal(
                "2",
                "Мила",
                "Очень добрая и спокойная",
                "https://example.com/mila.jpg",
                "https://t.me/owner_mila"
        ));

        allAnimals.add(new Animal(
                "3",
                "Тайсон",
                "Активный, любит бегать",
                "https://example.com/tyson.jpg",
                "https://t.me/owner_tyson"
        ));

        allAnimals.add(new Animal(
                "4",
                "Рыжик",
                "Обожает гулять и играть с мячом",
                "",
                "https://t.me/owner_ryzhik"
        ));
    }

    private void applyFiltersAndQuery() {
        filteredAnimals.clear();

        String queryLower = currentQuery.toLowerCase(Locale.getDefault());

        for (Animal animal : allAnimals) {
            if (!matchesQuery(animal, queryLower)) {
                continue;
            }
            if (!matchesFilters(animal, currentFilters)) {
                continue;
            }
            filteredAnimals.add(animal);
        }

        adapter.setItems(filteredAnimals);
        updateEmptyState();
    }

    private boolean matchesQuery(@NonNull Animal animal,
                                 @NonNull String queryLower) {
        if (queryLower.isEmpty()) {
            return true;
        }

        String name = animal.getName() != null
                ? animal.getName().toLowerCase(Locale.getDefault())
                : "";
        String desc = animal.getDescription() != null
                ? animal.getDescription().toLowerCase(Locale.getDefault())
                : "";

        return name.contains(queryLower) || desc.contains(queryLower);
    }

    private boolean matchesFilters(@NonNull Animal animal,
                                   @NonNull SearchFilters filters) {
        // Сейчас модель Animal содержит только базовые поля.
        // Работает фильтр "только с фото"; остальные можно включить после расширения модели.

        if (filters.withPhotoOnly) {
            String imageUrl = animal.getImageUrl();
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                return false;
            }
        }

        // species / gender / age / favoritesOnly не проверяются,
        // чтобы не тянуть несуществующие поля из Animal.

        return true;
    }

    private void updateEmptyState() {
        if (filteredAnimals.isEmpty()) {
            resultsRecyclerView.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
        } else {
            resultsRecyclerView.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onFiltersApplied(@NonNull SearchFilters filters) {
        currentFilters = filters;
        applyFiltersAndQuery();
    }
}
