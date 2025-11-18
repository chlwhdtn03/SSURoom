package cse.ssuroom.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import cse.ssuroom.R;
import cse.ssuroom.database.Property;
import cse.ssuroom.fragment.RoomDetailFragment;

public class PropertyListAdapter extends RecyclerView.Adapter<PropertyListAdapter.PropertyViewHolder> {

    private final Context context;
    private final List<Property> properties;

    public PropertyListAdapter(Context context, List<Property> properties) {
        this.context = context;
        this.properties = properties;
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_room_list, parent, false);
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

        private final ShapeableImageView roomImage;
        private final ImageView favoriteIcon;
        private final TextView roomTitle;
        private final TextView roomScoreLabel;
        private final TextView roomScore;
        private final TextView roomLocation;
        private final TextView roomPrice;
        private final TextView roomDetails;
        private final Button viewOnMapButton;

        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);

            roomImage = itemView.findViewById(R.id.room_image);
            favoriteIcon = itemView.findViewById(R.id.favorite_icon);
            roomTitle = itemView.findViewById(R.id.room_title);
            roomScoreLabel = itemView.findViewById(R.id.room_score_label);
            roomScore = itemView.findViewById(R.id.room_score);
            roomLocation = itemView.findViewById(R.id.room_location);
            roomPrice = itemView.findViewById(R.id.room_price);
            roomDetails = itemView.findViewById(R.id.room_details);
            viewOnMapButton = itemView.findViewById(R.id.view_on_map_button);
        }

        public void bind(Property property) {
            // 제목
            roomTitle.setText(property.getTitle());

            // 위치
            if (property.getLocation() != null) {
                String address = (String) property.getLocation().get("address");
                roomLocation.setText(address != null ? address : "위치 정보 없음");
            } else {
                roomLocation.setText("위치 정보 없음");
            }

            // 가격
            String priceText = getPriceText(property);
            roomPrice.setText(priceText);

            // 상세 정보 (방 타입 · 면적 · 층수)
            String details = String.format("%s · %.0f평 (약 %.0f㎡) · %d층",
                    property.getRoomType(),
                    property.getArea() / 3.3,  // ㎡를 평으로 변환
                    property.getArea(),
                    property.getFloor());
            roomDetails.setText(details);

            // 슈방 점수
            if (property.getScores() != null) {
                Object overallScore = property.getScores().get("overall");
                if (overallScore != null) {
                    double score = ((Number) overallScore).doubleValue();
                    roomScore.setText(String.format("%.0f", score * 100)); // 0~1 값을 0~100으로 변환
                } else {
                    roomScore.setText("0");
                }
            } else {
                roomScore.setText("0");
            }

            // 이미지 로드
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

            // 즐겨찾기 아이콘 클릭
            favoriteIcon.setOnClickListener(v -> {
                Toast.makeText(context, "즐겨찾기 기능 준비중", Toast.LENGTH_SHORT).show();
            });

            // 지도보기 버튼 클릭
            viewOnMapButton.setOnClickListener(v -> {
                Toast.makeText(context, "지도보기 기능 준비중", Toast.LENGTH_SHORT).show();
            });

            // ⭐ 아이템 전체 클릭 - 상세 화면으로 이동
            itemView.setOnClickListener(v -> {
                RoomDetailFragment fragment = RoomDetailFragment.newInstance(property.getPropertyId());

                // ⭐ BottomSheet로 표시
                fragment.show(((AppCompatActivity) context).getSupportFragmentManager(), "RoomDetail");
            });
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
                    return String.format("보증금 %s만원 / 월세 %s만원",
                            formatter.format(deposit),
                            formatter.format(monthlyRent));
                }
            }

            return "가격 정보 없음";
        }
    }
}