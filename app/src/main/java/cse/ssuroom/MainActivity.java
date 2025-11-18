package cse.ssuroom;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import cse.ssuroom.databinding.ActivityMainBinding;
import cse.ssuroom.fragment.ChatFragment;
import cse.ssuroom.fragment.FavorFragment;
import cse.ssuroom.fragment.MapFragment;
import cse.ssuroom.fragment.MyInfoFragment;
import cse.ssuroom.fragment.RoomlistFragment;

public class MainActivity extends AppCompatActivity {

    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private final RoomlistFragment roomlistFragment = new RoomlistFragment();
    private final MapFragment mapFragment = new MapFragment();
    private final FavorFragment favorFragment = new FavorFragment();
    private final ChatFragment chatFragment = new ChatFragment();
    private final MyInfoFragment myInfoFragment = new MyInfoFragment();

    private Fragment activeFragment = mapFragment;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        fragmentManager.beginTransaction().add(R.id.screen, myInfoFragment, "myInfo").hide(myInfoFragment).commit();
        fragmentManager.beginTransaction().add(R.id.screen, favorFragment, "favor").hide(favorFragment).commit();
        fragmentManager.beginTransaction().add(R.id.screen, mapFragment, "map").hide(mapFragment).commit();
        fragmentManager.beginTransaction().add(R.id.screen, chatFragment, "chat").hide(chatFragment).commit();
        fragmentManager.beginTransaction().add(R.id.screen, roomlistFragment, "roomlist").commit();



        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_roomlist) {
                changeFragment(roomlistFragment);
            } else if (itemId == R.id.action_favor) {
                changeFragment(favorFragment);
            } else if (itemId == R.id.action_map) {
                changeFragment(mapFragment);
            } else if (itemId == R.id.action_chat) {
                changeFragment(chatFragment);
            } else if (itemId == R.id.action_myinfo) {
                changeFragment(myInfoFragment);
            }
            return true;
        });
    }

    private void changeFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.screen, fragment).commit();
    }
}