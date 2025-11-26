package cse.ssuroom.fragment;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private final List<ChatRoom> chatRooms = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private SwipeRefreshLayout swipeRefreshLayout;

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
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        setupRecyclerView();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout.setOnRefreshListener(() -> loadChatRooms());
        loadChatRooms();
    }

    private void setupRecyclerView() {
        adapter = new ChatAdapter(chatRooms, this, getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // swipe해서 채팅방 지우기 (왼쪽 스와이프만 허용)
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            private final ColorDrawable background = new ColorDrawable(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
            private final Paint textPaint = new Paint();
            private final String deleteText = "삭제";

            {
                textPaint.setColor(ContextCompat.getColor(requireContext(), android.R.color.white));
                textPaint.setTextSize(48f); // 텍스트 크기 설정
                textPaint.setAntiAlias(true);
                textPaint.setTextAlign(Paint.Align.CENTER);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ChatRoom chatRoom = chatRooms.get(position);
                    deleteChatRoom(chatRoom.getChatRoomId());
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;

                // 배경 그리기
                if (dX < 0) { // 왼쪽으로 스와이프
                    background.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(),
                            itemView.getRight(), itemView.getBottom());
                } else { // 스와이프하지 않은 상태
                    background.setBounds(0, 0, 0, 0);
                }
                background.draw(c);

                // 텍스트 그리기 (왼쪽으로 스와이프할 때만)
                if (dX < 0) {
                    // 텍스트 위치 계산 (세로 중앙 정렬)
                    float textY = itemView.getTop() + (itemView.getHeight() / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f);
                    // 텍스트 위치 계산 (가로 중앙 정렬)
                    float textX = itemView.getRight() + dX / 2;
                    c.drawText(deleteText, textX, textY, textPaint);
                }
            }
        }).attachToRecyclerView(recyclerView);
    }

    private void deleteChatRoom(String chatRoomId) {
        db.collection("chat_rooms").document(chatRoomId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "채팅방이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "채팅방 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Error deleting document", e);
                });
    }

    public void loadChatRooms() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        swipeRefreshLayout.setRefreshing(true);

        db.collection("chat_rooms")
                .whereArrayContains("userIds", currentUser.getUid())
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    swipeRefreshLayout.setRefreshing(false); // Stop refreshing animation
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
