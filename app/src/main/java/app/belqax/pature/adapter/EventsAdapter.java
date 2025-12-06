package app.belqax.pature.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.belqax.pature.R;
import app.belqax.pature.model.EventItem;

public class EventsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnEventClickListener {
        void onEventClick(@NonNull EventItem eventItem);
    }

    public static class EventListItem {

        public enum ItemType {
            HEADER,
            EVENT
        }

        @NonNull
        private final ItemType type;
        private final String headerTitle;
        private final EventItem eventItem;

        private EventListItem(@NonNull ItemType type,
                              String headerTitle,
                              EventItem eventItem) {
            this.type = type;
            this.headerTitle = headerTitle;
            this.eventItem = eventItem;
        }

        @NonNull
        public static EventListItem header(@NonNull String title) {
            return new EventListItem(ItemType.HEADER, title, null);
        }

        @NonNull
        public static EventListItem event(@NonNull EventItem eventItem) {
            return new EventListItem(ItemType.EVENT, null, eventItem);
        }

        @NonNull
        public ItemType getType() {
            return type;
        }

        public String getHeaderTitle() {
            return headerTitle;
        }

        public EventItem getEventItem() {
            return eventItem;
        }
    }

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_EVENT = 1;

    private final List<EventListItem> items = new ArrayList<>();
    private final OnEventClickListener onEventClickListener;

    private final SimpleDateFormat timeFormatter =
            new SimpleDateFormat("dd.MM, HH:mm", Locale.getDefault());

    public EventsAdapter(@NonNull OnEventClickListener onEventClickListener) {
        this.onEventClickListener = onEventClickListener;
    }

    public void setItems(@NonNull List<EventListItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        EventListItem item = items.get(position);
        if (item.getType() == EventListItem.ItemType.HEADER) {
            return VIEW_TYPE_HEADER;
        } else {
            return VIEW_TYPE_EVENT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_event_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_event, parent, false);
            return new EventViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {
        EventListItem item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else if (holder instanceof EventViewHolder) {
            ((EventViewHolder) holder).bind(item.getEventItem(), onEventClickListener, timeFormatter);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {

        private final TextView sectionTitleView;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionTitleView = itemView.findViewById(R.id.eventSectionTitle);
        }

        void bind(@NonNull EventListItem item) {
            sectionTitleView.setText(item.getHeaderTitle());
        }
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iconView;
        private final TextView titleView;
        private final TextView descriptionView;
        private final TextView timeView;
        private final View unreadDotView;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.eventIcon);
            titleView = itemView.findViewById(R.id.eventTitle);
            descriptionView = itemView.findViewById(R.id.eventDescription);
            timeView = itemView.findViewById(R.id.eventTime);
            unreadDotView = itemView.findViewById(R.id.eventUnreadDot);
        }

        void bind(@NonNull EventItem eventItem,
                  @NonNull OnEventClickListener onEventClickListener,
                  @NonNull SimpleDateFormat timeFormatter) {

            titleView.setText(eventItem.getTitle());
            descriptionView.setText(eventItem.getDescription());

            Date date = new Date(eventItem.getTimeMillis());
            timeView.setText(timeFormatter.format(date));

            switch (eventItem.getType()) {
                case NEW_CONTACT:
                    iconView.setImageResource(R.drawable.ic_event);
                    break;
                case MESSAGE:
                    iconView.setImageResource(R.drawable.ic_info);
                    break;
                case REMINDER:
                    iconView.setImageResource(R.drawable.ic_info);
                    break;
                case UPDATE:
                default:
                    iconView.setImageResource(R.drawable.ic_info);
                    break;
            }

            if (eventItem.isUnread()) {
                unreadDotView.setVisibility(View.VISIBLE);
            } else {
                unreadDotView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> onEventClickListener.onEventClick(eventItem));
        }
    }
}
