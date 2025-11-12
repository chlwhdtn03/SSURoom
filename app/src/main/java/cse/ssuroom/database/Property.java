package cse.ssuroom.database;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * 모든 매물(단기, 양도)의 공통 정보를 담는 부모 클래스입니다.
 */
public abstract class Property {

    private String title;
    private String description;
    private String roomType;
    private String hostId;
    private int floor;
    private double area;
    private Date createdAt;

    private HashMap<String, Object> details;
    private HashMap<String, Object> pricing;
    private HashMap<String, Object> amenities;
    private HashMap<String, Object> availability;
    private HashMap<String, Object> scores;
    private HashMap<String, Object> location;
    private List<String> photos;

    public Property() {}


    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public String getHostId() { return hostId; }
    public void setHostId(String hostId) { this.hostId = hostId; }
    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }
    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public HashMap<String, Object> getDetails() { return details; }
    public void setDetails(HashMap<String, Object> details) { this.details = details; }
    public HashMap<String, Object> getPricing() { return pricing; }
    public void setPricing(HashMap<String, Object> pricing) { this.pricing = pricing; }
    public HashMap<String, Object> getAmenities() { return amenities; }
    public void setAmenities(HashMap<String, Object> amenities) { this.amenities = amenities; }
    public HashMap<String, Object> getAvailability() { return availability; }
    public void setAvailability(HashMap<String, Object> availability) { this.availability = availability; }
    public HashMap<String, Object> getScores() { return scores; }
    public void setScores(HashMap<String, Object> scores) { this.scores = scores; }
    public HashMap<String, Object> getLocation() { return location; }
    public void setLocation(HashMap<String, Object> location) { this.location = location; }
    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }

    /**
     * 제네릭을 사용한 공통 빌더 클래스.
     * T: 빌더가 생성할 최종 객체의 타입 (e.g., ShortTerm)
     * B: 빌더 자기 자신의 타입 (e.g., ShortTerm.Builder)
     */
    @SuppressWarnings("unchecked") // "this"를 B로 캐스팅하는 것은 항상 안전합니다.
    public abstract static class Builder<T extends Property, B extends Builder<T, B>> {
        private String title;
        private String description;
        private String roomType;
        private String hostId;
        private int floor;
        private double area;
        private Date createdAt;
        private HashMap<String, Object> details = new HashMap<>();
        private HashMap<String, Object> pricing = new HashMap<>();
        private HashMap<String, Object> amenities = new HashMap<>();
        private HashMap<String, Object> availability = new HashMap<>();
        private HashMap<String, Object> scores = new HashMap<>();
        private HashMap<String, Object> location = new HashMap<>();
        private List<String> photos;

        protected B self() {
            return (B) this;
        }

        public B setTitle(String title) { this.title = title; return self(); }
        public B setDescription(String description) { this.description = description; return self(); }
        public B setRoomType(String roomType) { this.roomType = roomType; return self(); }
        public B setHostId(String hostId) { this.hostId = hostId; return self(); }
        public B setFloor(int floor) { this.floor = floor; return self(); }
        public B setArea(double area) { this.area = area; return self(); }
        public B setCreatedAt(Date createdAt) { this.createdAt = createdAt; return self(); }
        public B setDetails(HashMap<String,Object> details) { this.details = details; return self(); }
        public B setPricing(HashMap<String,Object> pricing) { this.pricing = pricing; return self(); }
        public B setAmenities(HashMap<String,Object> amenities) { this.amenities = amenities; return self(); }
        public B setAvailability(HashMap<String,Object> availability) { this.availability = availability; return self(); }
        public B setScores(HashMap<String,Object> scores) { this.scores = scores; return self(); }
        public B setLocation(HashMap<String,Object> location) { this.location = location; return self(); }
        public B setPhotos(List<String> photos) { this.photos = photos; return self(); }

        // 자식 빌더에서 이 메소드를 구현하여 실제 객체를 생성합니다.
        public abstract T build();

        // 자식 클래스가 부모의 속성을 쉽게 설정할 수 있도록 돕는 메소드입니다.
        protected void build(T property) {
            property.setTitle(this.title);
            property.setDescription(this.description);
            property.setRoomType(this.roomType);
            property.setHostId(this.hostId);
            property.setFloor(this.floor);
            property.setArea(this.area);
            property.setCreatedAt(this.createdAt);
            property.setDetails(this.details);
            property.setPricing(this.pricing);
            property.setAmenities(this.amenities);
            property.setAvailability(this.availability);
            property.setScores(this.scores);
            property.setLocation(this.location);
            property.setPhotos(this.photos);
        }
    }
}
