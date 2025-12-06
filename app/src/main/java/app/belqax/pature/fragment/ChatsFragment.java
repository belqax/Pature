package app.belqax.pature.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.belqax.pature.R;
import app.belqax.pature.adapter.ChatsAdapter;
import app.belqax.pature.model.ChatThread;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ChatsAdapter adapter;

    private final List<ChatThread> allChats = new ArrayList<>();
    private final List<ChatThread> filteredChats = new ArrayList<>();

    private boolean filterOnlyUnread = false;
    private boolean filterOnlyFavorites = false;

    private enum SortMode {
        NEWEST,
        OLDEST,
        NAME
    }

    private SortMode sortMode = SortMode.NEWEST;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.chatsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ChatsAdapter(chatThread -> {
            // TODO: открыть экран диалога
            // openChat(chatThread);
        });
        recyclerView.setAdapter(adapter);

        View filterButton = view.findViewById(R.id.chatsFilterButton);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showChatFiltersBottomSheet());
        }

        loadMockChats();
        applyFiltersAndUpdateList();
    }

    private void loadMockChats() {
        allChats.clear();

        long now = System.currentTimeMillis();

        allChats.add(new ChatThread(
                "1",
                "Приют «Снежа»",
                "Спасибо, что откликнулись!",
                now - 1000L * 60 * 5,
                3,
                true
        ));

        allChats.add(new ChatThread(
                "2",
                "Хозяйка Лунтика",
                "Мы готовы встретиться в субботу.",
                now - 1000L * 60 * 60,
                0,
                false
        ));

        allChats.add(new ChatThread(
                "3",
                "Приют «Добрые лапы»",
                "Отправили вам ещё фото.",
                now - 1000L * 60 * 30,
                1,
                false
        ));

        allChats.add(new ChatThread(
                "4",
                "Полина",
                "Как там наш пёс?",
                now - 1000L * 60 * 60 * 24,
                0,
                true
        ));
    }

    private void applyFiltersAndUpdateList() {
        filteredChats.clear();

        for (ChatThread chat : allChats) {
            if (filterOnlyUnread && !chat.hasUnread()) {
                continue;
            }
            if (filterOnlyFavorites && !chat.isFavorite()) {
                continue;
            }
            filteredChats.add(chat);
        }

        switch (sortMode) {
            case NEWEST:
                Collections.sort(filteredChats,
                        (o1, o2) -> Long.compare(o2.getLastMessageTimeMillis(), o1.getLastMessageTimeMillis()));
                break;
            case OLDEST:
                Collections.sort(filteredChats,
                        Comparator.comparingLong(ChatThread::getLastMessageTimeMillis));
                break;
            case NAME:
                Collections.sort(filteredChats,
                        (o1, o2) -> o1.getTitle().compareToIgnoreCase(o2.getTitle()));
                break;
            default:
                break;
        }

        adapter.setItems(filteredChats);
    }

    private void showChatFiltersBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());

        View contentView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_chat_filters, null, false);

        dialog.setContentView(contentView);

        MaterialCheckBox onlyUnreadCheckBox =
                contentView.findViewById(R.id.onlyUnreadCheckBox);
        MaterialCheckBox onlyFavoritesCheckBox =
                contentView.findViewById(R.id.onlyFavoritesCheckBox);
        ChipGroup sortChipGroup = contentView.findViewById(R.id.sortChipGroup);
        MaterialButton resetButton =
                contentView.findViewById(R.id.resetChatFiltersButton);
        MaterialButton applyButton =
                contentView.findViewById(R.id.applyChatFiltersButton);

        if (onlyUnreadCheckBox != null) {
            onlyUnreadCheckBox.setChecked(filterOnlyUnread);
        }
        if (onlyFavoritesCheckBox != null) {
            onlyFavoritesCheckBox.setChecked(filterOnlyFavorites);
        }
        if (sortChipGroup != null) {
            switch (sortMode) {
                case NEWEST:
                    sortChipGroup.check(R.id.chipSortNewest);
                    break;
                case OLDEST:
                    sortChipGroup.check(R.id.chipSortOldest);
                    break;
                case NAME:
                    sortChipGroup.check(R.id.chipSortByName);
                    break;
                default:
                    sortChipGroup.check(R.id.chipSortNewest);
                    break;
            }
        }

        if (resetButton != null) {
            resetButton.setOnClickListener(v -> {
                if (onlyUnreadCheckBox != null) {
                    onlyUnreadCheckBox.setChecked(false);
                }
                if (onlyFavoritesCheckBox != null) {
                    onlyFavoritesCheckBox.setChecked(false);
                }
                if (sortChipGroup != null) {
                    sortChipGroup.check(R.id.chipSortNewest);
                }
            });
        }

        if (applyButton != null) {
            applyButton.setOnClickListener(v -> {
                if (onlyUnreadCheckBox != null) {
                    filterOnlyUnread = onlyUnreadCheckBox.isChecked();
                }
                if (onlyFavoritesCheckBox != null) {
                    filterOnlyFavorites = onlyFavoritesCheckBox.isChecked();
                }

                if (sortChipGroup != null) {
                    int checkedId = sortChipGroup.getCheckedChipId();
                    if (checkedId == R.id.chipSortOldest) {
                        sortMode = SortMode.OLDEST;
                    } else if (checkedId == R.id.chipSortByName) {
                        sortMode = SortMode.NAME;
                    } else {
                        sortMode = SortMode.NEWEST;
                    }
                }

                applyFiltersAndUpdateList();
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}
