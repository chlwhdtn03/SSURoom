package cse.ssuroom;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import cse.ssuroom.databinding.ActivityMainBinding;
import cse.ssuroom.fragment.ChatFragment;
import cse.ssuroom.fragment.FavorFragment;
import cse.ssuroom.fragment.MapFragment;
import cse.ssuroom.fragment.MyInfoFragment;
import cse.ssuroom.fragment.RoomlistFragment;

public class MainActivity extends AppCompatActivity {

    private final RoomlistFragment roomlistFragment = new RoomlistFragment();
    private final MapFragment mapFragment = new MapFragment();
    private final FavorFragment favorFragment = new FavorFragment();
    private final ChatFragment chatFragment = new ChatFragment();
    private final MyInfoFragment myInfoFragment = new MyInfoFragment();

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set initial fragment
        changeFragment(roomlistFragment);

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