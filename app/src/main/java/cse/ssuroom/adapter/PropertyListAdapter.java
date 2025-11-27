package cse.ssuroom.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import cse.ssuroom.R;
import cse.ssuroom.database.LeaseTransfer;
import cse.ssuroom.database.Property;
import cse.ssuroom.database.ShortTerm;
import cse.ssuroom.fragment.RoomDetailFragment;
import cse.ssuroom.user.User;

public class PropertyListAdapter extends RecyclerView.Adapter<PropertyListAdapter.PropertyViewHolder> {

    private final Context context;
    private final List<Property> properties;
    private final int layoutId;
    private final OnMapButtonClickListener onMapButtonClickListener;
    private final String currentUid;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    public interface OnMapButtonClickListener {
        void onMapButtonClick(Property property);
    }

    public PropertyListAdapter(Context context, List<Property> properties, int layoutId,
                               String currentUid, OnMapButtonClickListener onMapButtonClickListener) {
        this.context = context;
        this.properties = properties;
        this.layoutId = layoutId;
        this.currentUid = currentUid;  // 이제 클래스 필드에 저장됨
        this.onMapButtonClickListener = onMapButtonClickListener;
    }


    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Property property = properties.get(position);
        holder.bind(property);
    }

    @Override
    public int getItemCount() {
        return properties.size();
    }

    class PropertyViewHolder extends RecyclerView.ViewHolder {

        // Common Views
        private final ShapeableImageView roomImage;
        private final ImageView favoriteIcon;
        private final TextView roomTitle;
        private final TextView roomPrice;
        private TextView roomDetails;
        private TextView roomLocation;
        private TextView roomDuration;
        private TextView roomScore;
        private Button viewOnMapButton;
        private TextView propertyTypeBadge;

        private Button deleteButton;

        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);

            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            roomTitle = itemView.findViewById(R.id.room_title);
            roomPrice = itemView.findViewById(R.id.room_price);
            roomImage = itemView.findViewById(R.id.room_image);
            roomLocation = itemView.findViewById(R.id.room_location);
            roomScore = itemView.findViewById(R.id.room_score);
            viewOnMapButton = itemView.findViewById(R.id.view_on_map_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            roomDetails = itemView.findViewById(R.id.room_details);

        }

        public void bind(Property property) {
            roomTitle.setText(property.getTitle());
            roomPrice.setText(getPriceText(property));

            // 🔹 삭제 버튼 안전하게 처리
            Button deleteButton = itemView.findViewById(R.id.delete_button);
            if (deleteButton != null) { // 버튼이 있는 경우만
                if (currentUser != null && property.getHostId() != null
                        && property.getHostId().equals(currentUser.getUid())) {
                    deleteButton.setVisibility(View.VISIBLE);
                    deleteButton.setOnClickListener(v -> showDeleteDialog(property));
                } else {
                    deleteButton.setVisibility(View.GONE);
                }
            }



            // 매뮬 이미지 설정
            if (property.getPhotos() != null && !property.getPhotos().isEmpty()) {
                Glide.with(context)
                        .load(property.getPhotos().get(0))
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(roomImage);
            } else {
                roomImage.setImageResource(R.drawable.ic_launcher_background);
            }
            // 매물 위치 설정
            if (property.getLocation() != null) {
                String address = (String) property.getLocation().get("address");
                roomLocation.setText(address != null ? address : "위치 정보 없음");
            } else {
                roomLocation.setText("위치 정보 없음");
            }

            // 슈방 점수 설정
            if (property.getScores() != null) {
                Object overallScore = property.getScores().get("overall");
                if (overallScore != null) {
                    roomScore.setText(String.format("%.0f", ((Number) overallScore).doubleValue()));
                } else {
                    roomScore.setText("0");
                }
            } else {
                roomScore.setText("0");
            }
            // 지도보기 버튼
            viewOnMapButton.setOnClickListener(v -> {
                if (onMapButtonClickListener != null) {
                    onMapButtonClickListener.onMapButtonClick(property);
                }
            });
            // 즐겨찾기 설정
            updateFavoriteIcon(property.getPropertyId());
            favoriteIcon.setOnClickListener(v -> toggleFavorite(property.getPropertyId()));
            // 매물 선택 시 상세 페이지로 이동
            itemView.setOnClickListener(v -> {
                RoomDetailFragment fragment = RoomDetailFragment.newInstance(property.getPropertyId());
                fragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "RoomDetail");
            });

            if (layoutId == R.layout.item_room_list) {
                String details = String.format("%s · %.0f평 (약 %.0f㎡) · %d층",
                        property.getRoomType(),
                        property.getArea() / 3.3,
                        property.getArea(),
                        property.getFloor());
                if (roomDetails != null) {
                    roomDetails.setText(details);
                }

            }
        }
        private String getPriceText(Property property) {
            if (property.getPricing() == null) {
                return "가격 정보 없음";
            }

            String type = (String) property.getPricing().get("type");
            NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);

            if ("short_term".equals(type)) {
                Object weeklyPrice = property.getPricing().get("weeklyPrice");
                if (weeklyPrice != null) {
                    return formatter.format(weeklyPrice) + "원/주";
                }
            } else if ("lease_transfer".equals(type)) {
                Object deposit = property.getPricing().get("deposit");
                Object monthlyRent = property.getPricing().get("monthlyRent");

                if (deposit != null && monthlyRent != null) {
                    return String.format("보증금 %s / 월세 %s",
                            formatter.format(deposit),
                            formatter.format(monthlyRent));
                }
            }
            return "가격 정보 없음";
        }

        private void toggleFavorite(String propertyId) {
            if (currentUser == null) {
                Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentReference userRef = db.collection("users").document(currentUser.getUid());

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (!documentSnapshot.exists()) return;
                User user = documentSnapshot.toObject(User.class);
                if (user == null || user.getFavorites() == null) return;

                if (user.getFavorites().contains(propertyId)) {
                    userRef.update("favorites", FieldValue.arrayRemove(propertyId))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "즐겨찾기에서 제거했습니다.", Toast.LENGTH_SHORT).show();
                                favoriteIcon.setImageResource(R.drawable.ic_favor);
                            });
                } else {
                    userRef.update("favorites", FieldValue.arrayUnion(propertyId))
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "즐겨찾기에 추가했습니다.", Toast.LENGTH_SHORT).show();
                                favoriteIcon.setImageResource(R.drawable.ic_favor_filled);
                            });
                }
            });
        }
        
        private void updateFavoriteIcon(String propertyId) {
            if (currentUser == null) {
                favoriteIcon.setImageResource(R.drawable.ic_favor);
                return;
            }
            db.collection("users").document(currentUser.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) return;
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getFavorites() != null && user.getFavorites().contains(propertyId)) {
                            favoriteIcon.setImageResource(R.drawable.ic_favor_filled);
                        } else {
                            favoriteIcon.setImageResource(R.drawable.ic_favor);
                        }
                    });
        }
        private void showDeleteDialog(Property property) {
            new AlertDialog.Builder(context)
                    .setTitle("매물 삭제")
                    .setMessage("정말 이 매물을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> deleteProperty(property))
                    .setNegativeButton("취소", null)
                    .show();
        }

        private void deleteProperty(Property property) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 🔥 매물 삭제
            db.collection("properties")
                    .document(property.getPropertyId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {

                        // 리스트에서 제거
                        int index = properties.indexOf(property);
                        if (index != -1) {
                            properties.remove(index);
                            notifyItemRemoved(index);
                        }

                        Toast.makeText(context, "매물이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

    }
}