package cse.ssuroom.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import cse.ssuroom.R;
import cse.ssuroom.database.LeaseTransfer;
import cse.ssuroom.database.LeaseTransferRepository;
import cse.ssuroom.database.ShortTerm;
import cse.ssuroom.database.ShortTermRepository;
import cse.ssuroom.databinding.FragmentUploadPropertyBinding;

public class PropertyUploadFragment extends Fragment {

    private FragmentUploadPropertyBinding binding;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final StorageReference storageRef = storage.getReference();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Repository 인스턴스
    private ShortTermRepository shortTermRepo;
    private LeaseTransferRepository leaseTransferRepo;

    private boolean isShortTerm = true;
    private List<String> uploadedImageUrls = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUploadPropertyBinding.inflate(inflater, container, false);

        // Repository 초기화
        shortTermRepo = new ShortTermRepository();
        leaseTransferRepo = new LeaseTransferRepository();

        // 갤러리 런처 등록
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            uploadImageToFirebase(selectedImageUri);
                        }
                    }
                }
        );

        return binding.getRoot();
    }

    private void uploadImageToFirebase(Uri imageUri) {
        String fileName = "property_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child("property_images/" + fileName);

        Toast.makeText(requireContext(), "이미지 업로드 중...", Toast.LENGTH_SHORT).show();

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        uploadedImageUrls.add(downloadUrl);

                        // 업로드된 이미지 개수 표시
                        if (uploadedImageUrls.size() == 1) {
                            binding.etImageUrl.setText(downloadUrl);
                        } else {
                            binding.etImageUrl.setText(uploadedImageUrls.size() + "개의 이미지 업로드됨");
                        }

                        Toast.makeText(requireContext(),
                                "이미지 업로드 완료! (" + uploadedImageUrls.size() + "개)",
                                Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListeners();
        updateTypeSelection(true);
    }

    private void setupListeners() {
        binding.ivClose.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

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
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        binding.btnCancel.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

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
            binding.cardShortTerm.setStrokeColor(green);
            binding.cardShortTerm.setStrokeWidth(6);
            binding.cardShortTerm.setCardBackgroundColor(lightGreen);

            binding.cardContract.setStrokeColor(grey);
            binding.cardContract.setStrokeWidth(3);
            binding.cardContract.setCardBackgroundColor(lightGrey);

            binding.layoutWeeklyPrice.setVisibility(View.VISIBLE);
            binding.layoutDepositMonthly.setVisibility(View.GONE);
        } else {
            binding.cardShortTerm.setStrokeColor(grey);
            binding.cardShortTerm.setStrokeWidth(3);
            binding.cardShortTerm.setCardBackgroundColor(lightGrey);

            binding.cardContract.setStrokeColor(blue);
            binding.cardContract.setStrokeWidth(6);
            binding.cardContract.setCardBackgroundColor(lightBlue);

            binding.layoutWeeklyPrice.setVisibility(View.GONE);
            binding.layoutDepositMonthly.setVisibility(View.VISIBLE);
        }
    }

    private void submitProperty() {
        // 로그인 확인
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String hostId = currentUser.getUid();
        String title = Objects.requireNonNull(binding.etTitle.getText()).toString().trim();

        // 기본 유효성 검사
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "매물 제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uploadedImageUrls.isEmpty()) {
            Toast.makeText(requireContext(), "최소 1개의 이미지를 업로드해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 등록 버튼 비활성화 (중복 클릭 방지)
        binding.btnSubmit.setEnabled(false);
        Toast.makeText(requireContext(), "매물 등록 중...", Toast.LENGTH_SHORT).show();

        // 매물 타입에 따라 객체 생성 및 저장
        if (isShortTerm) {
            saveShortTermProperty(hostId, title);
        } else {
            saveLeaseTransferProperty(hostId, title);
        }
    }

    private void saveShortTermProperty(String hostId, String title) {
        String weeklyPrice = binding.etWeeklyPrice.getText().toString().trim();

        if (weeklyPrice.isEmpty()) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "주당 가격을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 가격 정보 생성
            HashMap<String, Object> pricing = new HashMap<>();
            pricing.put("weeklyPrice", Integer.parseInt(weeklyPrice));
            pricing.put("type", "short_term");

            // 기본값으로 HashMap 생성 (실제로는 UI에서 받아와야 함)
            HashMap<String, Object> location = createDefaultLocation();
            HashMap<String, Object> amenities = createDefaultAmenities();
            HashMap<String, Object> scores = createDefaultScores();

            // ShortTerm 객체 생성
            ShortTerm property = new ShortTerm(
                    title,
                    "매물 설명", // TODO: UI에서 입력받기
                    hostId,
                    uploadedImageUrls,
                    "원룸", // TODO: UI에서 선택받기
                    1, // TODO: UI에서 입력받기
                    20.0, // TODO: UI에서 입력받기
                    new Date(), // 입주 가능일 TODO: UI에서 선택받기
                    null, // 퇴실일은 단기임대라 필요시 추가
                    pricing,
                    location,
                    amenities,
                    scores
            );

            // Firestore에 저장
            shortTermRepo.save(property, this::handleSaveResult);

        } catch (NumberFormatException e) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "올바른 가격을 입력해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLeaseTransferProperty(String hostId, String title) {
        String deposit = Objects.requireNonNull(binding.etDeposit.getText()).toString().trim();
        String monthlyRent = Objects.requireNonNull(binding.etMonthlyRent.getText()).toString().trim();

        if (deposit.isEmpty() || monthlyRent.isEmpty()) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "보증금과 월세를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 가격 정보 생성
            HashMap<String, Object> pricing = new HashMap<>();
            pricing.put("deposit", Integer.parseInt(deposit));
            pricing.put("monthlyRent", Integer.parseInt(monthlyRent));
            pricing.put("type", "lease_transfer");

            // 기본값으로 HashMap 생성
            HashMap<String, Object> location = createDefaultLocation();
            HashMap<String, Object> amenities = createDefaultAmenities();
            HashMap<String, Object> scores = createDefaultScores();

            // LeaseTransfer 객체 생성
            LeaseTransfer property = new LeaseTransfer(
                    title,
                    "매물 설명", // TODO: UI에서 입력받기
                    hostId,
                    uploadedImageUrls,
                    "원룸", // TODO: UI에서 선택받기
                    1, // TODO: UI에서 입력받기
                    20.0, // TODO: UI에서 입력받기
                    new Date(), // 입주 가능일 TODO: UI에서 선택받기
                    null, // 퇴실일 TODO: 필요시 추가
                    pricing,
                    location,
                    amenities,
                    scores
            );

            // Firestore에 저장
            leaseTransferRepo.save(property, this::handleSaveResult);

        } catch (NumberFormatException e) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "올바른 금액을 입력해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSaveResult(String propertyId) {
        binding.btnSubmit.setEnabled(true);

        if (propertyId != null) {
            Toast.makeText(requireContext(), "매물이 성공적으로 등록되었습니다!", Toast.LENGTH_LONG).show();
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        } else {
            Toast.makeText(requireContext(), "매물 등록에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    // 기본값 생성 메소드들 (나중에 UI에서 입력받도록 수정 필요)
    private HashMap<String, Object> createDefaultLocation() {
        HashMap<String, Object> location = new HashMap<>();
        location.put("address", "서울시 동작구 상도동");
        location.put("latitude", 0);
        location.put("longitude", 0);
        location.put("distanceToSchool", 0); // 학교까지 거리(m)
        return location;
    }

    private HashMap<String, Object> createDefaultAmenities() {
        HashMap<String, Object> amenities = new HashMap<>();
        amenities.put("hasAircon", false);
        amenities.put("hasRefrigerator", false);
        amenities.put("hasWasher", false);
        amenities.put("hasElevator", false);
        amenities.put("hasPet", false);
        return amenities;
    }

    private HashMap<String, Object> createDefaultScores() {
        HashMap<String, Object> scores = new HashMap<>();
        scores.put("cleanliness", 0.0);
        scores.put("communication", 0.0);
        scores.put("accuracy", 0.0);
        scores.put("location", 0.0);
        scores.put("overall", 0.0);
        return scores;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}