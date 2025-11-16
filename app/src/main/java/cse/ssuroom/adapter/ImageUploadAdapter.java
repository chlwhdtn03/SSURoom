package cse.ssuroom.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import cse.ssuroom.R;

public class ImageUploadAdapter extends RecyclerView.Adapter<ImageUploadAdapter.ImageViewHolder> {

    private List<String> imageUrls;
    private OnImageDeleteListener deleteListener;

    public interface OnImageDeleteListener {
        void onDelete(int position);
    }

    public ImageUploadAdapter(List<String> imageUrls, OnImageDeleteListener deleteListener) {
        this.imageUrls = imageUrls;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_uploading, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);

        // Glide로 이미지 로드
        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery) // 기본 플레이스홀더
                .error(android.R.drawable.ic_menu_report_image) // 에러 이미지
                .into(holder.ivImage);

        // 삭제 버튼 클릭
        holder.ivDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    deleteListener.onDelete(currentPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        ImageView ivDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_uploaded_image);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
}