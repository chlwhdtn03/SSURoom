package cse.ssuroom;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.SharedCamera;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;

import java.util.EnumSet;
import java.util.List;

import cse.ssuroom.databinding.ActivityArBinding;

public class ArActivity extends AppCompatActivity implements SurfaceHolder.Callback, DisplayManager.DisplayListener {

    private static final String TAG = "ArActivity";

    private boolean isUserRequestedInstall = true;
    private Session session;
    private SharedCamera sharedCamera;
    private Handler backgroundHandler;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewCaptureRequestBuilder;

    private ActivityArBinding binding;
    private SurfaceView surfaceView;
    private DisplayManager displayManager;

    private boolean surfaceCreated = false;
    private boolean cameraPermissionGranted = false;
    private boolean resuming = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    cameraPermissionGranted = true;
                } else {
                    Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        surfaceView = binding.surfaceView;
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);

        surfaceView.getHolder().addCallback(this);

        backgroundHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onResume() {
        super.onResume();
        resuming = true;

        displayManager.registerDisplayListener(this, backgroundHandler);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraPermissionGranted = true;
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }

        if (session == null) {
            try {
                if (!cameraPermissionGranted) {
                    throw new UnavailableException();
                }

                switch (ArCoreApk.getInstance().requestInstall(this, isUserRequestedInstall)) {
                    case INSTALLED:
                        session = new Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA));

//                        session.configure(new Config(session).setCloudAnchorMode(Config.CloudAnchorMode.ENABLED));
                        break;
                    case INSTALL_REQUESTED:
                        isUserRequestedInstall = false;
                        return;
                }
            } catch (UnavailableException e) {
                Log.e(TAG, "ARCore is not available:", e);
                Toast.makeText(this, "이 기기에서는 ARCore를 사용할 수 없습니다.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available while resuming session", e);
            session = null;
            return;
        }

        if (surfaceCreated) {
            openCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        resuming = false;
        displayManager.unregisterDisplayListener(this);
        if (session != null) {
            session.pause();
        }
        closeCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (session != null) {
            session.close();
            session = null;
        }
    }

    // --- SurfaceHolder.Callback methods ---

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        surfaceCreated = true;
        if (resuming && cameraPermissionGranted) {
            openCamera();
        }
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        if (session != null) {
            session.setDisplayGeometry(getDisplay().getRotation(), width, height);
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        surfaceCreated = false;
    }

    // --- DisplayListener methods ---

    @Override
    public void onDisplayAdded(int displayId) {}

    @Override
    public void onDisplayRemoved(int displayId) {}

    @Override
    public void onDisplayChanged(int displayId) {
        if (session != null) {
            session.setDisplayGeometry(getDisplay().getRotation(), surfaceView.getWidth(), surfaceView.getHeight());
        }
    }

    // --- Camera related methods ---

    private void openCamera() {
        if (session == null || !cameraPermissionGranted || cameraDevice != null) {
            return;
        }

        sharedCamera = session.getSharedCamera();
        String cameraId = session.getCameraConfig().getCameraId();
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                Log.d(TAG, "Camera device opened.");
                cameraDevice = camera;
                createCameraPreviewSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                Log.w(TAG, "Camera device disconnected.");
                closeCamera();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                Log.e(TAG, "Camera device error: " + error);
                closeCamera();
            }
        };

        try {
            cameraManager.openCamera(cameraId, sharedCamera.createARDeviceStateCallback(cameraStateCallback, backgroundHandler), backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to open camera", e);
        }
    }

    private void createCameraPreviewSession() {
        if (cameraDevice == null || session == null || !surfaceCreated) {
            return;
        }

        try {
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }

            previewCaptureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);

            List<Surface> surfaceList = sharedCamera.getArCoreSurfaces();
            surfaceList.add(surfaceView.getHolder().getSurface());

            for (Surface surface : surfaceList) {
                previewCaptureRequestBuilder.addTarget(surface);
            }

            CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "Camera capture session configured.");
                    captureSession = session;
                    setRepeatingCaptureRequest();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Failed to configure camera capture session.");
                }
            };

            cameraDevice.createCaptureSession(surfaceList, sharedCamera.createARSessionStateCallback(sessionStateCallback, backgroundHandler), backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "CameraAccessException", e);
        }
    }

    private void setRepeatingCaptureRequest() {
        if (captureSession == null) {
            return;
        }
        try {
            captureSession.setRepeatingRequest(previewCaptureRequestBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to set repeating request", e);
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }
}