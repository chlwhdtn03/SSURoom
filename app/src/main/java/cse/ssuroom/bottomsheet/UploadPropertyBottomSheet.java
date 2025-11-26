package cse.ssuroom.bottomsheet;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cse.ssuroom.BuildConfig;
import cse.ssuroom.R;
import cse.ssuroom.DaumAddressActivity;
import cse.ssuroom.adapter.ImageUploadAdapter;
import cse.ssuroom.database.LeaseTransfer;
import cse.ssuroom.database.LeaseTransferRepository;
import cse.ssuroom.database.ShortTerm;
import cse.ssuroom.database.ShortTermRepository;
import cse.ssuroom.databinding.FragmentUploadPropertyBinding;

public class UploadPropertyBottomSheet extends BottomSheetDialogFragment {

    private static final String KAKAO_REST_API_KEY = BuildConfig.KAKAO_REST_API_KEY;

    private FragmentUploadPropertyBinding binding;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> addressLauncher;
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

    // 선택된 주소 저장
    private String selectedAddress = "";
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public static UploadPropertyBottomSheet newInstance() {
        return new UploadPropertyBottomSheet();
    }

    private final String[] CATEGORY_CODES = {
            "CS2", // 편의점
            "CE7", // 카페
            "FD6", // 음식점
            "MT1", // 마트
            "PM9", // 약국
            "HP8", // 병원
            "SW8", // 지하철역
            "BS3", // 버스정류장
            "PK6", // 주차장
            "BK9"  // 은행
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadPropertyBinding.inflate(inflater, container, false);

        shortTermRepo = new ShortTermRepository();
        leaseTransferRepo = new LeaseTransferRepository();

        setupRecyclerView();

        // 갤러리 런처 초기화
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

        // 주소 검색 런처 초기화
        addressLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String address = result.getData().getStringExtra(DaumAddressActivity.EXTRA_SELECTED_ADDRESS);

                        if (address != null) {
                            selectedAddress = address;
                            binding.etLocation.setText(address);
                            Toast.makeText(requireContext(), "주소가 선택되었습니다", Toast.LENGTH_SHORT).show();
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

        // 주소 검색 버튼 클릭 리스너
        binding.btnSearchAddress.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), DaumAddressActivity.class);
            addressLauncher.launch(intent);
        });

        // etLocation 한글 입력 가능하도록 설정
        binding.etLocation.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        binding.etLocation.setFocusable(true);
        binding.etLocation.setFocusableInTouchMode(true);

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
        // 입주 가능 날짜 클릭
        binding.etMoveInDate.setOnClickListener(v ->
                showDatePicker(date -> {}, true)
        );

        binding.etMoveOutDate.setOnClickListener(v -> {
            if (isShortTerm) {
                showDatePicker(date -> {}, false);
            } else {
                Toast.makeText(requireContext(), "계약 양도일 경우 종료 날짜는 필요 없습니다", Toast.LENGTH_SHORT).show();
            }
        });
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

            binding.layoutMoveOutDate.setVisibility(View.VISIBLE);

        } else {
            binding.cardShortTerm.setStrokeColor(grey);
            binding.cardShortTerm.setStrokeWidth(3);
            binding.cardShortTerm.setCardBackgroundColor(lightGrey);

            binding.cardContract.setStrokeColor(blue);
            binding.cardContract.setStrokeWidth(6);
            binding.cardContract.setCardBackgroundColor(lightBlue);

            binding.layoutWeeklyPrice.setVisibility(View.GONE);
            binding.layoutDepositMonthly.setVisibility(View.VISIBLE);

            binding.layoutMoveOutContainer.setVisibility(View.GONE);


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

        // 주소가 있으면 먼저 좌표로 변환
        if (!location.isEmpty()) {
            convertAddressToCoordinates(location, hostId, title, description, location);
        } else {
            // 주소 없으면 바로 저장
            if (isShortTerm) {
                saveShortTermProperty(hostId, title, description, location,null);
            } else {
                saveLeaseTransferProperty(hostId, title, description, location,null);
            }
        }
    }

    /**
     * 카카오 API로 주소를 위도/경도로 변환
     */
    private void convertAddressToCoordinates(String address, String hostId, String title, String description, String location) {
        android.util.Log.d("Geocoding", "=== 카카오 API 좌표 변환 시작 ===");
        android.util.Log.d("Geocoding", "주소: " + address);
        android.util.Log.d("Geocoding", "API Key 길이: " + KAKAO_REST_API_KEY.length() + " chars");
        android.util.Log.d("Geocoding", "API Key 시작 5글자: " + (KAKAO_REST_API_KEY.length() >= 5 ? KAKAO_REST_API_KEY.substring(0, 5) : KAKAO_REST_API_KEY) + "...");

        executorService.execute(() -> {
            try {
                String encodedAddress = URLEncoder.encode(address, "UTF-8");
                String apiURL = "https://dapi.kakao.com/v2/local/search/address.json?query=" + encodedAddress;

                android.util.Log.d("Geocoding", "API URL: " + apiURL);

                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                // 카카오는 Authorization 헤더 사용
                String apiKey = KAKAO_REST_API_KEY.trim();
                con.setRequestProperty("Authorization", "KakaoAK " + apiKey);

                android.util.Log.d("Geocoding", "요청 헤더 설정 완료");

                int responseCode = con.getResponseCode();
                android.util.Log.d("Geocoding", "========================================");
                android.util.Log.d("Geocoding", "응답 코드: " + responseCode);
                android.util.Log.d("Geocoding", "========================================");

                BufferedReader br;
                if (responseCode == 200) {
                    br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                } else {
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream(), "UTF-8"));
                }

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                br.close();

                android.util.Log.d("Geocoding", "응답 내용: " + response.toString());

                // JSON 파싱
                JSONObject jsonResponse = new JSONObject(response.toString());

                // 에러 확인
                if (responseCode != 200) {
                    android.util.Log.e("Geocoding", "========================================");
                    android.util.Log.e("Geocoding", " API 에러 발생!");
                    android.util.Log.e("Geocoding", "응답 코드: " + responseCode);
                    android.util.Log.e("Geocoding", "응답 내용: " + response.toString());
                    android.util.Log.e("Geocoding", "========================================");

                    selectedLatitude = 0.0;
                    selectedLongitude = 0.0;
                    savePropertyWithCoordinates(hostId, title, description, location);
                    return;
                }

                // meta 정보 확인
                if (jsonResponse.has("meta")) {
                    JSONObject meta = jsonResponse.getJSONObject("meta");
                    int totalCount = meta.optInt("total_count", 0);
                    android.util.Log.d("Geocoding", "검색 결과 개수: " + totalCount);
                }

                // documents 배열에서 주소 정보 추출
                JSONArray documents = jsonResponse.optJSONArray("documents");

                if (documents != null && documents.length() > 0) {
                    JSONObject firstDoc = documents.getJSONObject(0);

                    // 카카오는 "y"가 위도, "x"가 경도
                    selectedLatitude = firstDoc.getDouble("y");
                    selectedLongitude = firstDoc.getDouble("x");

                    android.util.Log.d("Geocoding", "========================================");
                    android.util.Log.d("Geocoding", " 변환 성공! ");
                    android.util.Log.d("Geocoding", "위도(y): " + selectedLatitude);
                    android.util.Log.d("Geocoding", "경도(x): " + selectedLongitude);

                    // 추가 정보 로그
                    String addressName = firstDoc.optString("address_name", "");
                    String roadAddressName = "";
                    if (firstDoc.has("road_address") && !firstDoc.isNull("road_address")) {
                        JSONObject roadAddress = firstDoc.getJSONObject("road_address");
                        roadAddressName = roadAddress.optString("address_name", "");
                    }

                    android.util.Log.d("Geocoding", "지번 주소: " + addressName);
                    if (!roadAddressName.isEmpty()) {
                        android.util.Log.d("Geocoding", "도로명 주소: " + roadAddressName);
                    }
                    android.util.Log.d("Geocoding", "========================================");
                } else {
                    android.util.Log.e("Geocoding", "========================================");
                    android.util.Log.e("Geocoding", "❌ 주소 변환 실패: documents 배열이 비어있음");
                    android.util.Log.e("Geocoding", "전체 응답: " + response.toString());
                    android.util.Log.e("Geocoding", "========================================");
                    selectedLatitude = 0.0;
                    selectedLongitude = 0.0;
                }

                savePropertyWithCoordinates(hostId, title, description, location);

            } catch (Exception e) {
                android.util.Log.e("Geocoding", "========================================");
                android.util.Log.e("Geocoding", "❌ 예외 발생!", e);
                android.util.Log.e("Geocoding", "에러 타입: " + e.getClass().getSimpleName());
                android.util.Log.e("Geocoding", "에러 메시지: " + e.getMessage());
                android.util.Log.e("Geocoding", "========================================");
                e.printStackTrace();

                selectedLatitude = 0.0;
                selectedLongitude = 0.0;

                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "주소 변환 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    savePropertyWithCoordinates(hostId, title, description, location);
                });
            }
        });
    }
    private void calculatePropertyScores(ScoreCallback callback) {
        executorService.execute(() -> {
            try {
                int subwayScore = calculateFacilityScore(selectedLatitude, selectedLongitude, "SW8", 500);
                int laundryScore = calculateFacilityScore(selectedLatitude, selectedLongitude, "BK9", 500);
                int martScore = calculateFacilityScore(selectedLatitude, selectedLongitude, "MT1", 500);
                int convenienceScore = calculateFacilityScore(selectedLatitude, selectedLongitude, "CS2", 500);

                int optionScore = getOptionsScore();
                int overall = (subwayScore + laundryScore + martScore + convenienceScore + optionScore) / 5;

                HashMap<String, Object> scores = new HashMap<>();
                scores.put("subway", subwayScore);
                scores.put("laundry", laundryScore);
                scores.put("mart", martScore);
                scores.put("convenience", convenienceScore);
                scores.put("options", optionScore);
                scores.put("overall", overall);

                requireActivity().runOnUiThread(() -> {
                    if (callback != null) callback.onScoreCalculated(scores); // 전체 맵 전달
                });

                android.util.Log.d("PropertyScores", scores.toString());

            } catch (Exception e) {
                e.printStackTrace();
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "점수 계산 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }



    private HashMap<String, Object> createScoresMap(int subway, int laundry, int mart, int convenience, int options, int overall) {
        HashMap<String, Object> scores = new HashMap<>();
        scores.put("subway", subway);           // 지하철 점수
        scores.put("laundry", laundry);         // 세탁소 점수         // 마트 점수
        scores.put("convenience", convenience); // 편의점 점수
        scores.put("options", options);         // 옵션 점수
        scores.put("overall", overall);         // 종합 점수
        return scores;
    }

    private int getOptionsScore() {
        int optionCount = 0;
        if (isFullOptionSelected) optionCount++;
        if (isAirconSelected) optionCount++;
        if (isWasherSelected) optionCount++;
        if (isRefrigeratorSelected) optionCount++;
        if (isBedSelected) optionCount++;
        if (isDeskSelected) optionCount++;
        return (int) ((optionCount / 6.0) * 100);
    }
    /**
     * 주변 시설 점수 계산
     * categoryCode: CS2, PM9 등
     * radius: 검색 반경 (m)
     */
    private int calculateFacilityScore(double lat, double lng, String categoryCode, int radius) {
        if (lat == 0.0 && lng == 0.0) return 0;

        int maxScore = 100;
        int score = 0;

        try {
            String apiURL = String.format(
                    "https://dapi.kakao.com/v2/local/search/category.json?category_group_code=%s&y=%f&x=%f&radius=%d",
                    categoryCode, lat, lng, radius
            );

            URL url = new URL(apiURL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Authorization", "KakaoAK " + KAKAO_REST_API_KEY.trim());

            int responseCode = con.getResponseCode();
            if (responseCode != 200) return 0;

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) response.append(line);
            br.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray documents = jsonResponse.optJSONArray("documents");

            if (documents != null) {
                for (int i = 0; i < documents.length(); i++) {
                    JSONObject doc = documents.getJSONObject(i);
                    double facilityLat = doc.getDouble("y");
                    double facilityLng = doc.getDouble("x");

                    double distance = getDistance(lat, lng, facilityLat, facilityLng); // m 단위
                    int distanceScore = (int) Math.max(0, maxScore - distance / 10);  // 가까울수록 점수 높음
                    score += distanceScore;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Math.min(score, 100); // 최대 100점
    }

    /**
     * 거리 계산 (Haversine 공식)
     */
    private double getDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // 지구 반경 (m)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }



    /**
     * 좌표 변환 후 실제 저장 수행
     */
    private void savePropertyWithCoordinates(String hostId, String title, String description, String location) {
        android.util.Log.d("Geocoding", "=== 저장 시작 (위도: " + selectedLatitude + ", 경도: " + selectedLongitude + ") ===");

        // 1. 점수 계산
        calculatePropertyScores(overallScore -> {
            // 옵션 점수 포함 전체 scores 맵 생성
            int optionScore = getOptionsScore();
            calculatePropertyScores(scores -> {
                requireActivity().runOnUiThread(() -> {
                    if (isShortTerm) {
                        saveShortTermProperty(hostId, title, description, location, scores);
                    } else {
                        saveLeaseTransferProperty(hostId, title, description, location, scores);
                    }
                });
            });

        });
    }


    // ShortTerm 저장 시 scores 포함
    private void saveShortTermProperty(String hostId, String title, String description, String location, HashMap<String, Object> scores) {
        String weeklyPrice = binding.etWeeklyPrice.getText().toString().trim();
        String roomType = binding.etRoomType.getText().toString().trim();
        String areaStr = binding.etArea.getText().toString().trim();
        String floorStr = binding.etFloor.getText().toString().trim();
        Date moveInDate = (Date) binding.etMoveInDate.getTag();
        Date moveOutDate = (Date) binding.etMoveOutDate.getTag();



        if (weeklyPrice.isEmpty()) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "주당 가격을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isShortTerm) {
            moveOutDate = (Date) binding.etMoveOutDate.getTag();
            if (moveOutDate == null) {
                binding.btnSubmit.setEnabled(true);
                Toast.makeText(requireContext(), "입주 종료 날짜를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (moveInDate == null) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "입주 가능 날짜를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }


        if (roomType.isEmpty()) roomType = "원룸";

        double area = 20.0;
        if (!areaStr.isEmpty()) {
            try {
                area = Double.parseDouble(areaStr);
                if (area <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                binding.btnSubmit.setEnabled(true);
                Toast.makeText(requireContext(), "올바른 면적을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }

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
            pricing.put("weeklyPrice", Integer.parseInt(weeklyPrice));
            pricing.put("type", "short_term");

            HashMap<String, Object> locationMap = createLocationFromInput(location);
            HashMap<String, Object> amenities = getAmenitiesFromButtons();

            ShortTerm property = new ShortTerm(
                    title,
                    description.isEmpty() ? "매물 설명 없음" : description,
                    hostId,
                    uploadedImageUrls,
                    roomType,
                    floor,
                    area,
                    moveInDate,
                    moveOutDate,
                    pricing,
                    locationMap,
                    amenities,
                    scores // 여기 점수 넣기
            );

            shortTermRepo.save(property, this::handleSaveResult);

        } catch (NumberFormatException e) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "올바른 가격을 입력해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    // LeaseTransfer 저장 시 scores 포함
    private void saveLeaseTransferProperty(String hostId, String title, String description, String location, HashMap<String, Object> scores) {
        String deposit = binding.etDeposit.getText().toString().trim();
        String monthlyRent = binding.etMonthlyRent.getText().toString().trim();
        String roomType = binding.etRoomType.getText().toString().trim();
        String areaStr = binding.etArea.getText().toString().trim();
        String floorStr = binding.etFloor.getText().toString().trim();


        Date moveInDate = (Date) binding.etMoveInDate.getTag(); // 등록일은 moveInDate로 처리
        if (moveInDate == null) moveInDate = new Date(); // 기본값 현재


        if (deposit.isEmpty() || monthlyRent.isEmpty()) {
            binding.btnSubmit.setEnabled(true);
            Toast.makeText(requireContext(), "보증금과 월세를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (roomType.isEmpty()) roomType = "원룸";

        double area = 20.0;
        if (!areaStr.isEmpty()) {
            try {
                area = Double.parseDouble(areaStr);
                if (area <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                binding.btnSubmit.setEnabled(true);
                Toast.makeText(requireContext(), "올바른 면적을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }

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

            LeaseTransfer property = new LeaseTransfer(
                    title,
                    description.isEmpty() ? "매물 설명 없음" : description,
                    hostId,
                    uploadedImageUrls,
                    roomType,
                    floor,
                    area,
                    moveInDate,
                    new Date(),
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
            location.put("latitude", 0);
            location.put("longitude", 0);
        } else {
            location.put("address", locationInput);
            location.put("latitude", selectedLatitude);
            location.put("longitude", selectedLongitude);
        }
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

    private void showDatePicker(DateSelectedCallback callback, boolean isMoveIn) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    Date selectedDate = calendar.getTime();
                    String dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);

                    if (isMoveIn) {
                        binding.etMoveInDate.setTag(selectedDate); // Date 객체 저장
                        binding.etMoveInDate.setText(dateStr);
                    } else {
                        binding.etMoveOutDate.setTag(selectedDate); // Date 객체 저장
                        binding.etMoveOutDate.setText(dateStr);
                    }

                    if (callback != null) callback.onDateSelected(dateStr);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();
        binding = null;
    }
    interface ScoreCallback {
        void onScoreCalculated(HashMap<String, Object> scores);
    }
    interface DateSelectedCallback {
        void onDateSelected(String date); // yyyy-MM-dd 형식
    }
}