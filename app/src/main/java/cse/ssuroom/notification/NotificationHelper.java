package cse.ssuroom.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import cse.ssuroom.MainActivity;
import cse.ssuroom.R;

public class NotificationHelper {

    private static final String CHANNEL_ID = "ssuroom_chat_channel";
    private static final String CHANNEL_NAME = "채팅 알림";
    private static final String CHANNEL_DESCRIPTION = "새로운 채팅 메시지 알림입니다.";

    /**
     * 알림 채널을 생성합니다. Android 8.0 (Oreo) 이상에서만 필요합니다.
     * 앱 시작 시 한 번만 호출하면 됩니다.
     */
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * 새로운 메시지에 대한 알림을 생성하고 표시합니다.
     *
     * @param context    컨텍스트
     * @param chatRoomId 알림을 탭했을 때 이동할 채팅방의 ID
     * @param senderName 알림에 표시될 보낸 사람의 이름
     * @param message    알림에 표시될 메시지 내용
     */
    public static void showNewMessageNotification(Context context, String chatRoomId, String senderName, String message) {

        // 알림을 탭했을 때 MainActivity를 열고, 채팅방 ID를 전달하는 Intent 생성
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("chat_room_id", chatRoomId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, chatRoomId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 알림 내용 구성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher) // TODO: 앱 아이콘으로 교체 권장
                .setContentTitle(senderName)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent) // 알림 탭 시 실행할 Intent
                .setAutoCancel(true); // 알림을 탭하면 자동으로 사라지도록 설정

        // 각 채팅방마다 고유한 ID로 알림을 띄워, 여러 채팅방에서 메시지가 와도 덮어쓰지 않도록 함
        NotificationManagerCompat.from(context).notify(chatRoomId.hashCode(), builder.build());
    }
}
