package cse.ssuroom.bottomsheet;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.RangeSlider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cse.ssuroom.R;
import cse.ssuroom.databinding.FragmentFilterSettingBinding;

public class FilterSettingBottomSheet extends BottomSheetDialogFragment {

    private FragmentFilterSettingBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    public static final String TAG = "FilterSettingBottomSheet";

    // New UI elements via binding
    // Filter values
    private float minScore = 0;
    private float maxScore = 100;
    private float minPrice;
    private float maxPrice;
    private float minDuration = 1;
    private float maxDuration = 52;
    
    private OnFilterSavedListener savedListener;

    public interface OnFilterSavedListener {
        void onFilterSaved();
    }

    public static FilterSettingBottomSheet newInstance() {
        return new FilterSettingBottomSheet();
    }
    
    public void setOnFilterSavedListener(OnFilterSavedListener listener) {
        this.savedListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFilterSettingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        setupListeners();
        setDefaultFilterValues(); // Set initial filter values

        binding.btnSaveFilter.setOnClickListener(v -> saveFilter());
    }

    private void setDefaultFilterValues() {
        // Default score
        binding.sliderScore.setValues(0f, 100f);
        updateScoreText(0f, 100f);

        // Default duration
        binding.sliderDuration.setValues(1f, 52f);
        updateDurationText(1f, 52f);

        // Default property type to Short Term
        binding.rgPropertyType.check(R.id.rb_short_term);
        // This will trigger the listener and set default price for short term
    }

    private void setupListeners() {
        binding.sliderScore.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            minScore = values.get(0);
            maxScore = values.get(1);
            updateScoreText(minScore, maxScore);
        });

        binding.sliderDuration.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            minDuration = values.get(0);
            maxDuration = values.get(1);
            updateDurationText(minDuration, maxDuration);
        });

        binding.rgPropertyType.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isLeaseTransfer = (checkedId == R.id.rb_lease_transfer);
            setupPriceFilter(isLeaseTransfer);
        });

        binding.sliderPrice.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            minPrice = values.get(0);
            maxPrice = values.get(1);
            updatePriceText(minPrice, maxPrice, binding.rgPropertyType.getCheckedRadioButtonId() == R.id.rb_lease_transfer);
        });
    }

    private void setupPriceFilter(boolean isLeaseTransfer) {
        if (isLeaseTransfer) {
            // 계약양도: 월세 (만원 단위로 표시, 실제 저장은 만원 단위 숫자)
            binding.priceTitleSetting.setText("월세");
            binding.sliderPrice.setValueFrom(10);
            binding.sliderPrice.setValueTo(200);
            binding.sliderPrice.setStepSize(5);
            binding.sliderPrice.setValues(10f, 200f);
            minPrice = 10;
            maxPrice = 200;
        } else {
            // 단기임대: 주당 가격 (원 단위)
            binding.priceTitleSetting.setText("주당 가격");
            binding.sliderPrice.setValueFrom(50000);
            binding.sliderPrice.setValueTo(500000);
            binding.sliderPrice.setStepSize(10000);
            binding.sliderPrice.setValues(50000f, 500000f);
            minPrice = 50000;
            maxPrice = 500000;
        }
        updatePriceText(minPrice, maxPrice, isLeaseTransfer);
    }


    private void updateScoreText(float min, float max) {
        binding.minScoreText.setText(String.format(Locale.getDefault(), "%.0f점", min));
        binding.maxScoreText.setText(String.format(Locale.getDefault(), "%.0f점", max));
    }

    private void updatePriceText(float min, float max, boolean isLeaseTransfer) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);

        if (isLeaseTransfer) {
            // 계약양도: 만원 단위로 표시
            binding.minPriceTextSetting.setText(formatter.format((int) min) + "만원");
            binding.maxPriceTextSetting.setText(formatter.format((int) max) + "만원");
        } else {
            // 단기임대: 원 단위로 표시
            binding.minPriceTextSetting.setText(formatter.format((int) min) + "원");
            binding.maxPriceTextSetting.setText(formatter.format((int) max) + "원");
        }
    }

    private void updateDurationText(float min, float max) {
        if (max >= 52) {
            binding.minDurationText.setText(String.format(Locale.getDefault(), "%.0f주", min));
            binding.maxDurationText.setText("1년 이상");
        } else {
            binding.minDurationText.setText(String.format(Locale.getDefault(), "%.0f주", min));
            binding.maxDurationText.setText(String.format(Locale.getDefault(), "%.0f주", max));
        }
    }

    private void saveFilter() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String propertyType = "";
        int checkedId = binding.rgPropertyType.getCheckedRadioButtonId();
        if (checkedId == R.id.rb_short_term) {
            propertyType = "short_term";
        } else if (checkedId == R.id.rb_lease_transfer) {
            propertyType = "lease_transfer";
        }

        if (propertyType.isEmpty()) {
            Toast.makeText(getContext(), "매물 종류를 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get final values from sliders
        List<Float> scoreRange = binding.sliderScore.getValues();
        minScore = scoreRange.get(0);
        maxScore = scoreRange.get(1);

        List<Float> priceRange = binding.sliderPrice.getValues();
        minPrice = priceRange.get(0);
        maxPrice = priceRange.get(1);

        List<Float> durationRange = binding.sliderDuration.getValues();
        minDuration = durationRange.get(0);
        maxDuration = durationRange.get(1);


        Map<String, Object> filter = new HashMap<>();
        filter.put("propertyType", propertyType);
        filter.put("minScore", minScore);
        filter.put("maxScore", maxScore);
        filter.put("minPrice", minPrice);
        filter.put("maxPrice", maxPrice);
        filter.put("minDuration", minDuration);
        filter.put("maxDuration", maxDuration);
        filter.put("createdAt", new Date());

        String uid = currentUser.getUid();
        db.collection("users").document(uid)
                .update("notificationFilters", FieldValue.arrayUnion(filter))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "알림 조건이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    if (savedListener != null) {
                        savedListener.onFilterSaved();
                    }
                    dismiss(); // BottomSheet 닫기
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating document", e);
                    Toast.makeText(getContext(), "알림 조건 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
