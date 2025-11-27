package cse.ssuroom.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import cse.ssuroom.MainActivity;
import cse.ssuroom.R;
import cse.ssuroom.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    private FragmentLoginBinding binding;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        gotoRegisterLink(); // 파란색 글씨로 회원가입 안했으면 바로 회원가입 할 수 있게 연결 메서드

        binding.btnLogin.setOnClickListener(v -> {
            loginUser();
        });
    }


    private void gotoRegisterLink() {
        String text = getString(R.string.register_prompt);
        String targetWord = getString(R.string.register);
        SpannableString spannableString = new SpannableString(text);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, new RegisterFragment());
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                // getContext()가 null일 수 있으므로 requireContext() 사용 권장
                ds.setColor(ContextCompat.getColor(requireContext(), R.color.blue));
                ds.setUnderlineText(false);
            }
        };

        int startIndex = text.indexOf(targetWord);
        if (startIndex != -1) {
            int endIndex = startIndex + targetWord.length();
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        binding.tvRegister.setText(spannableString);
        binding.tvRegister.setMovementMethod(LinkMovementMethod.getInstance());
    }


    private void loginUser(){
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if(email.isEmpty() || password.isEmpty()){
            Toast.makeText(getContext(), "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }


        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Log.d("LoginFragment", "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            updateFCMToken(user.getUid());
                        } else {
                            Toast.makeText(getContext(), "로그인 성공", Toast.LENGTH_SHORT).show();
                            gotoMainActivity();
                        }
                    }
                    else{
                        Log.w("LoginFragment", "signInWithEmail:failure", task.getException());
                        Toast.makeText(getContext(), "이메일 또는 비밀번호를 확인해주세요", Toast.LENGTH_SHORT).show();
                    }

                });
    }

    private void updateFCMToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("LoginFragment", "Fetching FCM registration token failed", task.getException());
                        // 토큰 가져오기 실패 시, 일단 그냥 메인으로 이동
                        Toast.makeText(getContext(), "로그인 성공 (토큰 갱신 실패)", Toast.LENGTH_SHORT).show();
                        gotoMainActivity();
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    // Update token in Firestore
                    FirebaseFirestore.getInstance().collection("users").document(userId)
                            .update("fcmToken", token)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("LoginFragment", "FCM token updated successfully");
                                Toast.makeText(getContext(), "로그인 성공", Toast.LENGTH_SHORT).show();
                                gotoMainActivity();
                            })
                            .addOnFailureListener(e -> {
                                Log.w("LoginFragment", "Error updating FCM token", e);
                                Toast.makeText(getContext(), "로그인 성공 (토큰 갱신 실패)", Toast.LENGTH_SHORT).show();
                                gotoMainActivity();
                            });
                });
    }

    private void gotoMainActivity(){
        // Main 액비를 실행하는데 clear task 그 전 스택은 모두 지우고, New task 마치 앱의 처음 화면인 것 처럼 실행
        startActivity(new Intent(getActivity(), MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        // 현재 액비인 LoginActivity를 종료
        if(getActivity() != null)
            getActivity().finish();
    }


    // 메모리 누수 방지 코드?!?!?!?
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}