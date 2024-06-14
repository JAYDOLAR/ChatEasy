/*package Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chateasy.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import Models.MessageModel;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENDER = 1;
    private static final int VIEW_TYPE_RECEIVER = 2;

    private final List<MessageModel> messages;
    private final String currentUserId; // Assuming you have a way to get the current user ID

    public MessageAdapter(List<MessageModel> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = messages.get(position);
        return message.getFrom().equals(currentUserId) ? VIEW_TYPE_SENDER : VIEW_TYPE_RECEIVER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == VIEW_TYPE_SENDER) {
            view = inflater.inflate(R.layout.item_container_sent_message, parent, false);
            return new SenderViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.item_container_received_message, parent, false);
            return new ReceiverViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageModel message = messages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENDER) {
            ((SenderViewHolder) holder).bind(message);
        } else {
            ((ReceiverViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // Sender ViewHolder
    public static class SenderViewHolder extends RecyclerView.ViewHolder {
        private final TextView senderMessageTextView;
        private final TextView textDateAndTime;

        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMessageTextView = itemView.findViewById(R.id.textSentMessage);
            textDateAndTime = itemView.findViewById(R.id.textDateAndTime);
        }

        public void bind(MessageModel message) {
            senderMessageTextView.setText(message.getTextMessage());
            textDateAndTime.setText(formatDate(message.getCreatedAt()));
        }
    }

    // Receiver ViewHolder
    public static class ReceiverViewHolder extends RecyclerView.ViewHolder {
        private final TextView receiverMessageTextView;
        private final TextView textDateAndTime;

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            receiverMessageTextView = itemView.findViewById(R.id.textReceivedMessage);
            textDateAndTime = itemView.findViewById(R.id.textDateAndTime);
        }

        public void bind(MessageModel message) {
            receiverMessageTextView.setText(message.getTextMessage());
            textDateAndTime.setText(formatDate(message.getCreatedAt()));
        }
    }

    // Helper method to format timestamp to date-time string
    private static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}*/
package Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chateasy.R;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import Models.MessageModel;

public class MessageAdapter extends FirestoreRecyclerAdapter<MessageModel, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENDER = 1;
    private static final int VIEW_TYPE_RECEIVER = 2;

    private final String currentUserId; // Assuming you have a way to get the current user ID

    public MessageAdapter(@NonNull FirestoreRecyclerOptions<MessageModel> options, String currentUserId) {
        super(options);
        this.currentUserId = currentUserId;
    }

    // Helper method to format timestamp to date-time string
    @NonNull
    private static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull MessageModel message) {
        if (holder.getItemViewType() == VIEW_TYPE_SENDER) {
            ((SenderViewHolder) holder).bind(message);
        } else {
            ((ReceiverViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemViewType(int position) {
        MessageModel message = getItem(position);
        return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENDER : VIEW_TYPE_RECEIVER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        if (viewType == VIEW_TYPE_SENDER) {
            view = inflater.inflate(R.layout.item_container_sent_message, parent, false);
            return new SenderViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.item_container_received_message, parent, false);
            return new ReceiverViewHolder(view);
        }
    }

    // Sender ViewHolder
    public static class SenderViewHolder extends RecyclerView.ViewHolder {
        private final TextView senderMessageTextView;
        private final TextView textDateAndTime;

        public SenderViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMessageTextView = itemView.findViewById(R.id.textSentMessage);
            textDateAndTime = itemView.findViewById(R.id.textDateAndTime);
        }

        public void bind(@NonNull MessageModel message) {
            senderMessageTextView.setText(message.getMessageContent());
            textDateAndTime.setText(formatDate(message.getMessageTimestamp().toDate().getTime()));
        }
    }

    // Receiver ViewHolder
    public static class ReceiverViewHolder extends RecyclerView.ViewHolder {
        private final TextView receiverMessageTextView;
        private final TextView textDateAndTime;

        public ReceiverViewHolder(@NonNull View itemView) {
            super(itemView);
            receiverMessageTextView = itemView.findViewById(R.id.textReceivedMessage);
            textDateAndTime = itemView.findViewById(R.id.textDateAndTime);
        }

        public void bind(@NonNull MessageModel message) {
            receiverMessageTextView.setText(message.getMessageContent());
            textDateAndTime.setText(formatDate(message.getMessageTimestamp().toDate().getTime()));
        }
    }
}
