package cse.ssuroom.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.util.FusedLocationSource;

import cse.ssuroom.R;
import cse.ssuroom.bottomsheet.FilterBottomSheet;
import cse.ssuroom.bottomsheet.UploadPropertyBottomSheet;
import cse.ssuroom.databinding.FragmentMapBinding;

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

        return binding.getRoot();
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        naverMap.setCameraPosition(new CameraPosition(new LatLng(37.4959, 126.9577), 15));
        naverMap.setLocationSource(locationSource);
        UiSettings us = naverMap.getUiSettings();

        us.setLocationButtonEnabled(true);
    }

}