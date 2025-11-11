package cse.ssuroom.map;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;

import java.util.ArrayList;

public class MarkerManager {
    public static ArrayList<Marker> markers = new ArrayList<>();

    public static void addMaker(NaverMap map, com.naver.maps.geometry.LatLng loc, String title) {
        Marker marker = new Marker();
        marker.setPosition(loc);
        marker.setMap(map);
        markers.add(marker);
    }

    public static void removeAllMarker() {
        for(Marker marker : markers) {
            marker.setMap(null);
        }
        markers.clear();
    }

    public static void removeMarker(int index) {
        markers.get(index).setMap(null);
        markers.remove(index);
    }

}
