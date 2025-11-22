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
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cse.ssuroom.R;
import cse.ssuroom.database.LeaseTransfer;
import cse.ssuroom.database.ShortTerm;

public class ArPropertyListAdapter extends RecyclerView.Adapter<ArPropertyListAdapter.PropertyViewHolder> {

    private final Context context;
    private List<Object> properties;

    public ArPropertyListAdapter(Context context, List<Object> properties) {
        this.context = context;
        this.properties = properties;
    }

    public void updateProperties(List<Object> newProperties) {
        this.properties.clear();
        this.properties.addAll(newProperties);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_room_list, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Object property = properties.get(position);
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
            viewOnMapButton.setVisibility(View.GONE); // 지도보기 버튼 숨기기
        }

        public void bind(Object property) {
            if (property instanceof LeaseTransfer) {
                bindLease((LeaseTransfer) property);
            } else if (property instanceof ShortTerm) {
                bindShortTerm((ShortTerm) property);
            }
        }

        private void bindLease(LeaseTransfer property) {
            roomTitle.setText(property.getTitle());
            roomLocation.setText((String) property.getLocation().get("address"));

            NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
            String priceText = String.format("보증금 %s / 월세 %s",
                    formatter.format(property.getPricing().get("deposit")),
                    formatter.format(property.getPricing().get("monthlyRent")));
            roomPrice.setText(priceText);

            setDetails(property.getRoomType(), property.getArea(), (long) property.getFloor());
            setScore(property.getScores());
            setImages(property.getPhotos());

            itemView.setOnClickListener(v -> Toast.makeText(context, property.getTitle(), Toast.LENGTH_SHORT).show());
        }

        private void bindShortTerm(ShortTerm property) {
            roomTitle.setText(property.getTitle());
            roomLocation.setText((String) property.getLocation().get("address"));

            NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
            String priceText = formatter.format(property.getPricing().get("weeklyPrice")) + "원/주";
            roomPrice.setText(priceText);

            setDetails(property.getRoomType(), property.getArea(), (long) property.getFloor());
            setScore(property.getScores());
            setImages(property.getPhotos());

            itemView.setOnClickListener(v -> Toast.makeText(context, property.getTitle(), Toast.LENGTH_SHORT).show());
        }

        private void setDetails(String roomType, Double area, Long floor) {
            if (area == null || floor == null) {
                roomDetails.setText(roomType);
                return;
            }
             String details = String.format("%s · %.0f평 (약 %.0f㎡) · %d층",
                    roomType,
                    area / 3.3,
                    area,
                    floor);
            roomDetails.setText(details);
        }

        private void setScore(Map<String, Object> scores) {
            if (scores != null) {
                Object overallScore = scores.get("overall");
                if (overallScore instanceof Number) {
                    double score = ((Number) overallScore).doubleValue();
                    roomScore.setText(String.format(Locale.getDefault(), "%.0f", score * 100));
                } else {
                    roomScore.setText("0");
                }
            } else {
                roomScore.setText("0");
            }
        }

        private void setImages(List<String> photos) {
            if (photos != null && !photos.isEmpty()) {
                Glide.with(context)
                        .load(photos.get(0))
                        .centerCrop()
                        .into(roomImage);
            } else {
                roomImage.setImageResource(R.drawable.ic_launcher_background);
            }
        }
    }
}
