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

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // --- 여기부터 수정된 부분 ---
        Log.d(TAG, "FCM 메시지 수신!");
        Log.d(TAG, "보낸 사람: " + remoteMessage.getFrom());

        // 메시지에 데이터 페이로드가 포함되어 있는지 확인합니다.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "메시지 데이터: " + remoteMessage.getData());

            // 데이터 페이로드에서 채팅 정보 추출
            Map<String, String> data = remoteMessage.getData();
            String chatRoomId = data.get("chatRoomId");
            String senderName = data.get("senderName");
            String message = data.get("message");

            if (chatRoomId != null && senderName != null && message != null) {
                Log.d(TAG, "알림 생성 시도: " + senderName + " - " + message);
                // 알림을 표시하는 헬퍼 메서드 호출
                NotificationHelper.showNewMessageNotification(this, chatRoomId, senderName, message);
                Log.d(TAG, "알림 생성이 NotificationHelper에 요청되었습니다.");
            } else {
                Log.e(TAG, "수신된 데이터에 필수 정보가 누락되었습니다.");
            }
        } else {
            Log.d(TAG, "데이터 페이로드가 없는 메시지입니다.");
        }
        // --- 여기까지 ---
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        sendRegistrationToServer(token);
    }

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
