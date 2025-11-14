package cse.ssuroom.bottomsheet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import cse.ssuroom.R;
// 올바른 ViewBinding 경로로 수정
import cse.ssuroom.databinding.FragmentUploadPropertyBinding;

public class UploadPropertyBottomSheet extends BottomSheetDialogFragment {

    private FragmentUploadPropertyBinding binding;
    private boolean isShortTerm = true; // 기본값: 단기 임대

    public static UploadPropertyBottomSheet newInstance() {
        return new UploadPropertyBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadPropertyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupListeners();
        // 초기 상태: 단기 임대 선택으로 UI 설정
        updateTypeSelection(true);
    }

    private void setupListeners() {
        binding.ivClose.setOnClickListener(v -> dismiss());

        binding.cardShortTerm.setOnClickListener(v -> {
            if (!isShortTerm) {
                isShortTerm = true;
                updateTypeSelection(true);
            }
        });

        binding.cardContract.setOnClickListener(v -> {
            if (isShortTerm) {
                isShortTerm = false;
                updateTypeSelection(false);
            }
        });

        binding.uploadImageArea.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "이미지 선택 기능 구현 예정", Toast.LENGTH_SHORT).show();

        });

        binding.btnCancel.setOnClickListener(v -> dismiss());

        binding.btnSubmit.setOnClickListener(v -> submitProperty());
    }

    private void updateTypeSelection(boolean isShortTermSelected) {
        int green = ContextCompat.getColor(requireContext(), R.color.ssu_green_primary);
        int blue = ContextCompat.getColor(requireContext(), R.color.ssu_blue_primary);
        int lightGreen = ContextCompat.getColor(requireContext(), R.color.ssu_green_background);
        int lightBlue = ContextCompat.getColor(requireContext(), R.color.ssu_blue_background);
        int grey = ContextCompat.getColor(requireContext(), R.color.ssu_grey_divider);
        int lightGrey = ContextCompat.getColor(requireContext(), R.color.ssu_grey_background);

        if (isShortTermSelected) {
            // --- 단기 임대 선택 시 UI ---
            binding.cardShortTerm.setStrokeColor(green);
            binding.cardShortTerm.setStrokeWidth(6); // 테두리를 더 굵게
            binding.cardShortTerm.setCardBackgroundColor(lightGreen);

            binding.cardContract.setStrokeColor(grey);
            binding.cardContract.setStrokeWidth(3); // 테두리를 얇게
            binding.cardContract.setCardBackgroundColor(lightGrey);

            // 입력 필드 전환
            binding.layoutWeeklyPrice.setVisibility(View.VISIBLE);
            binding.layoutDepositMonthly.setVisibility(View.GONE);

        } else {
            // --- 계약 양도 선택 시 UI ---
            binding.cardShortTerm.setStrokeColor(grey);
            binding.cardShortTerm.setStrokeWidth(3);
            binding.cardShortTerm.setCardBackgroundColor(lightGrey);

            binding.cardContract.setStrokeColor(blue);
            binding.cardContract.setStrokeWidth(6);
            binding.cardContract.setCardBackgroundColor(lightBlue);

            // 입력 필드 전환
            binding.layoutWeeklyPrice.setVisibility(View.GONE);
            binding.layoutDepositMonthly.setVisibility(View.VISIBLE);
        }
    }

    private void submitProperty() {
        String title = binding.etTitle.getText().toString().trim();
        String imageUrl = binding.etImageUrl.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "매물 제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isShortTerm) {
            String weeklyPrice = binding.etWeeklyPrice.getText().toString().trim();
            if (weeklyPrice.isEmpty()) {
                Toast.makeText(requireContext(), "주당 가격을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String message = "단기 임대 등록: " + title + ", 주당 " + weeklyPrice + "원";
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();

        } else {
            String deposit = binding.etDeposit.getText().toString().trim();
            String monthlyRent = binding.etMonthlyRent.getText().toString().trim();
            if (deposit.isEmpty() || monthlyRent.isEmpty()) {
                Toast.makeText(requireContext(), "보증금과 월세를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String message = "계약 양도 등록: " + title + ", 보증금 " + deposit + "만원, 월세 " + monthlyRent + "만원";
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }

        // TODO: 실제 Firebase에 데이터 저장하는 로직 구현

        dismiss(); // 성공적으로 제출 후 닫기
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // 메모리 누수 방지
    }
}
