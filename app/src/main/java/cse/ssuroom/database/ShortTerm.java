package cse.ssuroom.database;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * 단기 매물 정보를 담는 클래스.
 * Property 클래스를 상속
 */
public class ShortTerm extends Property {

    /**
     * Firestore 용 생성자
     */
    public ShortTerm() {
        super();
    }

    /**
     * 매물을 등록할 때 필요한 모든 정보를 받는 생성자
     */
    public ShortTerm(String title, String description, String hostId, List<String> photos,
                     String roomType, int floor, double area, 
                     Date moveInDate, Date moveOutDate,
                     HashMap<String, Object> pricing, HashMap<String, Object> location, 
                     HashMap<String, Object> amenities, HashMap<String, Object> scores) {
        super(title, description, hostId, photos, roomType, floor, area, moveInDate, moveOutDate, pricing, location, amenities, scores);
    }


}
