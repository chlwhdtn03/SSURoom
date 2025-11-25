package cse.ssuroom.map;

import androidx.annotation.NonNull;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.clustering.ClusteringKey;

public class ItemKey implements ClusteringKey {
    private final int id;
    @NonNull
    private final LatLng position;

    public ItemKey(int id, @NonNull LatLng position) {
        this.id = id;
        this.position = position;
    }

    public int getId() {
        return id;
    }

    @Override
    @NonNull
    public LatLng getPosition() {
        return position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ItemKey itemKey = (ItemKey)o;

        return id == itemKey.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}