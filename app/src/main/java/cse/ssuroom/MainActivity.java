package cse.ssuroom;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import cse.ssuroom.databinding.ActivityMainBinding;
import cse.ssuroom.fragment.ChatFragment;
import cse.ssuroom.fragment.FavorFragment;
import cse.ssuroom.fragment.MapFragment;
import cse.ssuroom.fragment.MyInfoFragment;
import cse.ssuroom.fragment.RoomlistFragment;
import cse.ssuroom.notification.NotificationHelper;

public class MainActivity extends AppCompatActivity {

    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private final RoomlistFragment roomlistFragment = new RoomlistFragment();
    private final MapFragment mapFragment = new MapFragment();
    private final FavorFragment favorFragment = new FavorFragment();
    private final ChatFragment chatFragment = new ChatFragment();
    private final MyInfoFragment myInfoFragment = new MyInfoFragment();
    private Fragment activeFragment = roomlistFragment;
    private ActivityMainBinding binding;

    // 알림 권한 요청 런처
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                } else {
                    Toast.makeText(this, "알림 권한을 거부하시면 채팅 알림을 받을 수 없습니다.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 알림 채널 생성
        NotificationHelper.createNotificationChannel(this);

        // 알림 권한 요청
        requestNotificationPermission();

        // 백그라운드에서 FCM 토큰 갱신
        updateFCMToken();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        fragmentManager.beginTransaction().add(R.id.screen, myInfoFragment, "myInfo").hide(myInfoFragment).commit();
        fragmentManager.beginTransaction().add(R.id.screen, favorFragment, "favor").hide(favorFragment).commit();
        fragmentManager.beginTransaction().add(R.id.screen, mapFragment, "map").hide(mapFragment).commit();
        fragmentManager.beginTransaction().add(R.id.screen, chatFragment, "chat").hide(chatFragment).commit();
        fragmentManager.beginTransaction().add(R.id.screen, roomlistFragment, "roomlist").commit();


        changeFragment(roomlistFragment);
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_roomlist) {
                changeFragment(roomlistFragment);
            } else if (itemId == R.id.action_favor) {
                favorFragment.loadFavoriteProperties();  // 즐겨찾기 내역 바로 반영될 수 있도록
                changeFragment(favorFragment);
            }
            else if (itemId == R.id.action_map) {
                changeFragment(mapFragment);
            }
            else if (itemId == R.id.action_chat) {
                chatFragment.loadChatRooms();
                changeFragment(chatFragment);
            }
            else if (itemId == R.id.action_myinfo) {
                myInfoFragment.updateUserInfo();  // 내 정보 바로 반영될 수 있도록
                changeFragment(myInfoFragment);
            }
            return true;
        });
    }

    private void updateFCMToken() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return; // 로그인 상태가 아니면 중단

        String userId = currentUser.getUid();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("MainActivity", "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                            .update("fcmToken", token)
                            .addOnSuccessListener(aVoid -> Log.d("MainActivity", "FCM token updated in background."))
                            .addOnFailureListener(e -> Log.w("MainActivity", "Error updating FCM token in background", e));
                });
    }

    private void requestNotificationPermission() {
        // Android 13 (API 33) 이상에서만 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 이미 권한이 있는지 확인
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // 권한이 없다면, 사용자에게 권한 요청 팝업을 띄움
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void changeFragment(Fragment fragment) {
        // 현재 보였던 frag는 가리고, 새로운 frag는 보이게
        fragmentManager.beginTransaction().hide(activeFragment).show(fragment).commit();
        activeFragment = fragment;
    }
    public void navigateToMap(double lat, double lng) {
        binding.bottomNavigationView.setSelectedItemId(R.id.action_map);
        mapFragment.moveCamera(lat, lng);
    }
    
    public void setBottomNavigationVisibility(int visibility) {
        binding.bottomNavigationView.setVisibility(visibility);
    }

}
