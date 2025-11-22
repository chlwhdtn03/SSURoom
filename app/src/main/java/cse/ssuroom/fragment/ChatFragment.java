package cse.ssuroom.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import cse.ssuroom.R;
import cse.ssuroom.adapter.ChatAdapter;
import cse.ssuroom.chat.ChatRoom;

public class ChatFragment extends Fragment implements ChatAdapter.OnChatItemClickListener {

    private static final String TAG = "ChatFragment";

    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<ChatRoom> chatRooms = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        recyclerView = view.findViewById(R.id.chat_recycler_view);
        setupRecyclerView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadChatRooms();
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(chatRooms, this, getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // swipe해서 채팅방 지우고
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ChatRoom chatRoom = chatRooms.get(position);
                deleteChatRoom(chatRoom.getChatRoomId());
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void deleteChatRoom(String chatRoomId) {
        db.collection("chat_rooms").document(chatRoomId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "채팅방이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    // The snapshot listener in loadChatRooms will automatically update the list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "채팅방 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Error deleting document", e);
                });
    }

    private void loadChatRooms() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("chat_rooms")
                .whereArrayContains("userIds", currentUser.getUid())
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        chatRooms.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            ChatRoom chatRoom = doc.toObject(ChatRoom.class);
                            chatRoom.setChatRoomId(doc.getId());
                            chatRooms.add(chatRoom);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onChatItemClick(String chatRoomId) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.screen, ChatRoomFragment.newInstance(chatRoomId))
                .addToBackStack(null)
                .commit();
    }
}
