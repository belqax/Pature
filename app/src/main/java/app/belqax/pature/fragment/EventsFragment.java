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
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import app.belqax.pature.R;
import app.belqax.pature.adapter.EventsAdapter;
import app.belqax.pature.model.EventItem;

public class EventsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventsAdapter adapter;

    private final List<EventItem> allEvents = new ArrayList<>();

    private boolean filterOnlyUnread = false;
    @Nullable
    private EventItem.EventType filterType = null; // null = все

    private enum DayGroup {
        TODAY,
        YESTERDAY,
        EARLIER
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new EventsAdapter(eventItem -> {
            // TODO: обработка клика по событию:
            // - открыть чат
            // - открыть карточку животного
            // - открыть детали напоминания и т.п.
        });
        recyclerView.setAdapter(adapter);

        View filterButton = view.findViewById(R.id.eventsFilterButton);
        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showEventFiltersBottomSheet());
        }

        loadMockEvents();
        applyFiltersAndUpdate();
    }

    private void loadMockEvents() {
        allEvents.clear();

        long now = System.currentTimeMillis();

        allEvents.add(new EventItem(
                "1",
                EventItem.EventType.NEW_CONTACT,
                "Новый контакт",
                "У вас новый контакт с приютом «Снежа». Начните переписку.",
                now - 1000L * 60 * 5,
                true
        ));

        allEvents.add(new EventItem(
                "2",
                EventItem.EventType.MESSAGE,
                "Новое сообщение",
                "Приют «Добрые лапы» отправил вам новое сообщение.",
                now - 1000L * 60 * 30,
                true
        ));

        allEvents.add(new EventItem(
                "3",
                EventItem.EventType.UPDATE,
                "Обновление анкеты",
                "Статус анкеты Снежи обновлён: «Забронирована».",
                now - 1000L * 60 * 60 * 3,
                false
        ));

        // Вчера
        long yesterday = now - 1000L * 60 * 60 * 24;

        allEvents.add(new EventItem(
                "4",
                EventItem.EventType.REMINDER,
                "Напоминание",
                "Вчера была встреча с приютом «Снежа» в 15:00.",
                yesterday + 1000L * 60 * 60 * 10,
                false
        ));

        // Ранее
        long twoDaysAgo = now - 1000L * 60 * 60 * 48;

        allEvents.add(new EventItem(
                "5",
                EventItem.EventType.MESSAGE,
                "Новое сообщение (старое)",
                "Сообщение от приюта «Лучик надежды».",
                twoDaysAgo,
                false
        ));
    }

    private void applyFiltersAndUpdate() {
        List<EventItem> filtered = new ArrayList<>();

        for (EventItem event : allEvents) {
            if (filterOnlyUnread && !event.isUnread()) {
                continue;
            }
            if (filterType != null && event.getType() != filterType) {
                continue;
            }
            filtered.add(event);
        }

        Collections.sort(filtered, new Comparator<EventItem>() {
            @Override
            public int compare(EventItem o1, EventItem o2) {
                return Long.compare(o2.getTimeMillis(), o1.getTimeMillis());
            }
        });

        List<EventsAdapter.EventListItem> uiItems = buildSectionedList(filtered);
        adapter.setItems(uiItems);
    }

    @NonNull
    private List<EventsAdapter.EventListItem> buildSectionedList(@NonNull List<EventItem> events) {
        List<EventsAdapter.EventListItem> result = new ArrayList<>();

        DayGroup lastGroup = null;

        for (EventItem event : events) {
            DayGroup group = resolveDayGroup(event.getTimeMillis());

            if (group != lastGroup) {
                String headerTitle;
                switch (group) {
                    case TODAY:
                        headerTitle = getString(R.string.events_section_today);
                        break;
                    case YESTERDAY:
                        headerTitle = getString(R.string.events_section_yesterday);
                        break;
                    case EARLIER:
                    default:
                        headerTitle = getString(R.string.events_section_earlier);
                        break;
                }
                result.add(EventsAdapter.EventListItem.header(headerTitle));
                lastGroup = group;
            }

            result.add(EventsAdapter.EventListItem.event(event));
        }

        return result;
    }

    @NonNull
    private DayGroup resolveDayGroup(long timeMillis) {
        Calendar now = Calendar.getInstance();
        Calendar eventCal = Calendar.getInstance();
        eventCal.setTimeInMillis(timeMillis);

        if (isSameDay(eventCal, now)) {
            return DayGroup.TODAY;
        }

        Calendar yesterday = (Calendar) now.clone();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        if (isSameDay(eventCal, yesterday)) {
            return DayGroup.YESTERDAY;
        }

        return DayGroup.EARLIER;
    }

    private boolean isSameDay(@NonNull Calendar c1, @NonNull Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private void showEventFiltersBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());

        View contentView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_event_filters, null, false);

        dialog.setContentView(contentView);

        MaterialCheckBox onlyUnreadCheckBox =
                contentView.findViewById(R.id.onlyUnreadEventsCheckBox);
        ChipGroup typeChipGroup =
                contentView.findViewById(R.id.eventTypeChipGroup);
        MaterialButton resetButton =
                contentView.findViewById(R.id.resetEventFiltersButton);
        MaterialButton applyButton =
                contentView.findViewById(R.id.applyEventFiltersButton);

        if (onlyUnreadCheckBox != null) {
            onlyUnreadCheckBox.setChecked(filterOnlyUnread);
        }

        if (typeChipGroup != null) {
            if (filterType == null) {
                typeChipGroup.check(R.id.chipEventTypeAll);
            } else {
                switch (filterType) {
                    case NEW_CONTACT:
                        typeChipGroup.check(R.id.chipEventTypeNewContacts);
                        break;
                    case MESSAGE:
                        typeChipGroup.check(R.id.chipEventTypeMessages);
                        break;
                    case UPDATE:
                        typeChipGroup.check(R.id.chipEventTypeUpdates);
                        break;
                    case REMINDER:
                        typeChipGroup.check(R.id.chipEventTypeReminders);
                        break;
                    default:
                        typeChipGroup.check(R.id.chipEventTypeAll);
                        break;
                }
            }
        }

        if (resetButton != null) {
            resetButton.setOnClickListener(v -> {
                if (onlyUnreadCheckBox != null) {
                    onlyUnreadCheckBox.setChecked(false);
                }
                if (typeChipGroup != null) {
                    typeChipGroup.check(R.id.chipEventTypeAll);
                }
            });
        }

        if (applyButton != null) {
            applyButton.setOnClickListener(v -> {
                if (onlyUnreadCheckBox != null) {
                    filterOnlyUnread = onlyUnreadCheckBox.isChecked();
                }

                if (typeChipGroup != null) {
                    int checkedId = typeChipGroup.getCheckedChipId();
                    if (checkedId == R.id.chipEventTypeNewContacts) {
                        filterType = EventItem.EventType.NEW_CONTACT;
                    } else if (checkedId == R.id.chipEventTypeMessages) {
                        filterType = EventItem.EventType.MESSAGE;
                    } else if (checkedId == R.id.chipEventTypeUpdates) {
                        filterType = EventItem.EventType.UPDATE;
                    } else if (checkedId == R.id.chipEventTypeReminders) {
                        filterType = EventItem.EventType.REMINDER;
                    } else {
                        filterType = null;
                    }
                }

                applyFiltersAndUpdate();
                dialog.dismiss();
            });
        }

        dialog.show();
    }
}
