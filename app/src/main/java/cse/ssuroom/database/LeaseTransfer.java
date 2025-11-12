package cse.ssuroom.database;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class LeaseTransfer {
    public LeaseTransfer(){};

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

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getRoomType() {
        return roomType;
    }
    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }
    public String getHostId() {
        return hostId;
    }
    public void setHostId(String hostId) {
        this.hostId = hostId;
    }
    public double getArea() {
        return area;
    }
    public void setArea(double area) {
        this.area = area;
    }
    public int getFloor() {
        return floor;
    }
    public void setFloor(int floor) {
        this.floor = floor;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    public HashMap<String, Object> getPricing() {
        return pricing;
    }
    public void setPricing(HashMap<String, Object> pricing) {
        this.pricing = pricing;
    }
    public HashMap<String, Object> getDetails() {
        return details;
    }
    public void setDetails(HashMap<String, Object> details) {
        this.details = details;
    }
    public HashMap<String, Object> getAmenities() {
        return amenities;
    }
    public void setAmenities(HashMap<String, Object> amenities) {
        this.amenities = amenities;
    }
    public HashMap<String, Object> getAvailability() {
        return availability;
    }
    public void setAvailability(HashMap<String, Object> availability) {
        this.availability = availability;
    }
    public HashMap<String, Object> getScores() {
        return scores;
    }
    public void setScores(HashMap<String, Object> scores) {
        this.scores = scores;
    }
    public HashMap<String, Object> getLocation() {
        return location;
    }
    public void setLocation(HashMap<String, Object> location) {
        this.location = location;
    }
    public List<String> getPhotos() {
        return photos;
    }
    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public static class Builder {
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

        public Builder setTitle(String title) {
            this.title = title;
            return this; }
        public Builder setDescription(String description) {
            this.description = description;
            return this; }
        public Builder setRoomType(String roomType) {
            this.roomType = roomType;
            return this; }
        public Builder setHostId(String hostId) {
            this.hostId = hostId;
            return this; }
        public Builder setFloor(int floor) {
            this.floor = floor;
            return this; }
        public Builder setArea(double area) {
            this.area = area;
            return this; }
        public Builder setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
            return this; }

        public Builder setDetails(HashMap<String,Object> details) {
            this.details = details;
            return this; }
        public Builder setPricing(HashMap<String,Object> pricing) {
            this.pricing = pricing;
            return this; }
        public Builder setAmenities(HashMap<String,Object> amenities) {
            this.amenities = amenities;
            return this; }
        public Builder setAvailability(HashMap<String,Object> availability) {
            this.availability = availability;
            return this; }
        public Builder setScores(HashMap<String,Object> scores) {
            this.scores = scores;
            return this; }
        public Builder setLocation(HashMap<String,Object> location) {
            this.location = location;
            return this; }

        public Builder setPhotos(List<String> photos) {
            this.photos = photos;
            return this; }

        public LeaseTransfer build() {
            LeaseTransfer lease = new LeaseTransfer();
            lease.title = this.title;
            lease.description = this.description;
            lease.roomType = this.roomType;
            lease.hostId = this.hostId;
            lease.floor = this.floor;
            lease.area = this.area;
            lease.createdAt = this.createdAt;

            lease.details = this.details;
            lease.pricing = this.pricing;
            lease.amenities = this.amenities;
            lease.availability = this.availability;
            lease.scores = this.scores;
            lease.location = this.location;

            lease.photos = this.photos;

            return lease;
        }
    }








}
