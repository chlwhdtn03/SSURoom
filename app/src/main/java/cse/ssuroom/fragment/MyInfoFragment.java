package cse.ssuroom.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import cse.ssuroom.LoginActivity;
import cse.ssuroom.R;
import cse.ssuroom.databinding.FragmentMyInfoBinding;


public class MyInfoFragment extends Fragment {

    private FragmentMyInfoBinding binding;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMyInfoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        updateUserInfo(); // 앞으로 필요한 유저 데이터를 가져올 함수
        setupLogoutButton(); // 로그아웃 버튼

    }
    private void updateUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // 여기서 필요한 유저 데이터 가져오기
        }
    }

    private void setupLogoutButton() {
        binding.btnLogout.setOnClickListener(v -> {
            // Firebase에서 로그아웃 처리
            mAuth.signOut();
            Toast.makeText(getContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();

            // getActivity()가 null이 아닌지 확인하여 NullPointerException 방지
            if (getActivity() == null) return;

            Intent intent = new Intent(getActivity(), LoginActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish(); // 현재 액티비티(MainActivity)를 종료
        });
    }


}