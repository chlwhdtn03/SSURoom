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
import android.text.Editable;
import android.text.TextWatcher;
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
import com.google.firebase.firestore.FirebaseFirestore;

import cse.ssuroom.MainActivity;
import cse.ssuroom.R;
import cse.ssuroom.databinding.FragmentRegisterBinding;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth mAuth;
    private boolean isEmailChecked = false;
    private String checkedEmail = "";



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();

        gotoLoginLink(); // 파란색 글씨로 이미 회원가입 했으면 바로 로그인 화면으로 이동

        binding.btnCheckDuplicate.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(getContext(), "이메일을 입력해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            checkEmailDuplicate(email);
        });

        // 이메일 입력창에 텍스트 변경 감지하지 기능
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 텍스트 변경 전
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 텍스트가 변경될 때마다 isEmailChecked 상태를 초기화하고 버튼 상태를 되돌림
                isEmailChecked = false;
                binding.btnCheckDuplicate.setText(getString(R.string.check));
                binding.btnCheckDuplicate.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue));
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 텍스트 변경 후
            }
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


    private void checkEmailDuplicate(String email) {
        binding.btnCheckDuplicate.setEnabled(false);

        mAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isNewUser = task.getResult().getSignInMethods().isEmpty();
                        if (isNewUser) {
                            isEmailChecked = true;
                            checkedEmail = email;
                            Toast.makeText(getContext(), "사용 가능한 이메일입니다.", Toast.LENGTH_SHORT).show();
                            binding.btnCheckDuplicate.setText(getString(R.string.email_available));
                            binding.btnCheckDuplicate.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue));
                        } else {
                            isEmailChecked = false;
                            Toast.makeText(getContext(), "이미 사용 중인 이메일입니다.", Toast.LENGTH_SHORT).show();
                            binding.btnCheckDuplicate.setText(getString(R.string.email_unavailable));
                            binding.btnCheckDuplicate.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red));
                        }
                    } else {
                        isEmailChecked = false;
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "오류 발생";
                        Toast.makeText(getContext(), "이메일 확인 중 오류 발생: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                    binding.btnCheckDuplicate.setEnabled(true);
                });
    }


    private void createAccount(){
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();
        String confirmPassword = binding.etConfirmPassword.getText().toString();

        // 회원가입 전, 중복 확인을 통과했는지 검사
        if (!isEmailChecked) {
            Toast.makeText(getContext(), "이메일 중복 확인을 먼저 진행해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 중복 확인 후 이메일을 수정했는지 검사
        if (!checkedEmail.equals(email)) {
            Toast.makeText(getContext(), "이메일이 변경되었습니다. 다시 중복 확인을 해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 조건에 부합하는 지 검사하는 로직
        if(name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
            Toast.makeText(getContext(), "모든 필드를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        //비밀번호 조건
        if(password.length() < 8) {
            Toast.makeText(getContext(), "비밀번호는 8자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        //비밀번호 확인 조건
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