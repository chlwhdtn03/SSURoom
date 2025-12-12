package cse.ssuroom.database;

import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class LeaseTransfer extends Property {


    public LeaseTransfer() {
        super();
    }


    public LeaseTransfer(String title, String description, String hostId, List<String> photos,
                         String roomType, int floor, double area, 
                         Date moveInDate, Date moveOutDate,
                         HashMap<String, Object> pricing, HashMap<String, Object> location, 
                         HashMap<String, Object> amenities, HashMap<String, Object> scores) {
        super(title, description, hostId, photos, roomType, floor, area, moveInDate, moveOutDate, pricing, location, amenities, scores);
    }

}
