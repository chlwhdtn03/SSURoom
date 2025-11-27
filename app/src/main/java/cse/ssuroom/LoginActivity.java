package cse.ssuroom;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

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
            // 사용자가 이미 로그인되어 있으면 토큰을 업데이트하고 MainActivity로 이동
            updateFCMTokenAndGoToMain(currentUser);
        }
        // 로그인되어 있지 않으면 LoginFragment 그대로
    }

    private void updateFCMTokenAndGoToMain(FirebaseUser user) {
        String userId = user.getUid();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("LoginActivity", "Fetching FCM registration token failed", task.getException());
                        // 토큰 가져오기 실패 시, 일단 그냥 메인으로 이동
                        goToMainActivity();
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    // Update token in Firestore
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                            .update("fcmToken", token)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("LoginActivity", "FCM token updated successfully on auto-login.");
                                goToMainActivity();
                            })
                            .addOnFailureListener(e -> {
                                Log.w("LoginActivity", "Error updating FCM token on auto-login", e);
                                // 토큰 업데이트 실패 시, 일단 그냥 메인으로 이동
                                goToMainActivity();
                            });
                });
    }


    //MainActivity로 이동하고 현재 액티비티를 종료하는 메서드
    private void goToMainActivity() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
