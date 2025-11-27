package cse.ssuroom.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.ArrayList;
import java.util.List;

import cse.ssuroom.R;
import cse.ssuroom.adapter.PropertyListAdapter;
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

    private ShortTermRepository shortTermRepo;
    private LeaseTransferRepository leaseTransferRepo;

    private PropertyListAdapter adapter;
    private List<Property> propertyList = new ArrayList<>();

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

        // Repository 초기화
        shortTermRepo = new ShortTermRepository();
        leaseTransferRepo = new LeaseTransferRepository();

        // RecyclerView 설정
        adapter = new PropertyListAdapter(requireContext(), propertyList, R.layout.item_room_list, property -> {
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
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        rentTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateView(isChecked);
                loadProperties(isChecked);
            }
        });

        // Initial state
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

    private void loadProperties(boolean isLeaseTransfer) {
        Log.d(TAG, "==========================================");
        Log.d(TAG, "매물 로드: " + (isLeaseTransfer ? "계약양도" : "단기임대"));

        propertyList.clear();
        adapter.notifyDataSetChanged();

        if (isLeaseTransfer) {
            // 계약양도 매물 조회
            leaseTransferRepo.findAll(properties -> {
                Log.d(TAG, "계약양도 매물 조회 완료: " + properties.size() + "개");
                propertyList.addAll(properties);
                adapter.notifyDataSetChanged();
                updateListingInfo(properties.size());
            });
        } else {
            // 단기임대 매물 조회
            shortTermRepo.findAll(properties -> {
                Log.d(TAG, "단기임대 매물 조회 완료: " + properties.size() + "개");
                propertyList.addAll(properties);
                adapter.notifyDataSetChanged();
                updateListingInfo(properties.size());
            });
        }
    }

    private void updateListingInfo(int count) {
        listingInfo.setText("슈방 점수순 · " + count + "개");
    }
}