package cse.ssuroom.database;

import com.google.firebase.firestore.Exclude;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Property {


    @Exclude
    private String propertyId;

    // --- 기본 정보 ---
    private String title;
    private String description;
    private String hostId;
    private Date createdAt;
    private List<String> photos;

    // --- 매물 상세 정보 ---
    private String roomType;
    private int floor;
    private double area;

    // --- 날짜 정보 ---
    private Date moveInDate;
    private Date moveOutDate;

    // --- 복합 정보 (가격, 위치, 옵션, 점수) ---
    private HashMap<String, Object> pricing;
    private HashMap<String, Object> location;
    private HashMap<String, Object> amenities;
    private HashMap<String, Object> scores;

    /**
     * Firestore가 데이터를 객체로 변환할 때 오류가 나지 않도록 비어있는 기본 생성자가 반드시 필요합니다.
     */
    public Property() {}

    /**
     * 매물을 등록할 때 필요한 모든 정보를 받는 생성자입니다.
     */
    public Property(String title, String description, String hostId, List<String> photos,
                    String roomType, int floor, double area,
                    Date moveInDate, Date moveOutDate,
                    HashMap<String, Object> pricing, HashMap<String, Object> location,
                    HashMap<String, Object> amenities, HashMap<String, Object> scores) {
        this.title = title;
        this.description = description;
        this.hostId = hostId;
        this.photos = photos;
        this.roomType = roomType;
        this.floor = floor;
        this.area = area;
        this.moveInDate = moveInDate;
        this.moveOutDate = moveOutDate;
        this.pricing = pricing;
        this.location = location;
        this.amenities = amenities;
        this.scores = scores;
        this.createdAt = new Date(); // 객체가 생성되는 시점의 시간으로 자동 설정
    }

    //게터 세터

    @Exclude
    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String id) {
        this.propertyId = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getFloor() {
        return floor;
    }

    public void setFloor(int floor) {
        this.floor = floor;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public Date getMoveInDate() {
        return moveInDate;
    }

    public void setMoveInDate(Date moveInDate) {
        this.moveInDate = moveInDate;
    }

    public Date getMoveOutDate() {
        return moveOutDate;
    }

    public void setMoveOutDate(Date moveOutDate) {
        this.moveOutDate = moveOutDate;
    }

    public HashMap<String, Object> getPricing() {
        return pricing;
    }

    public void setPricing(HashMap<String, Object> pricing) {
        this.pricing = pricing;
    }

    public HashMap<String, Object> getLocation() {
        return location;
    }

    public void setLocation(HashMap<String, Object> location) {
        this.location = location;
    }

    public HashMap<String, Object> getAmenities() {
        return amenities;
    }

    public void setAmenities(HashMap<String, Object> amenities) {
        this.amenities = amenities;
    }

    public HashMap<String, Object> getScores() {
        return scores;
    }

    public void setScores(HashMap<String, Object> scores) {
        this.scores = scores;
    }
}