package cse.ssuroom.database;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * 양도 매물 정보를 담는 클래스.
 * Property 클래스를 상속받아 공통 속성을 물려받습니다.
 */
public class LeaseTransfer extends Property {

    /**
     * Firestore가 데이터를 객체로 변환할 때 오류가 나지 않도록 비어있는 기본 생성자가 반드시 필요합니다.
     */
    public LeaseTransfer() {
        super();
    }

    /**
     * 매물을 등록할 때 필요한 모든 정보를 받는 생성자입니다.
     * 부모 클래스의 생성자(super)를 호출하여 모든 공통 속성을 초기화합니다.
     */
    public LeaseTransfer(String title, String description, String hostId, List<String> photos,
                         String roomType, int floor, double area, 
                         Date moveInDate, Date moveOutDate,
                         HashMap<String, Object> pricing, HashMap<String, Object> location, 
                         HashMap<String, Object> amenities, HashMap<String, Object> scores) {
        super(title, description, hostId, photos, roomType, floor, area, moveInDate, moveOutDate, pricing, location, amenities, scores);
    }

    // LeaseTransfer에만 필요한 고유한 필드나 메소드가 있다면 여기에 추가할 수 있습니다.
}
