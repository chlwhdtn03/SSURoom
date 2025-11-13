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
        mAuth = FirebaseAuth.getInstance();

        gotoRegisterLink(); // 파란색 글씨로 회원가입 안했으면 바로 회원가입 할 수 있게 연결하는 함수

        binding.btnLogin.setOnClickListener(v -> {
            loginUser();
        });

        return binding.getRoot();
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
                        Toast.makeText(getContext(), "로그인 성공", Toast.LENGTH_SHORT).show();
                        // TODO : 여기서 원하는 유저 정보 빼올 수 있음. 아니면 여기서 유저 데이터를 데이터
                        gotoMainActivity();
                    }
                    else{
                        Log.w("LoginFragment", "signInWithEmail:failure", task.getException());
                        Toast.makeText(getContext(), "이메일 또는 비밀번호를 확인해주세요" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }

                });
    }
    private void gotoMainActivity(){
        Intent intent = new Intent(getActivity(), MainActivity.class);
        //MainActivity 이외의 다른 모든 액티비티를 스택에서 제거하는 코드
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        //현재 LoginActivity를 종료
        if(getActivity() != null)
            getActivity().finish();
    }


    // 메모리 누수 방지 코드라는데 모르겠음 ?!?!?!?
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}