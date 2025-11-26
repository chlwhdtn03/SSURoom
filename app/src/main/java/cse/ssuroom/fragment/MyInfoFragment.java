package cse.ssuroom.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.util.ArrayList;
import java.util.List;

import cse.ssuroom.LoginActivity;
import cse.ssuroom.R;
import cse.ssuroom.adapter.PropertyListAdapter;
import cse.ssuroom.database.LeaseTransferRepository;
import cse.ssuroom.database.Property;
import cse.ssuroom.database.ShortTermRepository;
import cse.ssuroom.databinding.FragmentMyInfoBinding;
import cse.ssuroom.user.User;


public class MyInfoFragment extends Fragment {

    private FragmentMyInfoBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;
    private ActivityResultLauncher<Intent> galleryLauncher; // 갤러리에서 이미지를 고르고 결과를 받아오기 위한 런처
    private PropertyListAdapter myListingsAdapter;
    private List<Property> myListings = new ArrayList<>();
    private ShortTermRepository shortTermRepo;
    private LeaseTransferRepository leaseTransferRepo;

    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                                    Uri selectedImageUri = result.getData().getData();
                                    if (selectedImageUri != null) {
                                        uploadProfileImage(selectedImageUri); // 갤러리에서 선택한 이미지를 Firebase Storage에 업로드
                                    }
                                }

                });
    }

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
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        shortTermRepo = new ShortTermRepository();
        leaseTransferRepo = new LeaseTransferRepository();

        myListingsAdapter = new PropertyListAdapter(getContext(), myListings, R.layout.item_favorite_list);
        binding.recyclerViewMyListings.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewMyListings.setAdapter(myListingsAdapter);

        updateUserInfo(); // 앞으로 필요한 유저 데이터를 가져올 함수

        binding.profileImage.setOnClickListener(v -> showImageChangeDialog()); // 이미지 변경을 위한 dialog 호출

        binding.btnLogout.setOnClickListener(v -> logout()); // 로그아웃 실행

    }
    // 네비게이션 selected에서 사용하기 위해 public으로 전환함
    public void updateUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if(!isAdded()){
                            return;
                        }
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                binding.profileName.setText(user.getName());
                                binding.profileEmail.setText(user.getEmail());
                                
                                loadMyListings(user);
                                
                                // 여기서부턴 프로필 이미지 가져오기
                                String imageUrl = user.getProfileImageUrl();

                                Glide.with(this)
                                        .load(imageUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_myinfo)
                                        .into(binding.profileImage);
                            }
                        }
                        else{
                            Toast.makeText(getContext(), "유저 정보를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "정보 로딩 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    });

        }
    }
    private void loadMyListings(User user) {
        if (user.getUploadedProperties() != null && !user.getUploadedProperties().isEmpty()) {
            fetchPropertiesByIds(user.getUploadedProperties());
        } else {
            // 올린 매물이 없는 경우 빈 목록
            updateMyListingsAdapter(new ArrayList<>());
        }
    }
    // 매물 가져오기
    private void fetchPropertiesByIds(List<String> propertyIds) {
        List<Property> combinedList = new ArrayList<>();
        final int[] tasksCompleted = {0};
        int totalTasks = 2; // 매물 타입 2개

        // 단기 임대 매물 가져오기
        shortTermRepo.findAllByIds(propertyIds, shortTerms -> {
            synchronized (combinedList) {
                combinedList.addAll(shortTerms);
            }
            tasksCompleted[0]++;
            if (tasksCompleted[0] == totalTasks) {
                updateMyListingsAdapter(combinedList);
            }
        });

        // 계약 양도 매물 가져오기
        leaseTransferRepo.findAllByIds(propertyIds, leaseTransfers -> {
            synchronized (combinedList) {
                combinedList.addAll(leaseTransfers);
            }
            tasksCompleted[0]++;
            if (tasksCompleted[0] == totalTasks) {
                updateMyListingsAdapter(combinedList);
            }
        });
    }
    private void updateMyListingsAdapter(List<Property> properties) {
        if (!isAdded()) return;

        myListings.clear();
        myListings.addAll(properties);
        myListingsAdapter.notifyDataSetChanged();

        // 목록이 비어 있는지에 따라 RecyclerView와 "매물 없음" 텍스트의 노출 여부 결정
        if (properties.isEmpty()) {
            binding.textViewEmptyListings.setVisibility(View.VISIBLE);
            binding.recyclerViewMyListings.setVisibility(View.GONE);
        } else {
            binding.textViewEmptyListings.setVisibility(View.GONE);
            binding.recyclerViewMyListings.setVisibility(View.VISIBLE);
        }
    }

    // 이미지 변경 다이얼로그를 보여주는 메서드
    private void showImageChangeDialog() {
        final CharSequence[] options = {"갤러리에서 선택", "기본 이미지로 변경", "취소"};
        new AlertDialog.Builder(requireContext())
                .setTitle("프로필 사진 변경")
                .setItems(options, (dialog, item) -> {
                    String selected = options[item].toString();
                    if (selected.equals("갤러리에서 선택")) {
                        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(galleryIntent);
                    } else if (selected.equals("기본 이미지로 변경")) {
                        // DB의 ProfileImageUrl을 빈 문자열로 업데이트
                        updateProfileImageUrl("");
                    } else if (selected.equals("취소")) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    // 선택된 이미지를 DB에 업로드
    private void uploadProfileImage(Uri imageUri) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;


        StorageReference profileImageRef = storageRef.child("profile_images/" + currentUser.getUid() + ".jpg");

        // 파일 업로드
        profileImageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // 업로드 성공 시, 다운로드 URL을 가져옵니다.
                    profileImageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                        // 가져온 URL을 Firestore에 업데이트
                        updateProfileImageUrl(downloadUri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    // Firestore의 사용자 정보에 이미지 URL 업데이트
    private void updateProfileImageUrl(String imageUrl) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .update("profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    if(!isAdded()){
                        return;
                    }
                    Toast.makeText(getContext(), "프로필 사진이 변경되었습니다.", Toast.LENGTH_SHORT).show();
                    Glide.with(this)
                            .load(imageUrl.isEmpty() ? R.drawable.ic_myinfo : imageUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_myinfo)
                            .error(R.drawable.ic_myinfo)
                            .into(binding.profileImage);
                    if (imageUrl.isEmpty()) {
                        binding.profileImage.setImageResource(R.drawable.ic_myinfo);
                    } else {
                        Glide.with(MyInfoFragment.this)
                                .load(imageUrl)
                                .circleCrop()
                                .into(binding.profileImage);
                    }
                })
                .addOnFailureListener(e -> {
                    if(!isAdded()){
                        return;
                    }
                    Toast.makeText(getContext(), "정보 업데이트에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void logout() {
            // Firebase에서 로그아웃 처리
            mAuth.signOut();
            Toast.makeText(getContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();

            // getActivity()가 null이 아닌지 확인하여 NullPointerException 방지
            if (getActivity() == null) return;

            Intent intent = new Intent(getActivity(), LoginActivity.class);

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish(); // 현재 액티비티(MainActivity)를 종료
        }
}



