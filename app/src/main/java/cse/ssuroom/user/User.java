package cse.ssuroom.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// FireStore에 저장할 User Class (프로필 이미지는 생성자로 바로 불가능함)
public class User {
    private String uid;
    private String name;
    private String email;
    private String profileImageUrl; // 프로필 이미지 URL을 저장할 필드
    private List<String> favorites;
    private List<String> uploadedProperties;
    private List<Map<String, Object>> notificationFilters; // 알림 필터 설정을 저장할 리스트


    public User() {
        this.favorites = new ArrayList<>();
        this.uploadedProperties = new ArrayList<>();
        this.notificationFilters = new ArrayList<>(); // 초기화
    }

    // 데이터를 저장할 때 사용할 생성자
    public User(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.profileImageUrl = ""; // 처음에는 빈 값으로 초기화
        this.favorites = new ArrayList<>();
        this.uploadedProperties = new ArrayList<>();
        this.notificationFilters = new ArrayList<>(); // 초기화
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

    public List<String> getUploadedProperties() {
        return uploadedProperties;
    }

    public void setUploadedProperties(List<String> uploadedProperties) {
        this.uploadedProperties = uploadedProperties;
    }

    public List<Map<String, Object>> getNotificationFilters() {
        return notificationFilters;
    }

    public void setNotificationFilters(List<Map<String, Object>> notificationFilters) {
        this.notificationFilters = notificationFilters;
    }
}
