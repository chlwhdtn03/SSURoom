package cse.ssuroom.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import cse.ssuroom.R;
import cse.ssuroom.adapter.PropertyListAdapter;
import cse.ssuroom.bottomsheet.FilterBottomSheet;
import cse.ssuroom.database.LeaseTransferRepository;
import cse.ssuroom.database.Property;
import cse.ssuroom.database.ShortTermRepository;

public class RoomlistFragment extends Fragment {

    private static final String TAG = "RoomlistFragment";

    private LinearLayout header;
    private SwitchMaterial rentTypeSwitch;
    private TextView shortTermRentLabel;
    private TextView leaseTransferLabel;
    private TextView listingInfo;
    private RecyclerView recyclerView;
    private Button filterButton;

    private ShortTermRepository shortTermRepo;
    private LeaseTransferRepository leaseTransferRepo;

    private PropertyListAdapter adapter;
    private List<Property> propertyList = new ArrayList<>();
    private List<Property> allProperties = new ArrayList<>(); // 전체 데이터 저장



    // 필터 값
    private float filterMinScore = 0;
    private float filterMaxScore = 100;
    private float filterMinPrice = 0;
    private float filterMaxPrice = Float.MAX_VALUE;
    private float filterMinDuration = 1;
    private float filterMaxDuration = 52;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_roomlist, container, false);

        header = view.findViewById(R.id.header);
        rentTypeSwitch = view.findViewById(R.id.rent_type_switch);
        shortTermRentLabel = view.findViewById(R.id.short_term_rent_label);
        leaseTransferLabel = view.findViewById(R.id.lease_transfer_label);
        listingInfo = view.findViewById(R.id.listing_info);
        recyclerView = view.findViewById(R.id.recycler_view);
        filterButton = view.findViewById(R.id.filter_button);



        shortTermRepo = new ShortTermRepository();
        leaseTransferRepo = new LeaseTransferRepository();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : null;


        adapter = new PropertyListAdapter(
                requireContext(),
                propertyList,
                R.layout.item_room_list,
                currentUid, // 🔹 현재 UID
                property -> { // 🔹 지도 클릭 리스너
                    if (property.getLocation() != null) {
                        try {
                            Object latObj = property.getLocation().get("latitude");
                            Object lngObj = property.getLocation().get("longitude");

                            if (latObj instanceof Number && lngObj instanceof Number) {
                                double lat = ((Number) latObj).doubleValue();
                                double lng = ((Number) lngObj).doubleValue();

                                if (getActivity() instanceof cse.ssuroom.MainActivity) {
                                    ((cse.ssuroom.MainActivity) getActivity()).navigateToMap(lat, lng);
                                }
                            } else {
                                Log.e(TAG, "Invalid location data types: lat=" + latObj + ", lng=" + lngObj);
                                Toast.makeText(getContext(), "위치 정보 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error navigating to map", e);
                            Toast.makeText(getContext(), "지도 이동 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "위치 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        rentTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateView(isChecked);
                loadProperties(isChecked);
            }
        });

        // 필터 버튼 클릭
        filterButton.setOnClickListener(v -> showFilterDialog());

        updateView(rentTypeSwitch.isChecked());
        loadProperties(rentTypeSwitch.isChecked());

        return view;
    }

    private void updateView(boolean isChecked) {
        if (isChecked) {
            // 계약양도
            header.setBackgroundColor(Color.parseColor("#4285F4")); // Google Blue
            shortTermRentLabel.setTextColor(Color.WHITE);
            leaseTransferLabel.setTextColor(Color.BLACK);
        } else {
            // 단기임대
            header.setBackgroundColor(Color.parseColor("#5CB85C")); // Green
            shortTermRentLabel.setTextColor(Color.BLACK);
            leaseTransferLabel.setTextColor(Color.WHITE);
        }
    }

    private double getOverallScore(Property property) {
        if (property.getScores() != null) {
            Object overallScore = property.getScores().get("overall");
            if (overallScore != null) {
                return ((Number) overallScore).doubleValue();
            }
        }
        return 0.0; // 점수가 없으면 0점으로 처리
    }

    private void loadProperties(boolean isLeaseTransfer) {
        Log.d(TAG, "==========================================");
        Log.d(TAG, "매물 로드: " + (isLeaseTransfer ? "계약양도" : "단기임대"));

        allProperties.clear();
        propertyList.clear();
        adapter.notifyDataSetChanged();

        // 필터 초기화
        resetFilters();

        if (isLeaseTransfer) {
            // 계약양도 매물 조회
            leaseTransferRepo.findAll(properties -> {
                Log.d(TAG, "계약양도 매물 조회 완료: " + properties.size() + "개");

                // 점수순 정렬 (높은 점수 먼저)
                properties.sort((p1, p2) -> {
                    double score1 = getOverallScore(p1);
                    double score2 = getOverallScore(p2);
                    return Double.compare(score2, score1); // 내림차순
                });

                allProperties.addAll(properties);
                propertyList.addAll(properties);
                adapter.notifyDataSetChanged();
                updateListingInfo(properties.size());
            });
        } else {
            // 단기임대 매물 조회
            shortTermRepo.findAll(properties -> {
                Log.d(TAG, "단기임대 매물 조회 완료: " + properties.size() + "개");

                // 점수순 정렬 (높은 점수 먼저)
                properties.sort((p1, p2) -> {
                    double score1 = getOverallScore(p1);
                    double score2 = getOverallScore(p2);
                    return Double.compare(score2, score1); // 내림차순
                });

                allProperties.addAll(properties);
                propertyList.addAll(properties);
                adapter.notifyDataSetChanged();
                updateListingInfo(properties.size());
            });
        }
    }

    private void showFilterDialog() {
        FilterBottomSheet filterDialog = FilterBottomSheet.newInstance(rentTypeSwitch.isChecked());
        filterDialog.setFilterListener((minScore, maxScore, minPrice, maxPrice, minDuration, maxDuration) -> {
            // 필터 적용
            filterMinScore = minScore;
            filterMaxScore = maxScore;
            filterMinPrice = minPrice;
            filterMaxPrice = maxPrice;
            filterMinDuration = minDuration;
            filterMaxDuration = maxDuration;
            applyFilters();
        });
        filterDialog.show(getChildFragmentManager(), "FilterDialog");
    }

    private void applyFilters() {
        List<Property> filteredList = new ArrayList<>();

        Log.d(TAG, "========== 필터 적용 시작 ==========");
        Log.d(TAG, "전체 매물 수: " + allProperties.size());
        Log.d(TAG, "필터 조건 - 점수: " + filterMinScore + " ~ " + filterMaxScore);
        Log.d(TAG, "필터 조건 - 가격: " + filterMinPrice + " ~ " + filterMaxPrice);
        Log.d(TAG, "필터 조건 - 기간: " + filterMinDuration + " ~ " + filterMaxDuration + "주");

        for (Property property : allProperties) {
            // 점수 필터
            double score = getOverallScore(property);
            Log.d(TAG, "매물: " + property.getTitle() + " - 점수: " + score);
            if (score < filterMinScore || score > filterMaxScore) {
                Log.d(TAG, "  -> 점수로 필터링됨");
                continue;
            }

            // 가격 필터
            double price = getPrice(property);
            Log.d(TAG, "  -> 가격: " + price);
            if (price < filterMinPrice || price > filterMaxPrice) {
                Log.d(TAG, "  -> 가격으로 필터링됨");
                continue;
            }

            // 기간 필터 (주 단위)
            long durationWeeks = getDurationInWeeks(property);
            Log.d(TAG, "  -> 기간: " + durationWeeks + "주");
            if (durationWeeks < filterMinDuration || (filterMaxDuration < 52 && durationWeeks > filterMaxDuration)) {
                Log.d(TAG, "  -> 기간으로 필터링됨");
                continue;
            }

            Log.d(TAG, "  -> 필터 통과!");
            filteredList.add(property);
        }

        Log.d(TAG, "필터링 후 매물 수: " + filteredList.size());
        Log.d(TAG, "========== 필터 적용 완료 ==========");

        // 정렬 후 업데이트
        filteredList.sort((p1, p2) -> {
            double score1 = getOverallScore(p1);
            double score2 = getOverallScore(p2);
            return Double.compare(score2, score1);
        });

        propertyList.clear();
        propertyList.addAll(filteredList);
        adapter.notifyDataSetChanged();
        updateListingInfo(filteredList.size());
    }

    private double getPrice(Property property) {
        if (property.getPricing() == null) {
            Log.d(TAG, "pricing이 null");
            return 0;
        }

        String type = (String) property.getPricing().get("type");
        Log.d(TAG, "pricing type: " + type);

        if ("short_term".equals(type)) {
            Object weeklyPrice = property.getPricing().get("weeklyPrice");
            Log.d(TAG, "weeklyPrice 원본: " + weeklyPrice + " (타입: " + (weeklyPrice != null ? weeklyPrice.getClass().getName() : "null") + ")");

            if (weeklyPrice instanceof Number) {
                double price = ((Number) weeklyPrice).doubleValue();
                Log.d(TAG, "변환된 weeklyPrice: " + price);
                return price;
            }
        } else if ("lease_transfer".equals(type)) {
            Object monthlyRent = property.getPricing().get("monthlyRent");
            Log.d(TAG, "monthlyRent 원본: " + monthlyRent + " (타입: " + (monthlyRent != null ? monthlyRent.getClass().getName() : "null") + ")");

            if (monthlyRent instanceof Number) {
                double price = ((Number) monthlyRent).doubleValue();
                Log.d(TAG, "변환된 monthlyRent: " + price);
                return price;
            }
        }

        Log.d(TAG, "가격을 찾을 수 없음");
        return 0;
    }

    private long getDurationInWeeks(Property property) {
        if (property.getMoveInDate() == null) {
            Log.d(TAG, "moveInDate가 null");
            return 1; // 최소 1주로 설정
        }

        if (property.getMoveOutDate() == null) {
            Log.d(TAG, "moveOutDate가 null (계약양도?)");
            return 52; // 계약양도는 1년으로 설정
        }

        long diffMs = property.getMoveOutDate().getTime() - property.getMoveInDate().getTime();
        long weeks = diffMs / (7 * 24 * 60 * 60 * 1000);
        Log.d(TAG, "계산된 기간: " + weeks + "주");

        if (weeks <= 0) weeks = 1; // 최소 1주

        return weeks;
    }

    private void resetFilters() {
        filterMinScore = 0;
        filterMaxScore = 100;

        // 가격 초기화 (단기임대 vs 계약양도)
        if (rentTypeSwitch.isChecked()) {
            // 계약양도 - 만원 단위
            filterMinPrice = 10;   // 10만원
            filterMaxPrice = 200;  // 200만원
        } else {
            // 단기임대 - 원 단위
            filterMinPrice = 50000;
            filterMaxPrice = 500000;
        }

        filterMinDuration = 1;
        filterMaxDuration = 52;
    }

    private void updateListingInfo(int count) {
        listingInfo.setText("슈방 점수순 · " + count + "개");
    }
}