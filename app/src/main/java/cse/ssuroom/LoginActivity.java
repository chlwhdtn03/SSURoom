package cse.ssuroom;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import cse.ssuroom.database.LeaseTransfer;
import cse.ssuroom.database.LeaseTransferRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import cse.ssuroom.fragment.LoginFragment;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // 완전 처음실행인 경우 loginFragment 화면 보여주기
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LoginFragment())
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 현재 로그인된 사용자가 있는지 확인
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // 사용자가 이미 로그인되어 있으면 MainActivity로 즉시 이동
            goToMainActivity();
        }
         // Testing
//        new LeaseTransferRepository().getAllLeases(list -> {
//            System.out.println("List size: " + list.size());
//            list.forEach(System.out::println);
//        });


        // 로그인되어 있지 않으면 LoginFragment 그대로
    }


    //MainActivity로 이동하고 현재 액티비티를 종료하는 메서드
    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        // 뒤로가기 버튼을 눌렀을 때 로그인 화면으로 돌아오지 않도록 이전의 모든 액티비티 스택을 지우기
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}