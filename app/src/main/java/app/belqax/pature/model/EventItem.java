package app.belqax.pature.model;

import androidx.annotation.NonNull;

public class EventItem {

    public enum EventType {
        NEW_CONTACT,    // бывший "match"
        MESSAGE,
        UPDATE,
        REMINDER
    }

    private final String id;
    private final EventType type;
    private final String title;
    private final String description;
    private final long timeMillis;
    private final boolean unread;

    public EventItem(@NonNull String id,
                     @NonNull EventType type,
                     @NonNull String title,
                     @NonNull String description,
                     long timeMillis,
                     boolean unread) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.timeMillis = timeMillis;
        this.unread = unread;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public EventType getType() {
        return type;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getDescription() {
        return description;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public boolean isUnread() {
        return unread;
    }
}
