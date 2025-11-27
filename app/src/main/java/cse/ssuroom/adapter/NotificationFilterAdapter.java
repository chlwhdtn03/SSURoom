package cse.ssuroom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cse.ssuroom.R;

public class NotificationFilterAdapter extends RecyclerView.Adapter<NotificationFilterAdapter.FilterViewHolder> {

    private List<Map<String, Object>> filterList;
    private OnDeleteClickListener deleteListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    public NotificationFilterAdapter(List<Map<String, Object>> filterList, OnDeleteClickListener deleteListener) {
        this.filterList = filterList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public FilterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_filter, parent, false);
        return new FilterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilterViewHolder holder, int position) {
        Map<String, Object> filter = filterList.get(position);
        holder.bind(filter);
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filterList != null ? filterList.size() : 0;
    }

    static class FilterViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvDetails;
        ImageView btnDelete;

        public FilterViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDetails = itemView.findViewById(R.id.tv_details);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        public void bind(Map<String, Object> filter) {
            String propertyType = (String) filter.get("propertyType");
            boolean isLeaseTransfer = "lease_transfer".equals(propertyType);

            // Title
            tvTitle.setText(isLeaseTransfer ? "계약 양도 매물 알림" : "단기 임대 매물 알림");

            // Details construction
            StringBuilder details = new StringBuilder();
            NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);

            // Price
            double minPrice = getDouble(filter.get("minPrice"));
            double maxPrice = getDouble(filter.get("maxPrice"));
            
            if (isLeaseTransfer) {
                details.append(String.format("월세 %s~%s만원", formatter.format((int)minPrice), formatter.format((int)maxPrice)));
            } else {
                details.append(String.format("주당 %s~%s원", formatter.format((int)minPrice), formatter.format((int)maxPrice)));
            }

            // Duration
            double minDuration = getDouble(filter.get("minDuration"));
            double maxDuration = getDouble(filter.get("maxDuration"));
            details.append(", ");
            if (maxDuration >= 52) {
                details.append(String.format(Locale.KOREA, "%.0f주~1년 이상", minDuration));
            } else {
                details.append(String.format(Locale.KOREA, "%.0f~%.0f주", minDuration, maxDuration));
            }

            tvDetails.setText(details.toString());
        }
        
        private double getDouble(Object value) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return 0.0;
        }
    }
}
