package cse.ssuroom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
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
import android.location.Location;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import cse.ssuroom.map.GeoUtil;

public class ArRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "ArRenderer";

    // Shaders
    private static final String CAMERA_VERTEX_SHADER =
            "attribute vec4 a_Position;\n"
                    + "attribute vec2 a_TexCoord;\n"
                    + "varying vec2 v_TexCoord;\n"
                    + "void main() {\n"
                    + "    v_TexCoord = a_TexCoord;\n"
                    + "    gl_Position = a_Position;\n"
                    + "}";

    private static final String CAMERA_FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n"
                    + "precision mediump float;\n"
                    + "varying vec2 v_TexCoord;\n"
                    + "uniform samplerExternalOES s_Texture;\n"
                    + "void main() {\n"
                    + "    gl_FragColor = texture2D(s_Texture, v_TexCoord);\n"
                    + "}";

    private static final String ICON_VERTEX_SHADER =
            "uniform mat4 u_MvpMatrix;\n"
                    + "attribute vec4 a_Position;\n"
                    + "attribute vec2 a_TexCoord;\n"
                    + "varying vec2 v_TexCoord;\n"
                    + "void main() {\n"
                    + "    v_TexCoord = a_TexCoord;\n"
                    + "    gl_Position = u_MvpMatrix * a_Position;\n"
                    + "}";

    private static final String ICON_FRAGMENT_SHADER =
            "precision mediump float;\n"
                    + "varying vec2 v_TexCoord;\n"
                    + "uniform sampler2D s_Texture;\n"
                    + "void main() {\n"
                    + "    gl_FragColor = texture2D(s_Texture, v_TexCoord);\n"
                    + "}";

    // Vertex data
    private static final float[] QUAD_COORDS = new float[]{
            -1.0f, -1.0f, 0.0f, -1.0f, +1.0f, 0.0f, +1.0f, -1.0f, 0.0f, +1.0f, +1.0f, 0.0f,
    };
    private static final float[] QUAD_TEX_COORDS = new float[]{
            0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f,
    };
    private static final float[] ICON_COORDS = new float[]{
            -0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, -0.5f, 0.5f, 0.0f, 0.5f, 0.5f, 0.0f
    };

    private FloatBuffer quadCoords;
    private FloatBuffer quadTexCoords;
    private FloatBuffer transformedQuadTexCoords;
    private FloatBuffer iconCoords;
    private FloatBuffer iconTexCoords;

    private int cameraProgram;
    private int cameraPositionHandle;
    private int cameraTexCoordHandle;
    private int cameraTextureHandle;
    private int iconProgram;
    private int iconPositionHandle;
    private int iconTexCoordHandle;
    private int iconMvpMatrixHandle;
    private int iconTextureHandle;
    private int cameraTextureId = -1;
    private int leaseIconTextureId;
    private int shortIconTextureId;

    private Session session;
    private final ArActivity activity;
    private Location currentLocation;

    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    
    public ArRenderer(ArActivity activity) {
        this.activity = activity;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }

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
        if(cameraPositionHandle == -1) throw new RuntimeException("a_Position not found in camera shader");
        cameraTexCoordHandle = GLES20.glGetAttribLocation(cameraProgram, "a_TexCoord");
        if(cameraTexCoordHandle == -1) throw new RuntimeException("a_TexCoord not found in camera shader");
        cameraTextureHandle = GLES20.glGetUniformLocation(cameraProgram, "s_Texture");
        if(cameraTextureHandle == -1) throw new RuntimeException("s_Texture not found in camera shader");

        iconProgram = createProgram(ICON_VERTEX_SHADER, ICON_FRAGMENT_SHADER);
        iconPositionHandle = GLES20.glGetAttribLocation(iconProgram, "a_Position");
        if(iconPositionHandle == -1) throw new RuntimeException("a_Position not found in icon shader");
        iconTexCoordHandle = GLES20.glGetAttribLocation(iconProgram, "a_TexCoord");
        if(iconTexCoordHandle == -1) throw new RuntimeException("a_TexCoord not found in icon shader");
        iconMvpMatrixHandle = GLES20.glGetUniformLocation(iconProgram, "u_MvpMatrix");
        if(iconMvpMatrixHandle == -1) throw new RuntimeException("u_MvpMatrix not found in icon shader");
        iconTextureHandle = GLES20.glGetUniformLocation(iconProgram, "s_Texture");
        if(iconTextureHandle == -1) throw new RuntimeException("s_Texture not found in icon shader");
        
        leaseIconTextureId = loadTexture(activity, R.drawable.leaseicon, 128, 128);
        shortIconTextureId = loadTexture(activity, R.drawable.shorticon, 128, 128);

        quadCoords = createFloatBuffer(QUAD_COORDS);
        quadTexCoords = createFloatBuffer(QUAD_TEX_COORDS);
        transformedQuadTexCoords = createFloatBuffer(QUAD_TEX_COORDS);
        iconCoords = createFloatBuffer(ICON_COORDS);
        iconTexCoords = createFloatBuffer(QUAD_TEX_COORDS);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        activity.getDisplayRotationHelper().onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (session == null) return;

        activity.getDisplayRotationHelper().updateSessionIfNeeded(session);

        try {
            session.setCameraTextureName(cameraTextureId);
            Frame frame = session.update();
            com.google.ar.core.Camera camera = frame.getCamera();
            drawBackground(frame);

            if (camera.getTrackingState() == TrackingState.TRACKING) {
                frame.getCamera().getViewMatrix(viewMatrix, 0);
                frame.getCamera().getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f);
                
                List<Object> currentlyVisibleProperties = new ArrayList<>();
                for (ArActivity.PropertyForAR prop : activity.getArProperties()) {
                    if (prop.anchorCreationRequested && currentLocation != null) {
                        float[] translation = GeoUtil.calculateTranslationInAR(currentLocation, prop.latitude, prop.longitude);
                        Pose anchorPose = Pose.makeTranslation(translation[0], translation[2], -translation[1]);
                        prop.anchor = session.createAnchor(anchorPose);
                        prop.anchorCreationRequested = false;
                    }
                    if (prop.anchor != null && prop.anchor.getTrackingState() == TrackingState.TRACKING) {
                        // This is the crucial check
                        if (isAnchorVisible(prop.anchor, viewMatrix, projectionMatrix)) {
                            drawMarker(prop, camera, viewMatrix, projectionMatrix);
                            Object fullProperty = activity.getPropertyById(prop.id, prop.type);
                            if (fullProperty != null) currentlyVisibleProperties.add(fullProperty);
                        }
                    }
                }
                activity.updateVisibleProperties(currentlyVisibleProperties);
            }
        } catch (com.google.ar.core.exceptions.CameraNotAvailableException e) {
            Log.e(TAG, "Exception onDrawFrame", e);
        }
    }

    private void drawMarker(ArActivity.PropertyForAR prop, com.google.ar.core.Camera camera, float[] viewMatrix, float[] projectionMatrix) {
        float[] anchorMatrix = new float[16];
        prop.anchor.getPose().toMatrix(anchorMatrix, 0);

        float[] cameraPoseMatrix = new float[16];
        camera.getPose().toMatrix(cameraPoseMatrix, 0);
        
        float[] cameraPosition = {cameraPoseMatrix[12], cameraPoseMatrix[13], cameraPoseMatrix[14]};
        float[] anchorPosition = {anchorMatrix[12], anchorMatrix[13], anchorMatrix[14]};

        float[] lookAtMatrix = new float[16];
        Matrix.setLookAtM(lookAtMatrix, 0, anchorPosition[0], anchorPosition[1], anchorPosition[2], cameraPosition[0], cameraPosition[1], cameraPosition[2], 0f, 1f, 0f);
        
        Matrix.invertM(modelMatrix, 0, lookAtMatrix, 0);

        // Apply distance-based scaling
        float distance = prop.distance;
        float scaleFactor = 1.5f - (distance / 500.0f) * 0.9f;
        scaleFactor = Math.max(0.6f, Math.min(scaleFactor, 1.5f));
        Matrix.scaleM(modelMatrix, 0, scaleFactor, scaleFactor, scaleFactor);

        if (distance < 50) {
            float time = (float) (System.currentTimeMillis() % 2000) / 2000.0f;
            Matrix.translateM(modelMatrix, 0, 0f, (float) (Math.sin(time * 2 * Math.PI) * 0.2f), 0f);
        }

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
        GLES20.glDisable(GLES20.GL_CULL_FACE);
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        
        GLES20.glUseProgram(iconProgram);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        int textureId = "lease".equals(prop.type) ? leaseIconTextureId : shortIconTextureId;
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(iconTextureHandle, 1);
        GLES20.glUniformMatrix4fv(iconMvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glVertexAttribPointer(iconPositionHandle, 3, GLES20.GL_FLOAT, false, 0, iconCoords);
        GLES20.glVertexAttribPointer(iconTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, iconTexCoords);
        GLES20.glEnableVertexAttribArray(iconPositionHandle);
        GLES20.glEnableVertexAttribArray(iconTexCoordHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        checkGlError("drawIcon");
        GLES20.glDisableVertexAttribArray(iconPositionHandle);
        GLES20.glDisableVertexAttribArray(iconTexCoordHandle);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
    
    private boolean isAnchorVisible(Anchor anchor, float[] viewMatrix, float[] projectionMatrix) {
        float[] anchorPose = new float[16];
        anchor.getPose().toMatrix(anchorPose, 0);

        float[] worldPos = {anchorPose[12], anchorPose[13], anchorPose[14], 1};

        float[] viewProjectionMatrix = new float[16];
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);

        float[] screenPos = new float[4];
        Matrix.multiplyMV(screenPos, 0, viewProjectionMatrix, 0, worldPos, 0);

        return screenPos[3] > 0
                && Math.abs(screenPos[0] / screenPos[3]) < 1
                && Math.abs(screenPos[1] / screenPos[3]) < 1;
    }

    private static int loadTexture(Context context, int resourceId, int width, int height) {
        final int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0) {
            Bitmap bitmap = getBitmapFromVectorDrawable(context, resourceId, width, height);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
        if (textureHandle[0] == 0) throw new RuntimeException("Error loading texture.");
        return textureHandle[0];
    }

    private static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId, int width, int height) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private static int createProgram(String vertex, String fragment) {
        int v = loadShader(GLES20.GL_VERTEX_SHADER, vertex);
        int f = loadShader(GLES20.GL_FRAGMENT_SHADER, fragment);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        return p;
    }

    private static int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
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

    private static void checkGlError(String label) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }
}