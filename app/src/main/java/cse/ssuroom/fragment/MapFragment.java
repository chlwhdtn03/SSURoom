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
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.util.EnumSet;
import java.util.List;

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
public class MapFragment extends Fragment implements OnMapReadyCallback {
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

    Clusterer<ItemKey> clusterer = new Clusterer.Builder<ItemKey>().build();

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
            new FilterBottomSheet().show(getChildFragmentManager(), "filter");
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
    public void onMapReady(@NonNull NaverMap naverMap) {
        naverMap.setCameraPosition(new CameraPosition(new LatLng(37.4959, 126.9577), 15));
        naverMap.setLocationSource(locationSource);
        UiSettings us = naverMap.getUiSettings();
        us.setLocationButtonEnabled(true);


        leaseRepo.findAll((list) -> {
            for(LeaseTransfer lease : list) {
                try {
                    Marker marker = new Marker();
                    marker.setPosition(new LatLng((Double) lease.getLocation().get("latitude"), (Double) lease.getLocation().get("longitude")));
                    marker.setIcon(OverlayImage.fromResource(R.drawable.leaseicon));
                    marker.setCaptionAligns(Align.Top);
                    marker.setCaptionTextSize(16);
                    marker.setCaptionText(lease.getPricing().get("deposit") + "/" + lease.getPricing().get("monthlyRent"));
                    marker.setMap(naverMap);
                } catch (Exception e) {
                    Log.e("ShortRepo", "필수 데이터 없음");
                }
            }
        });

        shortRepo.findAll((list) -> {
            for(ShortTerm lease : list) {
                try {
                    Marker marker = new Marker();
                    marker.setPosition(new LatLng((Double) lease.getLocation().get("latitude"), (Double) lease.getLocation().get("longitude")));
                    marker.setIcon(OverlayImage.fromResource(R.drawable.shorticon));
                    marker.setCaptionAligns(Align.Top);
                    marker.setCaptionTextSize(16);
                    marker.setCaptionText(lease.getPricing().get("weeklyPrice") + "");
                    marker.setMap(naverMap);
                } catch (Exception e) {
                    Log.e("ShortRepo", "필수 데이터 없음");
                }
            }
        });
    }

}