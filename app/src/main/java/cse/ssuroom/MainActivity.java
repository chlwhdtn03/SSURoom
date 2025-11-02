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
import cse.ssuroom.fragment.FavorFragment;
import cse.ssuroom.fragment.MapFragment;
import cse.ssuroom.fragment.MyInfoFragment;

public class MainActivity extends AppCompatActivity {

    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private final MapFragment mapFragment = new MapFragment();
    private final FavorFragment favorFragment = new FavorFragment();
    private final MyInfoFragment myInfoFragment = new MyInfoFragment();
    private Fragment activeFragment = mapFragment;

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
        fragmentManager.beginTransaction().add(R.id.screen, mapFragment, "map").commit();


        binding.bottomMenu.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_home) {
                fragmentManager.beginTransaction().hide(activeFragment).show(mapFragment).commit();
                activeFragment = mapFragment;
            } else if (itemId == R.id.action_favor) {
                fragmentManager.beginTransaction().hide(activeFragment).show(favorFragment).commit();
                activeFragment = favorFragment;
            } else if (itemId == R.id.action_myinfo) {
                fragmentManager.beginTransaction().hide(activeFragment).show(myInfoFragment).commit();
                activeFragment = myInfoFragment;
            } else {
                return false;
            }
            return true;
        });
    }
}
