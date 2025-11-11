package cse.ssuroom.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import cse.ssuroom.R;

public class RoomlistFragment extends Fragment {

    private LinearLayout header;
    private SwitchMaterial rentTypeSwitch;
    private TextView shortTermRentLabel;
    private TextView leaseTransferLabel;
    private RecyclerView recyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_roomlist, container, false);

        header = view.findViewById(R.id.header);
        rentTypeSwitch = view.findViewById(R.id.rent_type_switch);
        shortTermRentLabel = view.findViewById(R.id.short_term_rent_label);
        leaseTransferLabel = view.findViewById(R.id.lease_transfer_label);
        recyclerView = view.findViewById(R.id.recycler_view);

        rentTypeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateView(isChecked);
            }
        });

        // Initial state
        updateView(rentTypeSwitch.isChecked());

        // TODO: Setup RecyclerView with an adapter

        return view;
    }

    private void updateView(boolean isChecked) {
        if (isChecked) {
            // 계약양도
            header.setBackgroundColor(Color.parseColor("#4285F4")); // Google Blue 이게 좋아보임
            shortTermRentLabel.setTextColor(Color.WHITE);
            leaseTransferLabel.setTextColor(Color.BLACK);
        } else {
            // 단기임대
            header.setBackgroundColor(Color.parseColor("#5CB85C")); // Green
            shortTermRentLabel.setTextColor(Color.BLACK);
            leaseTransferLabel.setTextColor(Color.WHITE);
        }
        // TODO: Add logic to reload RecyclerView data based on the selected type
    }
}
