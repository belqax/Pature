package app.belqax.pature.model;

import androidx.annotation.NonNull;

public class ChatThread {

    private final String id;
    private final String title;
    private final String lastMessage;
    private final long lastMessageTimeMillis;
    private final int unreadCount;
    private final boolean favorite;

    public ChatThread(@NonNull String id,
                      @NonNull String title,
                      @NonNull String lastMessage,
                      long lastMessageTimeMillis,
                      int unreadCount,
                      boolean favorite) {
        this.id = id;
        this.title = title;
        this.lastMessage = lastMessage;
        this.lastMessageTimeMillis = lastMessageTimeMillis;
        this.unreadCount = unreadCount;
        this.favorite = favorite;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    @NonNull
    public String getLastMessage() {
        return lastMessage;
    }

    public long getLastMessageTimeMillis() {
        return lastMessageTimeMillis;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public boolean hasUnread() {
        return unreadCount > 0;
    }
}
