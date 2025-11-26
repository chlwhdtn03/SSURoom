package cse.ssuroom.map;

import androidx.annotation.NonNull;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.clustering.ClusteringKey;

public class ItemKey implements ClusteringKey {
    private final int id;
    @NonNull
    private final LatLng position;
    private final ItemKey.Type type;

    public ItemKey(int id, @NonNull LatLng position, ItemKey.Type type) {
        this.id = id;
        this.position = position;
        this.type = type;
    }

    public enum Type {
        Lease, Short
    }

    public int getId() {
        return id;
    }

    public Type getType() {
        return type;
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