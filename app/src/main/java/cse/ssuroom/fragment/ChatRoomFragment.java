package cse.ssuroom.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cse.ssuroom.MainActivity;
import cse.ssuroom.R;
import cse.ssuroom.adapter.MessageAdapter;
import cse.ssuroom.chat.ChatRoom;
import cse.ssuroom.chat.Message;
import cse.ssuroom.database.Property;
import cse.ssuroom.user.User;

public class ChatRoomFragment extends Fragment {

    private static final String TAG = "ChatRoomFragment";
    private static final String ARG_CHAT_ROOM_ID = "chat_room_id";

    private String chatRoomId;
    private RecyclerView messagesRecyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messages = new ArrayList<>();
    private EditText messageInput;
    private ImageView sendButton;
    private ImageView backButton;
    private ImageView profileImage, roomImage;
    private TextView userName, roomTitleToolbar, roomTitleCard, viewListingButton;


    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private CollectionReference messagesCollection;

    private Thread thread;

    public static ChatRoomFragment newInstance(String chatRoomId) {
        ChatRoomFragment fragment = new ChatRoomFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CHAT_ROOM_ID, chatRoomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            chatRoomId = getArguments().getString(ARG_CHAT_ROOM_ID);
        }

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (chatRoomId != null) {
            messagesCollection = db.collection("chat_rooms").document(chatRoomId).collection("messages");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        messagesRecyclerView = view.findViewById(R.id.messages_recycler_view);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
        profileImage = view.findViewById(R.id.profile_image);
        roomImage = view.findViewById(R.id.room_image);
        userName = view.findViewById(R.id.user_name);
        roomTitleToolbar = view.findViewById(R.id.room_title_toolbar);
        roomTitleCard = view.findViewById(R.id.room_title_card);
        viewListingButton = view.findViewById(R.id.view_listing_button);
        backButton = view.findViewById(R.id.back_button);

        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        setupRecyclerView();
        setupSendButton();
        loadChatRoomDetails();
        loadMessages();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        messagesRecyclerView.setLayoutManager(layoutManager);
        messagesRecyclerView.setAdapter(messageAdapter);
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        if (messagesCollection == null) return;

        messagesCollection.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        messages.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            messages.add(doc.toObject(Message.class));
                        }
                        messageAdapter.notifyDataSetChanged();
                        if (messages.size() > 0) {
                            messagesRecyclerView.scrollToPosition(messages.size() - 1);
                        }
                    }
                });
    }

    private void loadChatRoomDetails() {
        if (chatRoomId == null) return;

        db.collection("chat_rooms").document(chatRoomId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            ChatRoom chatRoom = doc.toObject(ChatRoom.class);
            if (chatRoom == null) return;

            String propertyId = chatRoom.getPropertyId();
            if (propertyId != null) {
                loadPropertyDetails(propertyId);
            }

            String otherUserId = null;
            if (chatRoom.getUserIds() != null) {
                for (String userId : chatRoom.getUserIds()) {
                    if (!userId.equals(currentUser.getUid())) {
                        otherUserId = userId;
                        break;
                    }
                }
            }
            if (otherUserId != null) {
                loadOtherUserDetails(otherUserId);
            }
        });
    }

    private void loadPropertyDetails(String propertyId) {
        db.collection("short_terms").document(propertyId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                setPropertyDetails(doc);
            } else {
                db.collection("lease_transfers").document(propertyId).get().addOnSuccessListener(leaseDoc -> {
                    if (leaseDoc.exists()) {
                        setPropertyDetails(leaseDoc);
                    }
                });
            }
        });
    }

    private void setPropertyDetails(DocumentSnapshot doc) {
        Property property = doc.toObject(Property.class);
        if (property == null) return;

        roomTitleToolbar.setText(property.getTitle());
        roomTitleCard.setText(property.getTitle());
        if (property.getPhotos() != null && !property.getPhotos().isEmpty()) {
            Glide.with(this).load(property.getPhotos().get(0)).into(roomImage);
        }

        viewListingButton.setOnClickListener(v -> {
            RoomDetailFragment.newInstance(doc.getId()).show(getParentFragmentManager(), "RoomDetailFromChat");
        });
    }

    private void loadOtherUserDetails(String userId) {
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            User user = doc.toObject(User.class);
            if (user == null) return;

            userName.setText(user.getName());
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                Glide.with(this)
                        .load(user.getProfileImageUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(profileImage);
            } else {
                Glide.with(this)
                        .load(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(profileImage);
            }
        });
    }


    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }

        if (currentUser == null || messagesCollection == null) {
            Toast.makeText(getContext(), "메시지를 보낼 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String senderId = currentUser.getUid();
        Message message = new Message(senderId, text);

        messagesCollection.add(message)
                .addOnSuccessListener(documentReference -> {
                    messageInput.setText("");
                    updateChatRoomLastMessage(text);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding message", e);
                    Toast.makeText(getContext(), "메시지 전송 실패", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateChatRoomLastMessage(String messageText) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", messageText);
        updates.put("lastTimestamp", FieldValue.serverTimestamp());

        db.collection("chat_rooms").document(chatRoomId).update(updates)
                .addOnFailureListener(e -> Log.w(TAG, "Error updating chat room last message", e));
    }
}
