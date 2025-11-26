package cse.ssuroom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import androidx.core.content.ContextCompat;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ArRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "ArRenderer";

    // region SHADERS AND VERTEX DATA
    private static final String CAMERA_VERTEX_SHADER =
            "attribute vec4 a_Position; attribute vec2 a_TexCoord; varying vec2 v_TexCoord; void main() { v_TexCoord = a_TexCoord; gl_Position = a_Position; }";
    private static final String CAMERA_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float; varying vec2 v_TexCoord; uniform samplerExternalOES s_Texture; void main() { gl_FragColor = texture2D(s_Texture, v_TexCoord); }";

    private static final String OBJECT_VERTEX_SHADER =
            "uniform mat4 u_MvpMatrix; attribute vec4 a_Position; attribute vec2 a_TexCoord; varying vec2 v_TexCoord; void main() { v_TexCoord = a_TexCoord; gl_Position = u_MvpMatrix * a_Position; }";
    private static final String OBJECT_FRAGMENT_SHADER =
            "precision mediump float; varying vec2 v_TexCoord; uniform sampler2D s_Texture; void main() { vec4 color = texture2D(s_Texture, v_TexCoord); if(color.a < 0.1) discard; gl_FragColor = color; }";

    private static final float[] QUAD_COORDS = new float[]{ -1.0f, -1.0f, 0.0f, -1.0f, +1.0f, 0.0f, +1.0f, -1.0f, 0.0f, +1.0f, +1.0f, 0.0f };
    private static final float[] QUAD_TEX_COORDS = new float[]{ 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f };
    private static final float[] OBJECT_COORDS = new float[]{ -0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, -0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.0f };
    // endregion

    private FloatBuffer quadCoords, quadTexCoords, transformedQuadTexCoords, objectCoords, objectTexCoords;
    private int cameraProgram, cameraPositionHandle, cameraTexCoordHandle, cameraTextureHandle;
    private int objectProgram, objectPositionHandle, objectTexCoordHandle, objectMvpMatrixHandle, objectTextureHandle;
    private int cameraTextureId = -1, leaseIconTextureId = -1, shortIconTextureId = -1;

    private Session session;
    private final ArActivity activity;
    private Location currentLocation;

    private final float[] modelMatrix = new float[16], viewMatrix = new float[16], projectionMatrix = new float[16], mvpMatrix = new float[16];

    public ArRenderer(ArActivity activity) {
        this.activity = activity;
    }

    public void setSession(Session session) { this.session = session; }
    public void setCurrentLocation(Location location) { this.currentLocation = location; }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        cameraTextureId = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        cameraProgram = createProgram(CAMERA_VERTEX_SHADER, CAMERA_FRAGMENT_SHADER);
        cameraPositionHandle = GLES20.glGetAttribLocation(cameraProgram, "a_Position");
        cameraTexCoordHandle = GLES20.glGetAttribLocation(cameraProgram, "a_TexCoord");
        cameraTextureHandle = GLES20.glGetUniformLocation(cameraProgram, "s_Texture");

        objectProgram = createProgram(OBJECT_VERTEX_SHADER, OBJECT_FRAGMENT_SHADER);
        objectPositionHandle = GLES20.glGetAttribLocation(objectProgram, "a_Position");
        objectTexCoordHandle = GLES20.glGetAttribLocation(objectProgram, "a_TexCoord");
        objectMvpMatrixHandle = GLES20.glGetUniformLocation(objectProgram, "u_MvpMatrix");
        objectTextureHandle = GLES20.glGetUniformLocation(objectProgram, "s_Texture");

        leaseIconTextureId = loadTexture(activity, R.drawable.leaseicon);
        shortIconTextureId = loadTexture(activity, R.drawable.shorticon);

        quadCoords = createFloatBuffer(QUAD_COORDS);
        quadTexCoords = createFloatBuffer(QUAD_TEX_COORDS);
        transformedQuadTexCoords = createFloatBuffer(QUAD_TEX_COORDS);
        objectCoords = createFloatBuffer(OBJECT_COORDS);
        objectTexCoords = createFloatBuffer(QUAD_TEX_COORDS);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        if (session != null) {
            int rotation;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                rotation = activity.getDisplay().getRotation();
            } else {
                rotation = ((android.view.WindowManager) activity.getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay()
                        .getRotation();
            }
            session.setDisplayGeometry(rotation, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (session == null) return;
        session.setCameraTextureName(cameraTextureId);

        try {
            Frame frame = session.update();
            com.google.ar.core.Camera camera = frame.getCamera();

            // ⭐ [추가됨] AR 상태 업데이트
            String statusText = "AR Status: " + camera.getTrackingState().toString();
            if (camera.getTrackingState() == TrackingState.PAUSED) {
                statusText += "\n(주변을 비춰주세요)";
            }
            activity.updateArStatus(statusText);

            drawBackground(frame);

            if (camera.getTrackingState() == TrackingState.TRACKING) {
                camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 1000.0f);
                camera.getViewMatrix(viewMatrix, 0);

                List<Object> currentlyVisibleProperties = new ArrayList<>();
                for (ArActivity.PropertyForAR prop : activity.getArProperties()) {
                    if (prop.anchorCreationRequested && currentLocation != null) {
                        float[] translation = calculateTranslationInAR(currentLocation, prop.latitude, prop.longitude);
                        float east = translation[0];
                        float north = translation[1];

                        Pose anchorPose = Pose.makeTranslation(east, -1.0f, -north);
                        prop.anchor = session.createAnchor(anchorPose);
                        prop.anchorCreationRequested = false;
                    }

                    if (prop.anchor != null && prop.anchor.getTrackingState() == TrackingState.TRACKING) {
                        drawMarker(prop, camera.getPose(), viewMatrix, projectionMatrix);

                        if (isAnchorVisibleInScreen(prop.anchor, viewMatrix, projectionMatrix)) {
                            Object fullProperty = activity.getPropertyById(prop.id, prop.type);
                            if (fullProperty != null) currentlyVisibleProperties.add(fullProperty);
                        }
                    }
                }
                activity.updateVisibleProperties(currentlyVisibleProperties);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Exception onDrawFrame", t);
        }
    }

    private void drawMarker(ArActivity.PropertyForAR prop, Pose cameraPose, float[] viewMatrix, float[] projectionMatrix) {
        float[] anchorMatrix = new float[16];
        prop.anchor.getPose().toMatrix(anchorMatrix, 0);

        float[] anchorTranslation = new float[]{anchorMatrix[12], anchorMatrix[13], anchorMatrix[14]};

        float bobbingOffset = 0.0f;
        if (prop.distance <= 50.0f) {
            float time = (float)(System.currentTimeMillis() % 1000L) / 1000.0f * (float)Math.PI * 2.0f;
            bobbingOffset = (float)Math.sin(time) * 0.5f;
        }

        float[] cameraTranslation = cameraPose.getTranslation();
        float[] lookVector = new float[]{
                cameraTranslation[0] - anchorTranslation[0],
                0,
                cameraTranslation[2] - anchorTranslation[2]
        };

        double angle = Math.atan2(lookVector[0], lookVector[2]);
        float rotationDegrees = (float) Math.toDegrees(angle);

        Matrix.setIdentityM(modelMatrix, 0);

        Matrix.translateM(modelMatrix, 0, anchorTranslation[0], anchorTranslation[1] + bobbingOffset, anchorTranslation[2]);
        Matrix.rotateM(modelMatrix, 0, rotationDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.rotateM(modelMatrix, 0, 90.0f, 0.0f, 0.0f, 1.0f);

        Matrix.scaleM(modelMatrix, 0, 20.0f, 20.0f, 20.0f);

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        drawIcon(prop);
    }

    private void drawBackground(Frame frame) {
        GLES20.glDepthMask(false);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        GLES20.glUseProgram(cameraProgram);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
        frame.transformDisplayUvCoords(quadTexCoords, transformedQuadTexCoords);
        GLES20.glVertexAttribPointer(cameraPositionHandle, 3, GLES20.GL_FLOAT, false, 0, quadCoords);
        GLES20.glVertexAttribPointer(cameraTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, transformedQuadTexCoords);
        GLES20.glEnableVertexAttribArray(cameraPositionHandle);
        GLES20.glEnableVertexAttribArray(cameraTexCoordHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(cameraPositionHandle);
        GLES20.glDisableVertexAttribArray(cameraTexCoordHandle);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthMask(true);
    }

    private void drawIcon(ArActivity.PropertyForAR prop) {
        GLES20.glUseProgram(objectProgram);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        int textureId = "lease".equals(prop.type) ? leaseIconTextureId : shortIconTextureId;
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(objectTextureHandle, 0);

        GLES20.glUniformMatrix4fv(objectMvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glEnableVertexAttribArray(objectPositionHandle);
        GLES20.glEnableVertexAttribArray(objectTexCoordHandle);
        GLES20.glVertexAttribPointer(objectPositionHandle, 3, GLES20.GL_FLOAT, false, 0, objectCoords);
        GLES20.glVertexAttribPointer(objectTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, objectTexCoords);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(objectPositionHandle);
        GLES20.glDisableVertexAttribArray(objectTexCoordHandle);

        GLES20.glDisable(GLES20.GL_BLEND);
    }

    private boolean isAnchorVisibleInScreen(Anchor anchor, float[] viewMatrix, float[] projectionMatrix) {
        float[] vpMatrix = new float[16];
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        float[] anchorPose = new float[16];
        anchor.getPose().toMatrix(anchorPose, 0);
        float[] worldPos = {anchorPose[12], anchorPose[13], anchorPose[14], 1};
        float[] screenPos = new float[4];
        Matrix.multiplyMV(screenPos, 0, vpMatrix, 0, worldPos, 0);

        float w = screenPos[3];
        return w > 0 && Math.abs(screenPos[0]) < w * 1.2f && Math.abs(screenPos[1]) < w * 1.2f;
    }

    // region HELPER FUNCTIONS
    private static float[] calculateTranslationInAR(Location userLocation, double targetLatitude, double targetLongitude) {
        Location targetLocation = new Location("");
        targetLocation.setLatitude(targetLatitude);
        targetLocation.setLongitude(targetLongitude);

        float distance = userLocation.distanceTo(targetLocation);
        float bearing = userLocation.bearingTo(targetLocation);
        double bearingRad = Math.toRadians(bearing);

        float east = (float) (distance * Math.sin(bearingRad));
        float north = (float) (distance * Math.cos(bearingRad));

        return new float[]{east, north};
    }

    private static int loadTexture(Context context, int resourceId) {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0) {
            Bitmap bitmap = getBitmapFromVectorDrawable(context, resourceId);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
        if (textureHandle[0] == 0) {
            throw new RuntimeException("Error loading texture.");
        }
        return textureHandle[0];
    }

    private static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private static int createProgram(String vertex, String fragment) {
        int v = loadShader(GLES20.GL_VERTEX_SHADER, vertex);
        if (v == 0) return 0;
        int f = loadShader(GLES20.GL_FRAGMENT_SHADER, fragment);
        if (f == 0) return 0;
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Error linking program: " + GLES20.glGetProgramInfoLog(p));
            GLES20.glDeleteProgram(p);
            p = 0;
        }
        return p;
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    private static FloatBuffer createFloatBuffer(float[] coords) {
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }
    // endregion
}