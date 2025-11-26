package cse.ssuroom.bottomsheet;

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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cse.ssuroom.R;
import cse.ssuroom.adapter.ImageUploadAdapter;
import cse.ssuroom.database.LeaseTransfer;
import cse.ssuroom.database.LeaseTransferRepository;
import cse.ssuroom.database.ShortTerm;
import cse.ssuroom.database.ShortTermRepository;
import cse.ssuroom.databinding.FragmentUploadPropertyBinding;

public class UploadPropertyBottomSheet extends BottomSheetDialogFragment {

    private FragmentUploadPropertyBinding binding;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final StorageReference storageRef = storage.getReference();
    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();

    private ShortTermRepository shortTermRepo;
    private LeaseTransferRepository leaseTransferRepo;

    private boolean isShortTerm = true;
    private List<String> uploadedImageUrls = new ArrayList<>();
    private ImageUploadAdapter imageAdapter;

    // 편의시설 선택 상태
    private boolean isFullOptionSelected = false;
    private boolean isAirconSelected = false;
    private boolean isWasherSelected = false;
    private boolean isRefrigeratorSelected = false;
    private boolean isBedSelected = false;
    private boolean isDeskSelected = false;

    public static UploadPropertyBottomSheet newInstance() {
        return new UploadPropertyBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadPropertyBinding.inflate(inflater, container, false);

        shortTermRepo = new ShortTermRepository();
        leaseTransferRepo = new LeaseTransferRepository();

        setupRecyclerView();

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

    private void setupRecyclerView() {
        imageAdapter = new ImageUploadAdapter(uploadedImageUrls, position -> {
            uploadedImageUrls.remove(position);
            imageAdapter.notifyItemRemoved(position);
            imageAdapter.notifyItemRangeChanged(position, uploadedImageUrls.size());
            updateImagePreviewVisibility();
            updateImageCountText();
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        );
        binding.rvUploadedImages.setLayoutManager(layoutManager);
        binding.rvUploadedImages.setAdapter(imageAdapter);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListeners();
        updateTypeSelection(true);
        updateImagePreviewVisibility();
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

                        if (imageAdapter != null) {
                            imageAdapter.notifyItemInserted(uploadedImageUrls.size() - 1);
                        }
                        updateImagePreviewVisibility();
                        updateImageCountText();

                        Toast.makeText(requireContext(),
                                "이미지 업로드 완료!",
                                Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateImagePreviewVisibility() {
        if (uploadedImageUrls.isEmpty()) {
            binding.rvUploadedImages.setVisibility(View.GONE);
        } else {
            binding.rvUploadedImages.setVisibility(View.VISIBLE);
        }
    }

    private void updateImageCountText() {
        if (uploadedImageUrls.isEmpty()) {
            binding.tvImageCount.setText("클릭하여 이미지 추가");
        } else {
            binding.tvImageCount.setText(uploadedImageUrls.size() + "개의 이미지 업로드됨 (추가 가능)");
        }
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
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            galleryLauncher.launch(intent);
        });

        // 편의시설 버튼 클릭 리스너
        binding.cardFullOption.setOnClickListener(v -> {
            isFullOptionSelected = !isFullOptionSelected;
            updateAmenityCardStyle(binding.cardFullOption, isFullOptionSelected);
        });

        binding.cardAircon.setOnClickListener(v -> {
            isAirconSelected = !isAirconSelected;
            updateAmenityCardStyle(binding.cardAircon, isAirconSelected);
        });

        binding.cardWasher.setOnClickListener(v -> {
            isWasherSelected = !isWasherSelected;
            updateAmenityCardStyle(binding.cardWasher, isWasherSelected);
        });

        binding.cardRefrigerator.setOnClickListener(v -> {
            isRefrigeratorSelected = !isRefrigeratorSelected;
            updateAmenityCardStyle(binding.cardRefrigerator, isRefrigeratorSelected);
        });

        binding.cardBed.setOnClickListener(v -> {
            isBedSelected = !isBedSelected;
            updateAmenityCardStyle(binding.cardBed, isBedSelected);
        });

        binding.cardDesk.setOnClickListener(v -> {
            isDeskSelected = !isDeskSelected;
            updateAmenityCardStyle(binding.cardDesk, isDeskSelected);
        });

        binding.btnCancel.setOnClickListener(v -> dismiss());
        binding.btnSubmit.setOnClickListener(v -> submitProperty());
    }

    private void updateAmenityCardStyle(MaterialCardView card, boolean isSelected) {
        if (isSelected) {
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.ssu_green_primary));
            card.setStrokeWidth(6);
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ssu_green_background));
        } else {
            card.setStrokeColor(ContextCompat.getColor(requireContext(), R.color.ssu_grey_divider));
            card.setStrokeWidth(3);
            card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.ssu_grey_background));
        }
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        String hostId = currentUser.getUid();
        String title = binding.etTitle.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String location = binding.etLocation.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "매물 제목을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (uploadedImageUrls.isEmpty()) {
            Toast.makeText(requireContext(), "최소 1개의 이미지를 업로드해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSubmit.setEnabled(false);
        Toast.makeText(requireContext(), "매물 등록 중...", Toast.LENGTH_SHORT).show();

        if (isShortTerm) {
            saveShortTermProperty(hostId, title, description, location);
        } else {
            saveLeaseTransferProperty(hostId, title, description, location);
        }
    }

    private void saveShortTermProperty(String hostId, String title, String description, String location) {
        String weeklyPrice = binding.etWeeklyPrice.getText().toString().trim();
        String roomType = binding.etRoomType.getText().toString().trim();
        String areaStr = binding.etArea.getText().toString().trim();
        String floorStr = binding.etFloor.getText().toString().trim();

        if (weeklyPrice.isEmpty()) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "주당 가격을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 방 타입 처리
        if (roomType.isEmpty()) {
            roomType = "원룸"; // 기본값
        }

        // 면적 처리
        double area = 20.0; // 기본값
        if (!areaStr.isEmpty()) {
            try {
                area = Double.parseDouble(areaStr);
                if (area <= 0) {
                    binding.btnSubmit.setEnabled(true);
                    Toast.makeText(requireContext(), "면적은 0보다 커야 합니다", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                binding.btnSubmit.setEnabled(true);
                Toast.makeText(requireContext(), "올바른 면적을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 층수 처리
        int floor = 1; // 기본값
        if (!floorStr.isEmpty()) {
            try {
                floor = Integer.parseInt(floorStr);
            } catch (NumberFormatException e) {
                binding.btnSubmit.setEnabled(true);
                Toast.makeText(requireContext(), "올바른 층수를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            HashMap<String, Object> pricing = new HashMap<>();
            pricing.put("weeklyPrice", Integer.parseInt(weeklyPrice));
            pricing.put("type", "short_term");

            HashMap<String, Object> locationMap = createLocationFromInput(location);
            HashMap<String, Object> amenities = getAmenitiesFromButtons();
            HashMap<String, Object> scores = createDefaultScores();

            ShortTerm property = new ShortTerm(
                    title,
                    description.isEmpty() ? "매물 설명 없음" : description,
                    hostId,
                    uploadedImageUrls,
                    roomType,
                    floor,
                    area,
                    new Date(),
                    null,
                    pricing,
                    locationMap,
                    amenities,
                    scores
            );

            shortTermRepo.save(property, this::handleSaveResult);

        } catch (NumberFormatException e) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "올바른 가격을 입력해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLeaseTransferProperty(String hostId, String title, String description, String location) {
        String deposit = binding.etDeposit.getText().toString().trim();
        String monthlyRent = binding.etMonthlyRent.getText().toString().trim();
        String roomType = binding.etRoomType.getText().toString().trim();
        String areaStr = binding.etArea.getText().toString().trim();
        String floorStr = binding.etFloor.getText().toString().trim();

        if (deposit.isEmpty() || monthlyRent.isEmpty()) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "보증금과 월세를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 방 타입 처리
        if (roomType.isEmpty()) {
            roomType = "원룸";
        }

        // 면적 처리
        double area = 20.0;
        if (!areaStr.isEmpty()) {
            try {
                area = Double.parseDouble(areaStr);
                if (area <= 0) {
                    binding.btnSubmit.setEnabled(true);
                    Toast.makeText(requireContext(), "면적은 0보다 커야 합니다", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                binding.btnSubmit.setEnabled(true);
                Toast.makeText(requireContext(), "올바른 면적을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 층수 처리
        int floor = 1;
        if (!floorStr.isEmpty()) {
            try {
                floor = Integer.parseInt(floorStr);
            } catch (NumberFormatException e) {
                binding.btnSubmit.setEnabled(true);
                Toast.makeText(requireContext(), "올바른 층수를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        try {
            HashMap<String, Object> pricing = new HashMap<>();
            pricing.put("deposit", Integer.parseInt(deposit));
            pricing.put("monthlyRent", Integer.parseInt(monthlyRent));
            pricing.put("type", "lease_transfer");

            HashMap<String, Object> locationMap = createLocationFromInput(location);
            HashMap<String, Object> amenities = getAmenitiesFromButtons();
            HashMap<String, Object> scores = createDefaultScores();

            LeaseTransfer property = new LeaseTransfer(
                    title,
                    description.isEmpty() ? "매물 설명 없음" : description,
                    hostId,
                    uploadedImageUrls,
                    roomType,
                    floor,
                    area,
                    new Date(),
                    null,
                    pricing,
                    locationMap,
                    amenities,
                    scores
            );

            leaseTransferRepo.save(property, this::handleSaveResult);

        } catch (NumberFormatException e) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "올바른 금액을 입력해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    private HashMap<String, Object> getAmenitiesFromButtons() {
        HashMap<String, Object> amenities = new HashMap<>();
        amenities.put("isFullOption", isFullOptionSelected);
        amenities.put("hasAircon", isAirconSelected);
        amenities.put("hasWasher", isWasherSelected);
        amenities.put("hasRefrigerator", isRefrigeratorSelected);
        amenities.put("hasBed", isBedSelected);
        amenities.put("hasDesk", isDeskSelected);
        return amenities;
    }

    private void handleSaveResult(String propertyId) {
        binding.btnSubmit.setEnabled(true);
        // 내가 올린 매물 Firebase User에 등록
        if (propertyId != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users").document(currentUser.getUid())
                        .update("uploadedProperties", FieldValue.arrayUnion(propertyId));
            }

            Toast.makeText(requireContext(), "매물이 성공적으로 등록되었습니다!", Toast.LENGTH_LONG).show();
            dismiss();
        } else {
            Toast.makeText(requireContext(), "매물 등록에 실패했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    private HashMap<String, Object> createLocationFromInput(String locationInput) {
        HashMap<String, Object> location = new HashMap<>();
        if (locationInput.isEmpty()) {
            location.put("address", "서울시 동작구 상도동");
        } else {
            location.put("address", locationInput);
        }
        location.put("latitude", 0);
        location.put("longitude", 0);
        location.put("distanceToSchool", 0);
        return location;
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