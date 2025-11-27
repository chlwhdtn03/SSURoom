package cse.ssuroom.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.clustering.Clusterer;
import com.naver.maps.map.clustering.ClusterMarkerInfo;
import com.naver.maps.map.clustering.LeafMarkerInfo;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import cse.ssuroom.ArActivity;
import cse.ssuroom.R;
import cse.ssuroom.bottomsheet.FilterBottomSheet;
import cse.ssuroom.bottomsheet.UploadPropertyBottomSheet;
import cse.ssuroom.database.LeaseTransfer;
import cse.ssuroom.database.LeaseTransferRepository;
import cse.ssuroom.database.Property;
import cse.ssuroom.database.PropertyRepository;
import cse.ssuroom.database.ShortTerm;
import cse.ssuroom.database.ShortTermRepository;
import cse.ssuroom.databinding.FragmentMapBinding;
import cse.ssuroom.map.ItemKey;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback, FilterBottomSheet.FilterListener {
    private FusedLocationSource locationSource;
    private FragmentMapBinding binding;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private LeaseTransferRepository leaseRepo;
    private ShortTermRepository shortRepo;
    private NaverMap naverMap; // Store NaverMap instance
    private LatLng pendingCameraPosition; // Store pending camera position

    private Clusterer<ItemKey> clusterer;

    // 전체 데이터 저장 (필터용)
    private List<LeaseTransfer> allLeaseList = new ArrayList<>();
    private List<ShortTerm> allShortList = new ArrayList<>();

    // 필터 값 - 슈방 점수
    private float minScore = 0;
    private float maxScore = 100;

    // 필터 값 - 임대 기간 (주 단위)
    private float minDuration = 1;
    private float maxDuration = 52;

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MapFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        // Repository 초기화
        shortRepo = new ShortTermRepository();
        leaseRepo = new LeaseTransferRepository();

        com.naver.maps.map.MapFragment mapFragment = (com.naver.maps.map.MapFragment) getChildFragmentManager().findFragmentById(R.id.map_fragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        binding = FragmentMapBinding.inflate(inflater, container, false);


        binding.filterBtn.setOnClickListener(view -> {
            FilterBottomSheet filterSheet = FilterBottomSheet.newInstance(true, false); // 가격 필터 숨김
            filterSheet.setFilterListener(this);
            filterSheet.show(getChildFragmentManager(), "filter");
        });

        binding.addBtn.setOnClickListener(view ->{
            new UploadPropertyBottomSheet().show(getChildFragmentManager(), "add");

        });

        binding.ARBtn.setOnClickListener(view -> {
            if(isARCoreSupportedAndUpToDate()) {
                startActivity(new Intent(getActivity(), ArActivity.class));
            }
        });

        maybeEnableArButton();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    public void maybeEnableArButton() {
        ArCoreApk.getInstance().checkAvailabilityAsync(getContext(), availability -> {
            if (availability.isSupported()) {
                binding.ARBtn.setVisibility(View.VISIBLE);
                binding.ARBtn.setEnabled(true);
            } else { // The device is unsupported or unknown.
                binding.ARBtn.setVisibility(View.INVISIBLE);
                binding.ARBtn.setEnabled(false);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // Verify that ARCore is installed and using the current version.
    private boolean isARCoreSupportedAndUpToDate() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(getContext());
        switch (availability) {
            case SUPPORTED_INSTALLED:
                return true;

            case SUPPORTED_APK_TOO_OLD:
            case SUPPORTED_NOT_INSTALLED:
                try {
                    // Request ARCore installation or update if needed.
                    ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(getActivity(), true);
                    switch (installStatus) {
                        case INSTALL_REQUESTED:
                            Log.i("[AR]", "ARCore installation requested.");
                            return false;
                        case INSTALLED:
                            return true;
                    }
                } catch (UnavailableException e) {
                    Log.e("[AR]", "ARCore not installed", e);
                }
                return false;

            case UNSUPPORTED_DEVICE_NOT_CAPABLE:
                // This device is not supported for AR.
                return false;

            case UNKNOWN_CHECKING:
                // ARCore is checking the availability with a remote query.
                // This function should be called again after waiting 200 ms to determine the query result.
            case UNKNOWN_ERROR:
            case UNKNOWN_TIMED_OUT:
                // There was an error checking for AR availability. This may be due to the device being offline.
                // Handle the error appropriately.
        }
        return false;
    }

    @Override
    public void onFilterApplied(float minScore, float maxScore, float minPrice, float maxPrice, float minDuration, float maxDuration) {
        // 슈방 점수 필터 적용
        this.minScore = minScore;
        this.maxScore = maxScore;

        // 임대 기간 필터 적용
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;

        // 가격 필터는 무시 (지도에서는 가격 필터 사용 안 함)

        applyFilter();
    }

    private void applyFilter() {
        if (naverMap == null) {
            return;
        }

        Map<ItemKey, Object> keyTagMap = new HashMap<>();
        AtomicInteger idCounter = new AtomicInteger(0);

        for(LeaseTransfer lease : allLeaseList) {
            if (matchesLeaseFilter(lease)) {
                try {
                    LatLng pos = new LatLng((Double) lease.getLocation().get("latitude"), (Double) lease.getLocation().get("longitude"));
                    ItemKey key = new ItemKey(idCounter.getAndIncrement(), pos, ItemKey.Type.Lease);
                    keyTagMap.put(key, lease);
                } catch (Exception e) {
                    Log.e("LeaseRepo", "필수 데이터 없음");
                }
            }
        }

        for(ShortTerm term : allShortList) {
            if (matchesShortFilter(term)) {
                try {
                    LatLng pos = new LatLng((Double) term.getLocation().get("latitude"), (Double) term.getLocation().get("longitude"));
                    ItemKey key = new ItemKey(idCounter.getAndIncrement(), pos, ItemKey.Type.Short);
                    keyTagMap.put(key, term);
                } catch (Exception e) {
                    Log.e("ShortRepo", "필수 데이터 없음");
                }
            }
        }

        if (clusterer != null) {
            clusterer.setMap(null);
        }

        Clusterer.Builder<ItemKey> builder = new Clusterer.Builder<ItemKey>();
        builder.clusterMarkerUpdater((ClusterMarkerInfo info, Marker marker) -> {
            marker.setIcon(OverlayImage.fromResource(R.drawable.bg_circle_button));
            marker.setCaptionText(String.valueOf(info.getSize()));
            marker.setCaptionAligns(Align.Center);
            marker.setCaptionColor(Color.BLACK);
            marker.setCaptionHaloColor(Color.parseColor("#00000000"));
            marker.setCaptionTextSize(20);
        });

        builder.leafMarkerUpdater((LeafMarkerInfo info, Marker marker) -> {
            ItemKey key = (ItemKey) info.getKey();
            Object tag = info.getTag();

            marker.setCaptionAligns(Align.Top);
            marker.setCaptionTextSize(16);

            String propertyId = null;

            if (key.getType() == ItemKey.Type.Lease) {
                marker.setIcon(OverlayImage.fromResource(R.drawable.leaseicon));
                if (tag instanceof LeaseTransfer) {
                    LeaseTransfer lease = (LeaseTransfer) tag;
                    marker.setCaptionText(lease.getPricing().get("deposit") + "/" + lease.getPricing().get("monthlyRent"));
                    propertyId = lease.getPropertyId();
                }
            } else if (key.getType() == ItemKey.Type.Short) {
                marker.setIcon(OverlayImage.fromResource(R.drawable.shorticon));
                if (tag instanceof ShortTerm) {
                    ShortTerm term = (ShortTerm) tag;
                    marker.setCaptionText(String.valueOf(term.getPricing().get("weeklyPrice")));
                    propertyId = term.getPropertyId();
                }
            }

            String finalPropertyId = propertyId;
            marker.setOnClickListener(overlay -> {
                if (finalPropertyId != null) {
                    RoomDetailFragment fragment = RoomDetailFragment.newInstance(finalPropertyId);
                    fragment.show(getParentFragmentManager(), "RoomDetail");
                    return true;
                }
                return false;
            });
        });

        clusterer = builder.build();
        clusterer.addAll(keyTagMap);
        clusterer.setMap(naverMap);
    }

    private boolean matchesLeaseFilter(LeaseTransfer lease) {
        // 1. 슈방 점수 체크 (scores.overall)
        Map<String, Object> scores = lease.getScores();
        if (scores != null && scores.containsKey("overall")) {
            Object overallObj = scores.get("overall");
            if (overallObj != null) {
                float overall = ((Number) overallObj).floatValue();
                if (overall < minScore || overall > maxScore) {
                    return false;
                }
            }
        }

        // 2. 임대 기간 체크 (moveInDate ~ 현재까지의 주 단위 계산)
        Date moveInDate = lease.getMoveInDate();
        if (moveInDate != null) {
            long diffInMillis = System.currentTimeMillis() - moveInDate.getTime();
            long weeksPassed = diffInMillis / (1000 * 60 * 60 * 24 * 7);

            // 이미 지난 주 수가 필터 범위를 벗어나면 제외
            if (weeksPassed > maxDuration) {
                return false;
            }
        }

        return true;
    }

    private boolean matchesShortFilter(ShortTerm term) {
        // 1. 슈방 점수 체크 (scores.overall)
        Map<String, Object> scores = term.getScores();
        if (scores != null && scores.containsKey("overall")) {
            Object overallObj = scores.get("overall");
            if (overallObj != null) {
                float overall = ((Number) overallObj).floatValue();
                if (overall < minScore || overall > maxScore) {
                    return false;
                }
            }
        }

        // 2. 임대 기간 체크 (moveInDate ~ moveOutDate)
        Date moveInDate = term.getMoveInDate();
        Date moveOutDate = term.getMoveOutDate();

        if (moveInDate != null && moveOutDate != null) {
            long diffInMillis = moveOutDate.getTime() - moveInDate.getTime();
            long weeksTotal = diffInMillis / (1000 * 60 * 60 * 24 * 7);

            // 임대 기간이 필터 범위를 벗어나면 제외
            if (weeksTotal < minDuration || weeksTotal > maxDuration) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setCameraPosition(new CameraPosition(new LatLng(37.4959, 126.9577), 15));
        naverMap.setLocationSource(locationSource);
        UiSettings us = naverMap.getUiSettings();
        us.setLocationButtonEnabled(true);

        if (pendingCameraPosition != null) {
            naverMap.setCameraPosition(new CameraPosition(pendingCameraPosition, 17));
            pendingCameraPosition = null;
        }

        Map<ItemKey, Object> keyTagMap = new HashMap<>();
        AtomicBoolean leaseLoaded = new AtomicBoolean(false);
        AtomicBoolean shortLoaded = new AtomicBoolean(false);
        AtomicInteger idCounter = new AtomicInteger(0);

        Runnable checkCompletion = () -> {
            if (leaseLoaded.get() && shortLoaded.get()) {
                if (clusterer != null) {
                    clusterer.setMap(null);
                }

                Clusterer.Builder<ItemKey> builder = new Clusterer.Builder<ItemKey>();
                builder.clusterMarkerUpdater((ClusterMarkerInfo info, Marker marker) -> {
                    marker.setIcon(OverlayImage.fromResource(R.drawable.bg_circle_button));
                    marker.setCaptionText(String.valueOf(info.getSize()));
                    marker.setCaptionAligns(Align.Center);
                    marker.setCaptionColor(Color.BLACK);
                    marker.setCaptionHaloColor(Color.parseColor("#00000000"));
                    marker.setCaptionTextSize(20);
                });

                builder.leafMarkerUpdater((LeafMarkerInfo info, Marker marker) -> {
                    ItemKey key = (ItemKey) info.getKey();
                    Object tag = info.getTag();

                    marker.setCaptionAligns(Align.Top);
                    marker.setCaptionTextSize(16);

                    String propertyId = null;

                    if (key.getType() == ItemKey.Type.Lease) {
                        marker.setIcon(OverlayImage.fromResource(R.drawable.leaseicon));
                        if (tag instanceof LeaseTransfer) {
                            LeaseTransfer lease = (LeaseTransfer) tag;
                            marker.setCaptionText(lease.getPricing().get("deposit") + "/" + lease.getPricing().get("monthlyRent"));
                            propertyId = lease.getPropertyId();
                        }
                    } else if (key.getType() == ItemKey.Type.Short) {
                        marker.setIcon(OverlayImage.fromResource(R.drawable.shorticon));
                        if (tag instanceof ShortTerm) {
                            ShortTerm term = (ShortTerm) tag;
                            marker.setCaptionText(String.valueOf(term.getPricing().get("weeklyPrice")));
                            propertyId = term.getPropertyId();
                        }
                    }

                    String finalPropertyId = propertyId;
                    marker.setOnClickListener(overlay -> {
                        if (finalPropertyId != null) {
                            RoomDetailFragment fragment = RoomDetailFragment.newInstance(finalPropertyId);
                            fragment.show(getParentFragmentManager(), "RoomDetail");
                            return true;
                        }
                        return false;
                    });
                });

                clusterer = builder.build();
                clusterer.addAll(keyTagMap);
                clusterer.setMap(naverMap);
            }
        };

        leaseRepo.findAll((list) -> {
            allLeaseList = list;
            for(LeaseTransfer lease : list) {
                if (matchesLeaseFilter(lease)) {
                    try {
                        LatLng pos = new LatLng((Double) lease.getLocation().get("latitude"), (Double) lease.getLocation().get("longitude"));
                        ItemKey key = new ItemKey(idCounter.getAndIncrement(), pos, ItemKey.Type.Lease);
                        keyTagMap.put(key, lease);
                    } catch (Exception e) {
                        Log.e("LeaseRepo", "필수 데이터 없음");
                    }
                }
            }
            leaseLoaded.set(true);
            checkCompletion.run();
        });

        shortRepo.findAll((list) -> {
            allShortList = list;
            for(ShortTerm term : list) {
                if (matchesShortFilter(term)) {
                    try {
                        LatLng pos = new LatLng((Double) term.getLocation().get("latitude"), (Double) term.getLocation().get("longitude"));
                        ItemKey key = new ItemKey(idCounter.getAndIncrement(), pos, ItemKey.Type.Short);
                        keyTagMap.put(key, term);
                    } catch (Exception e) {
                        Log.e("ShortRepo", "필수 데이터 없음");
                    }
                }
            }
            shortLoaded.set(true);
            checkCompletion.run();
        });
    }

    public void moveCamera(double lat, double lng) {
        LatLng position = new LatLng(lat, lng);
        if (naverMap != null) {
            naverMap.setCameraPosition(new CameraPosition(position, 17));
        } else {
            pendingCameraPosition = position;
        }
    }

}