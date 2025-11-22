package cse.ssuroom;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.core.Frame;
import com.google.ar.core.Camera;
import android.view.MotionEvent;
import com.google.ar.core.TrackingState;
import com.google.ar.core.HitResult;
import com.google.ar.core.Trackable;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import cse.ssuroom.adapter.ArPropertyListAdapter;
import cse.ssuroom.database.LeaseTransfer;
import cse.ssuroom.database.LeaseTransferRepository;
import cse.ssuroom.database.ShortTerm;
import cse.ssuroom.database.ShortTermRepository;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class ArActivity extends AppCompatActivity {

    private static final String TAG = "ArActivity";

    private GLSurfaceView glSurfaceView;
    private ArRenderer renderer;
    private Session session;

    private boolean userRequestedInstall = true;
    private boolean locationPermissionGranted = false;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private DisplayRotationHelper displayRotationHelper;
    private TapHelper tapHelper;

    private final ArrayList<Anchor> tappedAnchors = new ArrayList<>();

    private LeaseTransferRepository leaseRepo;
    private ShortTermRepository shortRepo;
    private List<LeaseTransfer> leaseList = new ArrayList<>();
    private List<ShortTerm> shortList = new ArrayList<>();
    private List<PropertyForAR> arProperties = new ArrayList<>();
    private volatile boolean leaseDataLoaded = false;
    private volatile boolean shortDataLoaded = false;
    private RecyclerView arRecyclerView;
    private ArPropertyListAdapter arAdapter;
    private List<Object> visibleProperties = new ArrayList<>();

    public static class PropertyForAR {
        public final String id;
        public final String type; // "lease" or "short"
        public final double latitude;
        public final double longitude;
        public Anchor anchor;
        public boolean anchorCreationRequested;
        public float distance;

        public PropertyForAR(String id, String type, double latitude, double longitude) {
            this.id = id;
            this.type = type;
            this.latitude = latitude;
            this.longitude = longitude;
            this.anchor = null;
            this.anchorCreationRequested = false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PropertyForAR that = (PropertyForAR) o;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }


    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean cameraPermission = permissions.getOrDefault(Manifest.permission.CAMERA, false);
                locationPermissionGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                        permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (!cameraPermission) {
                    Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                    finish();
                }
                if (!locationPermissionGranted) {
                    Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_ar);

        glSurfaceView = findViewById(R.id.ar_gl_surface_view);
        
        displayRotationHelper = new DisplayRotationHelper(this);
        tapHelper = new TapHelper(this);
        glSurfaceView.setOnTouchListener(tapHelper);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location;
                        if(renderer != null){
                            renderer.setCurrentLocation(location);
                        }
                        processArProperties();
                    }
                }
            }
        };

        // Setup RecyclerView
        arRecyclerView = findViewById(R.id.ar_recycler_view);
        arAdapter = new ArPropertyListAdapter(this, visibleProperties);
        arRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        arRecyclerView.setAdapter(arAdapter);

        renderer = new ArRenderer(this);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        leaseRepo = new LeaseTransferRepository();
        shortRepo = new ShortTermRepository();

        leaseRepo.findAll(list -> {
            leaseList.clear();
            leaseList.addAll(list);
            leaseDataLoaded = true;
            Log.d(TAG, "Lease data loaded. Items: " + list.size());
            processArProperties();
        });
        shortRepo.findAll(list -> {
            shortList.clear();
            shortList.addAll(list);
            shortDataLoaded = true;
            Log.d(TAG, "Short-term data loaded. Items: " + list.size());
            processArProperties();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
        if (session == null) {
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                    case INSTALLED:
                        session = new Session(this);
                        break;
                    case INSTALL_REQUESTED:
                        userRequestedInstall = false;
                        return;
                }
            } catch (UnavailableUserDeclinedInstallationException e) {
                Toast.makeText(this, "ARCore 설치가 필요합니다.", Toast.LENGTH_LONG).show();
                finish();
                return;
            } catch (UnavailableException e) {
                Toast.makeText(this, "이 기기에서는 ARCore를 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        try {
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            session.configure(config);
            session.resume();
        } catch (CameraNotAvailableException e) {
            Toast.makeText(this, "카메라를 사용할 수 없습니다. 앱을 재시작해 주세요.", Toast.LENGTH_LONG).show();
            session = null;
            finish();
            return;
        }

        glSurfaceView.onResume();
        renderer.setSession(session);
        displayRotationHelper.onResume();
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (session != null) {
            session.pause();
        }
        glSurfaceView.onPause();
        displayRotationHelper.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (session != null) {
            session.close();
            session = null;
        }
    }

    private void checkPermissions() {
        boolean cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        locationPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!cameraPermission || !locationPermissionGranted) {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationUpdates() {
        if (locationPermissionGranted) {
            try {
                LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                        .setWaitForAccurateLocation(false)
                        .setMinUpdateIntervalMillis(500)
                        .setMaxUpdateDelayMillis(1000)
                        .build();
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
            } catch (SecurityException e) {
                Log.e(TAG, "Location permission not granted", e);
            }
        }
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    private synchronized void processArProperties() {
        if (currentLocation == null || !leaseDataLoaded || !shortDataLoaded) {
            Log.d(TAG, "processArProperties() called but prerequisites not met. Location: " + (currentLocation != null) + ", LeaseData: " + leaseDataLoaded + ", ShortData: " + shortDataLoaded);
            return;
        }
        Log.d(TAG, "processArProperties() - All prerequisites met. Processing...");

        double currentLatitude = currentLocation.getLatitude();
        double currentLongitude = currentLocation.getLongitude();

        // Handle lease properties
        for (LeaseTransfer lease : leaseList) {
            try {
                Map<String, Object> loc = lease.getLocation();
                if (loc == null) continue;

                Object latObj = loc.get("latitude");
                Object lonObj = loc.get("longitude");

                if (!(latObj instanceof Number) || !(lonObj instanceof Number)) {
                    Log.w(TAG, "Invalid location data for lease property: " + lease.getPropertyId());
                    continue;
                }

                double lat = ((Number) latObj).doubleValue();
                double lon = ((Number) lonObj).doubleValue();

                float[] distance = new float[1];
                Location.distanceBetween(currentLatitude, currentLongitude, lat, lon, distance);
                Log.d(TAG, "Lease property " + lease.getPropertyId() + " at (" + lat + ", " + lon + ") is " + distance[0] + "m away.");

                PropertyForAR arProperty = new PropertyForAR(lease.getPropertyId(), "lease", lat, lon);
                arProperty.distance = distance[0];
                int index = arProperties.indexOf(arProperty);

                if (distance[0] < 500) { // 500미터 이내
                    if (index == -1) { // 새로 추가된 매물
                        Log.d(TAG, "Adding lease property to AR list: " + lease.getPropertyId());
                        arProperty.anchorCreationRequested = true;
                        arProperties.add(arProperty);
                    } else {
                        arProperties.get(index).distance = distance[0];
                    }
                } else { // 500미터 밖
                    if (index != -1) { // 기존에 있던 매물이 멀어짐
                        Log.d(TAG, "Removing lease property from AR list: " + lease.getPropertyId());
                        PropertyForAR existingProp = arProperties.get(index);
                        if (existingProp.anchor != null) {
                            existingProp.anchor.detach();
                        }
                        arProperties.remove(index);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to process lease property", e);
            }
        }

        // Handle short-term properties
        for (ShortTerm shortTerm : shortList) {
            try {
                Map<String, Object> loc = shortTerm.getLocation();
                if (loc == null) continue;

                Object latObj = loc.get("latitude");
                Object lonObj = loc.get("longitude");

                if (!(latObj instanceof Number) || !(lonObj instanceof Number)) {
                    Log.w(TAG, "Invalid location data for short-term property: " + shortTerm.getPropertyId());
                    continue;
                }

                double lat = ((Number) latObj).doubleValue();
                double lon = ((Number) lonObj).doubleValue();

                float[] distance = new float[1];
                Location.distanceBetween(currentLatitude, currentLongitude, lat, lon, distance);
                Log.d(TAG, "Short-term property " + shortTerm.getPropertyId() + " at (" + lat + ", " + lon + ") is " + distance[0] + "m away.");

                PropertyForAR arProperty = new PropertyForAR(shortTerm.getPropertyId(), "short", lat, lon);
                arProperty.distance = distance[0];
                int index = arProperties.indexOf(arProperty);

                if (distance[0] < 500) { // 500미터 이내
                    if (index == -1) {
                        Log.d(TAG, "Adding short-term property to AR list: " + shortTerm.getPropertyId());
                        arProperty.anchorCreationRequested = true;
                        arProperties.add(arProperty);
                    } else {
                        arProperties.get(index).distance = distance[0];
                    }
                } else {
                    if (index != -1) {
                        Log.d(TAG, "Removing short-term property from AR list: " + shortTerm.getPropertyId());
                        PropertyForAR existingProp = arProperties.get(index);
                        if (existingProp.anchor != null) {
                            existingProp.anchor.detach();
                        }
                        arProperties.remove(index);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to process short-term property", e);
            }
        }
    }

    public List<PropertyForAR> getArProperties() {
        return arProperties;
    }

    public List<LeaseTransfer> getLeaseList() {
        return leaseList;
    }

    public List<ShortTerm> getShortList() {
        return shortList;
    }

    public Object getPropertyById(String id, String type) {
        if ("lease".equals(type)) {
            for (LeaseTransfer lease : leaseList) {
                if (lease.getPropertyId().equals(id)) {
                    return lease;
                }
            }
        } else if ("short".equals(type)) {
            for (ShortTerm shortTerm : shortList) {
                if (shortTerm.getPropertyId().equals(id)) {
                    return shortTerm;
                }
            }
        }
        return null;
    }

    public void updateVisibleProperties(List<Object> newVisibleProperties) {
        runOnUiThread(() -> {
            arAdapter.updateProperties(newVisibleProperties);
            Log.d(TAG, "UI updated with " + visibleProperties.size() + " visible properties.");
        });
    }

    public TapHelper getTapHelper() {
        return tapHelper;
    }

    public DisplayRotationHelper getDisplayRotationHelper() {
        return displayRotationHelper;
    }

    public Session getSession() {
        return session;
    }

    public void handleTap(Frame frame, Camera camera) {
        MotionEvent tap = tapHelper.poll();
        if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
            Log.d(TAG, "Tap detected at: (" + tap.getX() + ", " + tap.getY() + ")");
            List<HitResult> hitResultList = frame.hitTest(tap);
            Log.d(TAG, "Hit test results: " + hitResultList.size());
            for (HitResult hit : hitResultList) {
                // Check if any plane was hit, and create an anchor.
                Trackable trackable = hit.getTrackable();
                if ((trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose()))
                        || (trackable instanceof Point && ((Point) trackable).getOrientationMode() == OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                    // Cap the number of objects created.
                    if (tappedAnchors.size() >= 20) {
                        tappedAnchors.get(0).detach();
                        tappedAnchors.remove(0);
                    }
                    tappedAnchors.add(hit.createAnchor());
                    Log.d(TAG, "Anchor created from tap at: " + hit.getHitPose());
                    break;
                }
            }
        }
    }

    public ArrayList<Anchor> getTappedAnchors() {
        return tappedAnchors;
    }

}