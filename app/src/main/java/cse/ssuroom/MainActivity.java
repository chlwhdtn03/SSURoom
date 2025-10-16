package cse.ssuroom;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import cse.ssuroom.databinding.ActivityMainBinding;
import cse.ssuroom.fragment.FavorFragment;
import cse.ssuroom.fragment.MapFragment;
import cse.ssuroom.fragment.MyInfoFragment;

public class MainActivity extends AppCompatActivity {

    private final MapFragment mapFragment = new MapFragment();
    private final FavorFragment favorFragment = new FavorFragment();
    private final MyInfoFragment myInfoFragment = new MyInfoFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ActivityMainBinding binding = ActivityMainBinding.bind(getLayoutInflater().inflate(R.layout.activity_main, null));
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        changeFragment(mapFragment);

        binding.bottomMenu.setOnItemSelectedListener((item) -> {
            if (item.getItemId() == R.id.action_home) {
                changeFragment(mapFragment);
            }
            if (item.getItemId() == R.id.action_favor) {
                changeFragment(favorFragment);
            }
            if (item.getItemId() == R.id.action_myinfo) {
                changeFragment(myInfoFragment);
            }

            return true;
        });
    }

    private void changeFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.screen, fragment).commit();
    }
}