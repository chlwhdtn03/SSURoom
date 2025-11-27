package cse.ssuroom.notification;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    /**
     * FCM 메시지를 수신했을 때 호출됩니다.
     * 앱이 포그라운드 상태일 때 데이터 페이로드를 받습니다.
     * 백그라운드 상태일 때는 시스템 트레이에 자동으로 알림이 표시될 수 있습니다. (알림 페이로드의 경우)
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // 메시지에 데이터 페이로드가 포함되어 있는지 확인합니다.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());

            // 데이터 페이로드에서 채팅 정보 추출
            Map<String, String> data = remoteMessage.getData();
            String chatRoomId = data.get("chatRoomId");
            String senderName = data.get("senderName");
            String message = data.get("message");

            // 알림을 표시하는 헬퍼 메서드 호출
            if (chatRoomId != null && senderName != null && message != null) {
                NotificationHelper.showNewMessageNotification(this, chatRoomId, senderName, message);
            }
        }

        // 메시지에 알림 페이로드가 포함되어 있는지 확인합니다. (보통 데이터 페이로드와 함께 사용)
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }

    /**
     * 새로운 FCM 등록 토큰이 생성될 때마다 호출됩니다.
     * 이 토큰은 특정 디바이스로 메시지를 보낼 때 사용되는 고유한 주소입니다.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // 이 토큰을 서버(Firestore)로 보내서 현재 사용자의 정보에 저장해야 합니다.
        sendRegistrationToServer(token);
    }

    /**
     * 새로운 토큰을 Firestore의 현재 사용자 문서에 저장합니다.
     */
    private void sendRegistrationToServer(String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated successfully for user: " + currentUser.getUid()))
                    .addOnFailureListener(e -> Log.w(TAG, "Error updating FCM token", e));
        }
    }
}
