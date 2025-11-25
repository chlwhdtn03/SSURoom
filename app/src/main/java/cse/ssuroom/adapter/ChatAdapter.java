package cse.ssuroom.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import cse.ssuroom.R;
import cse.ssuroom.chat.ChatRoom;
import cse.ssuroom.user.User;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatRoom> chatRooms;
    private final OnChatItemClickListener listener;
    private final Context context;

    public interface OnChatItemClickListener {
        void onChatItemClick(String chatRoomId);
    }

    public ChatAdapter(List<ChatRoom> chatRooms, OnChatItemClickListener listener, Context context) {
        this.chatRooms = chatRooms;
        this.listener = listener;
        this.context = context;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatRoom chatRoom = chatRooms.get(position);
        holder.bind(chatRoom);
        holder.itemView.setOnClickListener(v -> listener.onChatItemClick(chatRoom.getChatRoomId()));
    }

    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoomName;
        TextView tvLastMessage;
        TextView tvLastTimestamp;
        ImageView ivProfile;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoomName = itemView.findViewById(R.id.tv_room_name_opponent_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            tvLastTimestamp = itemView.findViewById(R.id.tv_last_timestamp);
            ivProfile = itemView.findViewById(R.id.iv_profile);
        }

        public void bind(ChatRoom chatRoom) {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String otherUserId = null;

            for (String userId : chatRoom.getUserIds()) {
                if (!userId.equals(currentUserId)) {
                    otherUserId = userId;
                    break;
                }
            }

            if (otherUserId != null) {
                FirebaseFirestore.getInstance().collection("users").document(otherUserId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                User user = documentSnapshot.toObject(User.class);
                                if (user != null) {
                                    tvRoomName.setText(user.getName());
                                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                        Glide.with(context)
                                                .load(user.getProfileImageUrl())
                                                .circleCrop()
                                                .into(ivProfile);
                                    } else {
                                        Glide.with(context)
                                                .load(R.drawable.ic_profile_placeholder)
                                                .circleCrop()
                                                .into(ivProfile);
                                    }
                                }
                            }
                        });
            }


            tvLastMessage.setText(chatRoom.getLastMessage());
            if (chatRoom.getLastTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
                tvLastTimestamp.setText(sdf.format(chatRoom.getLastTimestamp()));
            } else {
                tvLastTimestamp.setText("");
            }
        }
    }
}
