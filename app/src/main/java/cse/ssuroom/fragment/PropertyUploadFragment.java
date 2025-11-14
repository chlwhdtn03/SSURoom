package cse.ssuroom.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Objects;

import cse.ssuroom.R;
import cse.ssuroom.databinding.FragmentUploadPropertyBinding;

public class PropertyUploadFragment extends Fragment {

    // ViewBinding으로 UI 요소에 안전하게 접근
    private FragmentUploadPropertyBinding binding;

    private boolean isShortTerm = true; // 기본값: 단기 임대

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUploadPropertyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupListeners();
        updateTypeSelection(true); // 초기 상태: 단기 임대 선택
    }

    private void setupListeners() {
        // 닫기 버튼
        binding.ivClose.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // 단기 임대 카드 클릭
        binding.cardShortTerm.setOnClickListener(v -> {
            if (!isShortTerm) {
                isShortTerm = true;
                updateTypeSelection(true);
            }
        });

        // 계약 양도 카드 클릭
        binding.cardContract.setOnClickListener(v -> {
            if (isShortTerm) {
                isShortTerm = false;
                updateTypeSelection(false);
            }
        });

        // 사진 업로드 영역 클릭
        binding.uploadImageArea.setOnClickListener(v -> {

            Toast.makeText(requireContext(), "이미지 선택 기능 구현 예정", Toast.LENGTH_SHORT).show();


        });

        // 취소 버튼
        binding.btnCancel.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // 등록 버튼
        binding.btnSubmit.setOnClickListener(v -> submitProperty());
    }

    private void updateTypeSelection(boolean isShortTermSelected) {
        // 색상 리소스 가져오기
        int green = ContextCompat.getColor(requireContext(), R.color.ssu_green_primary);
        int blue = ContextCompat.getColor(requireContext(), R.color.ssu_blue_primary);
        int lightGreen = ContextCompat.getColor(requireContext(), R.color.ssu_green_background);
        int lightBlue = ContextCompat.getColor(requireContext(), R.color.ssu_blue_background);
        int grey = ContextCompat.getColor(requireContext(), R.color.ssu_grey_divider);
        int lightGrey = ContextCompat.getColor(requireContext(), R.color.ssu_grey_background);

        if (isShortTermSelected) {
            // 단기 임대 선택
            binding.cardShortTerm.setStrokeColor(green);
            binding.cardShortTerm.setStrokeWidth(6);
            binding.cardShortTerm.setCardBackgroundColor(lightGreen);

            binding.cardContract.setStrokeColor(grey);
            binding.cardContract.setStrokeWidth(3);
            binding.cardContract.setCardBackgroundColor(lightGrey);

            // 입력 필드 전환
            binding.layoutWeeklyPrice.setVisibility(View.VISIBLE);
            binding.layoutDepositMonthly.setVisibility(View.GONE);

        } else {
            // 계약 양도 선택
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
        String title = Objects.requireNonNull(binding.etTitle.getText()).toString().trim();
        String imageUrl = Objects.requireNonNull(binding.etImageUrl.getText()).toString().trim();

        // 유효성 검사
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
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();

        } else {
            String deposit = Objects.requireNonNull(binding.etDeposit.getText()).toString().trim();
            String monthlyRent = Objects.requireNonNull(binding.etMonthlyRent.getText()).toString().trim();
            if (deposit.isEmpty() || monthlyRent.isEmpty()) {
                Toast.makeText(requireContext(), "보증금과 월세를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            String message = "계약 양도 등록: " + title + ", 보증금 " + deposit + "만원, 월세 " + monthlyRent + "만원";
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }

        if (getActivity() != null) {
            getActivity().onBackPressed();
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // 메모리 누수 방지
    }
}
