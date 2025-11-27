package cse.ssuroom.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import cse.ssuroom.MainActivity;
import cse.ssuroom.R;

public class NotificationHelper {

    private static final String CHAT_CHANNEL_ID = "ssuroom_chat_channel";
    private static final String CHAT_CHANNEL_NAME = "채팅 알림";
    private static final String CHAT_CHANNEL_DESCRIPTION = "새로운 채팅 메시지 알림입니다.";

    private static final String PROPERTY_CHANNEL_ID = "ssuroom_property_channel";
    private static final String PROPERTY_CHANNEL_NAME = "매물 알림";
    private static final String PROPERTY_CHANNEL_DESCRIPTION = "새로운 매물 등록 알림입니다.";


    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chatChannel = new NotificationChannel(CHAT_CHANNEL_ID, CHAT_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            chatChannel.setDescription(CHAT_CHANNEL_DESCRIPTION);

            NotificationChannel propertyChannel = new NotificationChannel(PROPERTY_CHANNEL_ID, PROPERTY_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            propertyChannel.setDescription(PROPERTY_CHANNEL_DESCRIPTION);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(chatChannel);
                notificationManager.createNotificationChannel(propertyChannel);
            }
        }
    }

    public static void showNewMessageNotification(Context context, String chatRoomId, String senderName, String message) {

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("chat_room_id", chatRoomId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, chatRoomId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHAT_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(senderName)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(context).notify(message.hashCode(), builder.build());
    }

    public static void showNewPropertyNotification(Context context, String propertyId, String title, String message) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("property_id", propertyId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, propertyId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PROPERTY_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManagerCompat.from(context).notify(propertyId.hashCode(), builder.build());
    }
}
