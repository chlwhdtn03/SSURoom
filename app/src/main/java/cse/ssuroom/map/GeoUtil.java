package cse.ssuroom.map;

import android.location.Location;

public class GeoUtil {

    private static final double EARTH_RADIUS = 6371000; // in meters

    /**
     * Converts LatLng to a 3D point in Earth-Centered Earth-Fixed (ECEF) coordinates.
     */
    public static double[] latLngToECEF(double latitude, double longitude, double altitude) {
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);
        double cosLat = Math.cos(latRad);
        double sinLat = Math.sin(latRad);
        double cosLon = Math.cos(lonRad);
        double sinLon = Math.sin(lonRad);

        double x = (EARTH_RADIUS + altitude) * cosLat * cosLon;
        double y = (EARTH_RADIUS + altitude) * cosLat * sinLon;
        double z = (EARTH_RADIUS + altitude) * sinLat;

        return new double[]{x, y, z};
    }

    /**
     * Converts ECEF coordinates to East-North-Up (ENU) coordinates relative to a reference point.
     */
    public static float[] ecefToENU(double currentLat, double currentLon, double[] ecef, double[] refEcef) {
        double latRad = Math.toRadians(currentLat);
        double lonRad = Math.toRadians(currentLon);
        double cosLat = Math.cos(latRad);
        double sinLat = Math.sin(latRad);
        double cosLon = Math.cos(lonRad);
        double sinLon = Math.sin(lonRad);

        double dx = ecef[0] - refEcef[0];
        double dy = ecef[1] - refEcef[1];
        double dz = ecef[2] - refEcef[2];

        float east = (float) (-sinLon * dx + cosLon * dy);
        float north = (float) (-sinLat * cosLon * dx - sinLat * sinLon * dy + cosLat * dz);
        float up = (float) (cosLat * cosLon * dx + cosLat * sinLon * dy + sinLat * dz);

        return new float[]{east, north, up};
    }

    public static float[] calculateTranslationInAR(Location userLocation, double targetLatitude, double targetLongitude) {
        double[] userEcef = latLngToECEF(userLocation.getLatitude(), userLocation.getLongitude(), userLocation.getAltitude());
        double[] targetEcef = latLngToECEF(targetLatitude, targetLongitude, userLocation.getAltitude()); // Assume same altitude for simplicity

        return ecefToENU(userLocation.getLatitude(), userLocation.getLongitude(), targetEcef, userEcef);
    }
}
