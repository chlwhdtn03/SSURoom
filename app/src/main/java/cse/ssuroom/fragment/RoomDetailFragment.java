package cse.ssuroom.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cse.ssuroom.MainActivity;
import cse.ssuroom.R;
import cse.ssuroom.adapter.ImageSliderAdapter;
import cse.ssuroom.user.User;

public class RoomDetailFragment extends DialogFragment {   // ⭐ BottomSheet → DialogFragment

    private static final String ARG_ROOM_ID = "ROOM_ID";
    private static final String TAG = "RoomDetailFragment";

    private ViewPager2 viewPagerImages;
    private TextView tvPrice, tvTitle, tvAddress, tvHostName;
    private ImageButton btnFavorite, btnClose;
    private MaterialButton btnChat;
    private ImageView ivHostProfile;

    private String roomId;
    private String hostId;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    public static RoomDetailFragment newInstance(String roomId) {
        RoomDetailFragment fragment = new RoomDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ROOM_ID, roomId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (getArguments() != null) {
            roomId = getArguments().getString(ARG_ROOM_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // ⭐ 팝업 배경 dim 적용
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppDialogTheme);

        View view = inflater.inflate(R.layout.fragment_property_detail, container, false);

        initViews(view);
        loadRoomDetails();
        setupListeners();

        return view;
    }

    // ⭐ 팝업 사이즈/배경 등 설정
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {

            // 화면 90% 가로 크기
            getDialog().getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );

            // 배경 둥글게 적용
            getDialog().getWindow().setBackgroundDrawableResource(R.drawable.bg_round_dialog);

            // 배경 어둡게 (dim)
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getDialog().getWindow().setDimAmount(0.4f);
        }
    }


    private void initViews(View view) {
        viewPagerImages = view.findViewById(R.id.viewPagerImages);
        tvPrice = view.findViewById(R.id.tvPrice);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvAddress = view.findViewById(R.id.tvAddress);
        tvHostName = view.findViewById(R.id.tvHostName);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnClose = view.findViewById(R.id.btnClose);
        btnChat = view.findViewById(R.id.btnChat);
        ivHostProfile = view.findViewById(R.id.ivHostProfile);
    }

    private void loadRoomDetails() {

        db.collection("short_terms").document(roomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        displayPropertyData(documentSnapshot);
                    } else {
                        db.collection("lease_transfers").document(roomId)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        displayPropertyData(doc);
                                    } else {
                                        Toast.makeText(getContext(), "매물을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "매물 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                    Log.e("RoomDetail", "Error loading room", e);
                });
    }

    private void displayPropertyData(com.google.firebase.firestore.DocumentSnapshot documentSnapshot) {
        String title = documentSnapshot.getString("title");

        // 주소
        String address = "";
        if (documentSnapshot.contains("location")) {
            Object locationObj = documentSnapshot.get("location");
            if (locationObj instanceof java.util.Map) {
                java.util.Map<String, Object> location = (java.util.Map<String, Object>) locationObj;
                address = (String) location.get("address");
            }
        }

        // 가격
        String priceText = "가격 정보 없음";
        if (documentSnapshot.contains("pricing")) {
            Object pricingObj = documentSnapshot.get("pricing");
            if (pricingObj instanceof java.util.Map) {
                java.util.Map<String, Object> pricing = (java.util.Map<String, Object>) pricingObj;
                String type = (String) pricing.get("type");

                if ("short_term".equals(type)) {
                    Object weeklyPrice = pricing.get("weeklyPrice");
                    if (weeklyPrice != null)
                        priceText = String.format("%,d원/주", ((Number) weeklyPrice).longValue());

                } else if ("lease_transfer".equals(type)) {
                    Object deposit = pricing.get("deposit");
                    Object monthlyRent = pricing.get("monthlyRent");
                    if (deposit != null && monthlyRent != null)
                        priceText = String.format("보증금 %,d만원 / 월세 %,d만원",
                                ((Number) deposit).longValue(),
                                ((Number) monthlyRent).longValue());
                }
            }
        }

        hostId = documentSnapshot.getString("hostId");

        tvTitle.setText(title != null ? title : "제목 없음");
        tvAddress.setText(address != null ? address : "주소 정보 없음");
        tvPrice.setText(priceText);

        List<String> photos = (List<String>) documentSnapshot.get("photos");
        if (photos != null && !photos.isEmpty()) {
            setupImageSlider(photos);
        }

        if (hostId != null) {
            loadHostInfo(hostId);
        }

        checkFavoriteStatus();
    }

    private void loadHostInfo(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        tvHostName.setText(name != null ? name : "익명");

                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_myinfo)
                                    .error(R.drawable.ic_myinfo)
                                    .into(ivHostProfile);
                        } else {
                            Glide.with(this)
                                    .load(R.drawable.ic_myinfo)
                                    .circleCrop()
                                    .into(ivHostProfile);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading host info", e);
                    tvHostName.setText("익명");
                    ivHostProfile.setImageResource(R.drawable.ic_myinfo);
                });
    }

    private void setupImageSlider(List<String> imageUrls) {
        ImageSliderAdapter adapter = new ImageSliderAdapter(getContext(), imageUrls);
        viewPagerImages.setAdapter(adapter);
    }

    private void checkFavoriteStatus() {
        if (currentUser == null) {
            btnFavorite.setImageResource(R.drawable.ic_favor);
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null && user.getFavorites() != null && user.getFavorites().contains(roomId)) {
                        btnFavorite.setImageResource(R.drawable.ic_favor_filled);
                    } else {
                        btnFavorite.setImageResource(R.drawable.ic_favor);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking favorite status", e));
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> dismiss());

        btnFavorite.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            saveFavoriteStatus();
        });
        // 채팅하기 기능 추가 - MJ
        btnChat.setOnClickListener(v -> {
            if (currentUser == null) {
                Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hostId.equals(currentUser.getUid())) {
                Toast.makeText(getContext(), "자신과는 채팅할 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            startChat();
        });
    }

    private void startChat() {
        String currentUserId = currentUser.getUid();

        db.collection("chat_rooms")
                .whereArrayContains("userIds", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    String existingChatRoomId = null;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        List<String> userIds = (List<String>) doc.get("userIds");
                        String property = doc.getString("propertyId");
                        if (userIds != null && userIds.contains(hostId) && roomId.equals(property)) {
                            existingChatRoomId = doc.getId();
                            break;
                        }
                    }

                    if (existingChatRoomId != null) {
                        // 이미 채팅방이 있는 상황 -> 채팅방으로 이동
                        navigateToChatRoom(existingChatRoomId);
                    } else {
                        // 새로운 채팅방 생성
                        cse.ssuroom.chat.ChatRoom newChatRoom = new cse.ssuroom.chat.ChatRoom();
                        newChatRoom.setUserIds(java.util.Arrays.asList(currentUserId, hostId));
                        newChatRoom.setPropertyId(roomId);
                        newChatRoom.setLastMessage("");

                        db.collection("chat_rooms")
                                .add(newChatRoom)
                                .addOnSuccessListener(documentReference -> {
                                    navigateToChatRoom(documentReference.getId());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error creating chat room", e);
                                    Toast.makeText(getContext(), "채팅방을 생성하는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }

    private void navigateToChatRoom(String chatRoomId) {
        // Dismiss this fragment first
        dismiss();

        // Navigate to ChatRoomFragment
        ChatRoomFragment chatRoomFragment = ChatRoomFragment.newInstance(chatRoomId);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.screen, chatRoomFragment)
                .addToBackStack(null)
                .commit();
    }

    private void saveFavoriteStatus() {
        if (currentUser == null) {
            Log.e(TAG, "saveFavoriteStatus: currentUser is null. This should not happen.");
            return;
        }
        DocumentReference userRef = db.collection("users").document(currentUser.getUid());

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Log.e(TAG, "saveFavoriteStatus: User document does not exist.");
                return;
            }
            User user = documentSnapshot.toObject(User.class);
            if (user == null || user.getFavorites() == null) {
                // Initialize favorites list if null, or handle as error
                if (user != null) {
                    user.setFavorites(new ArrayList<>());
                } else {
                    Log.e(TAG, "saveFavoriteStatus: User object is null after toObject conversion.");
                    return;
                }
            }

            if (user.getFavorites().contains(roomId)) {
                // 즐겨찾기 제거
                userRef.update("favorites", FieldValue.arrayRemove(roomId))
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "즐겨찾기에서 제거되었습니다.", Toast.LENGTH_SHORT).show();
                            btnFavorite.setImageResource(R.drawable.ic_favor);
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Error removing favorite", e));
            } else {
                // 즐겨찾기 추가
                userRef.update("favorites", FieldValue.arrayUnion(roomId))
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "즐겨찾기에 추가되었습니다.", Toast.LENGTH_SHORT).show();
                            btnFavorite.setImageResource(R.drawable.ic_favor_filled);
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Error adding favorite", e));
            }
        })
        .addOnFailureListener(e -> Log.e(TAG, "Error fetching user document for favorite status", e));
    }
}

