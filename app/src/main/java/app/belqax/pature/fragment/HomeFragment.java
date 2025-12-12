package app.belqax.pature.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.belqax.pature.R;
import app.belqax.pature.adapter.CardAdapter;
import app.belqax.pature.adapter.AnimalCardMapper;
import app.belqax.pature.data.repository.AnimalRepository;
import app.belqax.pature.model.AnimalCardItem;
import app.belqax.pature.ui.PatureStackLayout;

public final class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // Явный конфиг (правится сверху, без магии)
    private static final int PAGE_LIMIT = 30;
    private static final int PREFETCH_WHEN_LEFT = 8;
    private static final boolean HAS_PHOTOS_ONLY = false;

    @Nullable
    private PatureStackLayout stack;

    @Nullable
    private CardAdapter adapter;

    private final AnimalRepository repo = new AnimalRepository();

    private int offset = 0;
    private boolean isLoading = false;
    private boolean isEndReached = false;

    private final List<AnimalCardItem> cards = new ArrayList<>();
    private final Set<Long> seenIds = new HashSet<>();

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        stack = view.findViewById(R.id.patureStack);

        CardAdapter a = new CardAdapter(requireContext());
        a.setListener(new CardAdapter.CardActionListener() {
            @Override
            public void onLikeClicked(@NonNull AnimalCardItem item) {
                PatureStackLayout s = stack;
                if (s != null) {
                    s.swipeTopRight();
                }
            }

            @Override
            public void onDislikeClicked(@NonNull AnimalCardItem item) {
                PatureStackLayout s = stack;
                if (s != null) {
                    s.swipeTopLeft();
                }
            }

            @Override
            public void onCardClicked(@NonNull AnimalCardItem item) {
                Toast.makeText(requireContext(), item.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        adapter = a;

        PatureStackLayout s = stack;
        if (s != null) {
            s.setAdapter(a);
            s.setOnCardSwipedListener(new PatureStackLayout.OnCardSwipedListener() {
                @Override
                public void onCardSwiped(@NonNull View cardView, int adapterPosition, @NonNull PatureStackLayout.Direction direction) {
                    onSwiped(adapterPosition, direction);
                    maybePrefetch();
                }

                @Override
                public void onCardDrag(@NonNull View cardView, float progress, @NonNull PatureStackLayout.Direction direction) {
                    // Можно сюда повесить вибрацию/звук на 1.0, если нужно.
                }
            });
        }

        // Первая загрузка
        loadNextPageIfNeeded(true);
    }

    private void onSwiped(int adapterPosition, @NonNull PatureStackLayout.Direction direction) {
        if (adapterPosition < 0 || adapterPosition >= cards.size()) {
            Log.w(TAG, "swipe position out of range: " + adapterPosition + " size=" + cards.size());
            return;
        }

        AnimalCardItem item = cards.get(adapterPosition);
        boolean isLike = direction == PatureStackLayout.Direction.RIGHT;

        sendLikeDislike(item, isLike);
    }

    private void sendLikeDislike(@NonNull AnimalCardItem item, boolean isLike) {
        repo.likeAnimal(item.getId(), isLike, result -> {
            if (!isAdded()) {
                return;
            }

            if (result.isSuccess && result.data != null) {
                AnimalRepository.AnimalLikeResultDto dto = result.data;

                if (dto.matchCreated) {
                    Toast.makeText(requireContext(), "Это матч", Toast.LENGTH_SHORT).show();
                    // Здесь можно открыть экран матчей, когда он будет готов.
                }
                return;
            }

            // Ошибку лайка не превращаем в откат свайпа: UX обычно лучше без “возврата” карточки.
            String msg = buildRepoError("likeAnimal", result);
            Log.e(TAG, msg);
            Toast.makeText(requireContext(), "Не удалось отправить реакцию", Toast.LENGTH_SHORT).show();
        });
    }

    private void maybePrefetch() {
        PatureStackLayout s = stack;
        CardAdapter a = adapter;

        if (s == null || a == null) {
            return;
        }

        int top = s.getTopAdapterPosition();
        int left = a.getCount() - top;

        if (left <= PREFETCH_WHEN_LEFT) {
            loadNextPageIfNeeded(false);
        }
    }

    private void loadNextPageIfNeeded(boolean isInitial) {
        if (isLoading || isEndReached) {
            return;
        }

        isLoading = true;

        // TODO: сюда позже подключишь фильтры пользователя (species/city/age и т.д.)
        repo.getFeed(
                null,           // species
                null,           // city
                null,           // sex
                null,           // ageFromYears
                null,           // ageToYears
                HAS_PHOTOS_ONLY,
                PAGE_LIMIT,
                offset,
                result -> {
                    if (!isAdded()) {
                        isLoading = false;
                        return;
                    }

                    if (!result.isSuccess) {
                        isLoading = false;

                        String msg = buildRepoError("getFeed", result);
                        Log.e(TAG, msg);

                        if (isInitial) {
                            Toast.makeText(requireContext(), "Ошибка загрузки ленты", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    List<AnimalRepository.AnimalDto> data = result.data;
                    if (data == null || data.isEmpty()) {
                        isLoading = false;
                        isEndReached = true;
                        return;
                    }

                    List<AnimalCardItem> mapped = new ArrayList<>();
                    for (AnimalRepository.AnimalDto dto : data) {
                        if (dto == null) {
                            continue;
                        }
                        if (seenIds.contains(dto.id)) {
                            continue;
                        }
                        seenIds.add(dto.id);
                        mapped.add(AnimalCardMapper.fromDto(dto));
                    }

                    if (mapped.isEmpty()) {
                        // Сервер отдал только дубликаты (например, из-за offset/фильтров)
                        // Двигаем offset и пробуем ещё раз один раз.
                        offset += data.size();
                        isLoading = false;
                        loadNextPageIfNeeded(false);
                        return;
                    }

                    offset += data.size();

                    cards.addAll(mapped);

                    CardAdapter a = adapter;
                    PatureStackLayout s = stack;
                    if (a != null && s != null) {
                        a.setItems(cards);
                        s.setAdapter(a); // простой и надёжный ре-рендер
                    }

                    isLoading = false;
                }
        );
    }

    @NonNull
    private static <T> String buildRepoError(@NonNull String op, @NonNull AnimalRepository.RepoResult<T> result) {
        StringBuilder sb = new StringBuilder();
        sb.append(op).append(": ");
        if (result.httpCode != null) {
            sb.append("http=").append(result.httpCode).append(" ");
        }
        if (result.errorMessage != null) {
            sb.append("msg=").append(result.errorMessage).append(" ");
        }
        if (result.errorBody != null) {
            sb.append("body=").append(result.errorBody);
        }
        return sb.toString().trim();
    }
}
