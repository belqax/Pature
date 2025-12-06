package app.belqax.pature.adapter;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import app.belqax.pature.R;
import app.belqax.pature.model.ChatThread;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatsAdapter extends RecyclerView.Adapter<ChatsAdapter.ChatViewHolder> {

    public interface OnChatClickListener {
        void onChatClick(@NonNull ChatThread chatThread);
    }

    private final List<ChatThread> items = new ArrayList<>();
    private final OnChatClickListener onChatClickListener;
    private final SimpleDateFormat timeFormatter =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    public ChatsAdapter(@NonNull OnChatClickListener onChatClickListener) {
        this.onChatClickListener = onChatClickListener;
    }

    public void setItems(@NonNull List<ChatThread> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_thread, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatThread item = items.get(position);
        holder.bind(item, onChatClickListener, timeFormatter);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {

        private final ImageView avatarView;
        private final TextView titleView;
        private final TextView lastMessageView;
        private final TextView timeView;
        private final TextView unreadBadgeView;
        private final ImageView favoriteIconView;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.chatAvatar);
            titleView = itemView.findViewById(R.id.chatTitle);
            lastMessageView = itemView.findViewById(R.id.chatLastMessage);
            timeView = itemView.findViewById(R.id.chatTime);
            unreadBadgeView = itemView.findViewById(R.id.unreadBadge);
            favoriteIconView = itemView.findViewById(R.id.chatFavoriteIcon);
        }

        void bind(@NonNull ChatThread chatThread,
                  @NonNull OnChatClickListener onChatClickListener,
                  @NonNull SimpleDateFormat timeFormatter) {

            titleView.setText(chatThread.getTitle());
            lastMessageView.setText(chatThread.getLastMessage());

            Date date = new Date(chatThread.getLastMessageTimeMillis());
            timeView.setText(timeFormatter.format(date));

            if (chatThread.getUnreadCount() > 0) {
                unreadBadgeView.setVisibility(View.VISIBLE);
                unreadBadgeView.setText(String.valueOf(chatThread.getUnreadCount()));
            } else {
                unreadBadgeView.setVisibility(View.GONE);
            }

            if (chatThread.isFavorite()) {
                favoriteIconView.setVisibility(View.VISIBLE);
            } else {
                favoriteIconView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> onChatClickListener.onChatClick(chatThread));
        }
    }
}
