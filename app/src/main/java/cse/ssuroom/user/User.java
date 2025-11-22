package cse.ssuroom.user;

import java.util.ArrayList;
import java.util.List;

// FireStore에 저장할 User Class (프로필 이미지는 생성자로 바로 불가능함)
public class User {
    private String uid;
    private String name;
    private String email;
    private String profileImageUrl; // 프로필 이미지 URL을 저장할 필드
    private List<String> favorites;

    public User() {
        this.favorites = new ArrayList<>();
    }

    // 데이터를 저장할 때 사용할 생성자
    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.profileImageUrl = ""; // 처음에는 빈 값으로 초기화
        this.favorites = new ArrayList<>();
    }

    // Getter와 Setter
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public List<String> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<String> favorites) {
        this.favorites = favorites;
    }
}
