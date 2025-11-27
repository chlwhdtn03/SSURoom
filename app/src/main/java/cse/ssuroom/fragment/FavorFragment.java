package cse.ssuroom.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // Import SwipeRefreshLayout

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import cse.ssuroom.R;
import cse.ssuroom.adapter.PropertyListAdapter;
import cse.ssuroom.database.LeaseTransfer;
import cse.ssuroom.database.LeaseTransferRepository;
import cse.ssuroom.database.Property;
import cse.ssuroom.database.ShortTerm;
import cse.ssuroom.database.ShortTermRepository;
import cse.ssuroom.user.User;

public class FavorFragment extends Fragment {
    private RecyclerView recyclerView;
    private PropertyListAdapter adapter;
    private List<Property> favoriteProperties = new ArrayList<>();
    private TextView emptyMessageTextView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ShortTermRepository shortTermRepo;
    private LeaseTransferRepository leaseTransferRepo;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        shortTermRepo = new ShortTermRepository();
        leaseTransferRepo = new LeaseTransferRepository();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recycler_view_favorites);
        emptyMessageTextView = view.findViewById(R.id.text_view_empty_favorites);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        setupRecyclerView();
        
        swipeRefreshLayout.setOnRefreshListener(() -> { // swipe리스너
            loadFavoriteProperties();
        });

        loadFavoriteProperties();
    }

    private void setupRecyclerView() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : null;

        adapter = new PropertyListAdapter(
                getContext(),
                favoriteProperties,
                R.layout.item_room_list,
                currentUid, // 🔹 여기서 현재 UID 전달
                property -> { // 지도 버튼 클릭 리스너
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
                                Toast.makeText(getContext(), "위치 정보 형식이 올바르지 않습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "지도 이동 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "위치 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    // 네비게이션 selected에서 사용하기 위해 public으로 전환함
    public void loadFavoriteProperties() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            updateAdapter(new ArrayList<>()); // 그냥 빈 리스트
            return;
        }
        String uid = currentUser.getUid();

        db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null && user.getFavorites() != null && !user.getFavorites().isEmpty()) {
                    fetchPropertiesByIds(user.getFavorites());
                } else {
                    updateAdapter(new ArrayList<>());
                }
            } else {
                Toast.makeText(getContext(), "잘못된 계정입니다", Toast.LENGTH_SHORT).show();
                updateAdapter(new ArrayList<>());
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            updateAdapter(new ArrayList<>());
        });
    }

    private void fetchPropertiesByIds(List<String> propertyIds) {
        List<Property> combinedList = new ArrayList<>();
        final int[] tasksCompleted = {0};
        int totalTasks = 2; // 매물 종류 2가지

        // 단기 임대 fetch
        shortTermRepo.findAllByIds(propertyIds, shortTerms -> {
            synchronized (combinedList) {
                combinedList.addAll(shortTerms);
            }
            tasksCompleted[0]++;
            if (tasksCompleted[0] == totalTasks) {
                updateAdapter(combinedList);
            }
        });

        // 계약양도 fetch
        leaseTransferRepo.findAllByIds(propertyIds, leaseTransfers -> {
            synchronized (combinedList) {
                combinedList.addAll(leaseTransfers);
            }
            tasksCompleted[0]++;
            if (tasksCompleted[0] == totalTasks) {
                updateAdapter(combinedList);
            }
        });
    }

    private void updateAdapter(List<Property> properties) {
        favoriteProperties.clear();
        favoriteProperties.addAll(properties);
        adapter.notifyDataSetChanged();

        if (properties.isEmpty()) {
            emptyMessageTextView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyMessageTextView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        swipeRefreshLayout.setRefreshing(false); // Stop refreshing indicator
    }
}