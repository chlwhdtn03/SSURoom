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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cse.ssuroom.R;
import cse.ssuroom.databinding.FragmentFilterSettingBinding;

public class FilterSettingFragment extends Fragment {

    private FragmentFilterSettingBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public static FilterSettingFragment newInstance() {
        return new FilterSettingFragment();
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

        binding.btnSaveFilter.setOnClickListener(v -> saveFilter());
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

        List<Float> priceRange = binding.sliderPrice.getValues();
        Float minPrice = priceRange.get(0);
        Float maxPrice = priceRange.get(1);

        Map<String, Object> filter = new HashMap<>();
        filter.put("propertyType", propertyType);
        filter.put("minPrice", minPrice.intValue());
        filter.put("maxPrice", maxPrice.intValue());
        filter.put("createdAt", FieldValue.serverTimestamp()); // 필터 생성 시간

        String uid = currentUser.getUid();
        db.collection("users").document(uid)
                .update("notificationFilters", FieldValue.arrayUnion(filter))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "알림 조건이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    if (getParentFragmentManager() != null) {
                        getParentFragmentManager().popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w("FilterSetting", "Error updating document", e);
                    Toast.makeText(getContext(), "알림 조건 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
