package cse.ssuroom.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import cse.ssuroom.MainActivity;
import cse.ssuroom.R;
import cse.ssuroom.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth mAuth;
    private EditText etName, etEmail, etPhone, etPassword, etConfirmPassword;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();

        gotoLoginLink(); // 파란색 글씨로 이미 회원가입 했으면 바로 로그인 화면으로 이동

        binding.btnCheckDuplicate.setOnClickListener(v -> {
            // TODO: 이메일 중복 여부 확인하는 로직 필요
        });

        // 전화번호 인증 버튼 클릭 시에, 인증 번호 입력할 수 있는 LinearLayout이 보이게 설정하는 코드
        binding.btnSendVerification.setOnClickListener(v -> {
            // TODO: 전화번호 인증 번호를 전송하는 로직 필요 -> 못할 거 같은데 이메일 인증으로 할까요??
            binding.llVerificationCodeInput.setVisibility(View.VISIBLE); // 인증 번호 입력할 수 있는 LinearLayout 보이게 설정
        });

        binding.btnVerifyCode.setOnClickListener(v -> {
            // TODO: 인증 번호 확인하는 로직 필요
        });

        binding.btnRegister.setOnClickListener(v -> {
            createAccount();
        });

        return binding.getRoot();
    }

    private void gotoLoginLink(){
        String text = getString(R.string.login_prompt);
        String targetWord = getString(R.string.login);
        SpannableString spannableString = new SpannableString(text);

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // 현재 프래그먼트를 스택에서 제거하여 이전 화면(로그인)으로 돌아감
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                if (getContext() != null) {
                    ds.setColor(ContextCompat.getColor(getContext(), R.color.blue));
                }
                ds.setUnderlineText(false);
            }
        };

        int startIndex = text.indexOf(targetWord);
        if (startIndex != -1) {
            int endIndex = startIndex + targetWord.length();
            spannableString.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        binding.tvLogin.setText(spannableString);
        binding.tvLogin.setMovementMethod(LinkMovementMethod.getInstance());
    }




    private void createAccount(){
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        // 조건에 부합하는 지 검사하는 로직
        if(name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
            Toast.makeText(getContext(), "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if(password.length() < 8) {
            Toast.makeText(getContext(), "비밀번호는 8자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "회원가입 성공", Toast.LENGTH_SHORT).show();
                        FirebaseUser user = mAuth.getCurrentUser();
                        // TODO : 여기서 원하는 유저 정보 빼올 수 있음. 아니면 여기서 유저 데이터를 데이터
                        getParentFragmentManager().popBackStack(); //다시 로그인 화면으로 이동.
                    }
                    else {
                        if (task.getException() != null) {
                            Toast.makeText(getContext(), "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(), "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        }
                        // TODO : 오류 원인을 다시 봐야할 듯
                    }
                });

    }
}