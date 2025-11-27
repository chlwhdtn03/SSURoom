/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");
const logger = require("firebase-functions/logger");

// Firebase Admin SDK 초기화
initializeApp();

// "sendChatNotification"이라는 이름의 함수를 정의하고 export 합니다.
// 이 함수는 chat_rooms/{chatRoomId}/messages/{messageId} 경로에 새 문서가 생성될 때마다 실행됩니다.
exports.sendChatNotification = onDocumentCreated("chat_rooms/{chatRoomId}/messages/{messageId}", async (event) => {
  const chatRoomId = event.params.chatRoomId;
  const messageData = event.data.data();
  const senderId = messageData.senderId;

  // 1. 채팅방 정보를 가져와서 참여자 목록(userIds)을 확인합니다.
  const chatRoomSnap = await getFirestore().collection("chat_rooms").doc(chatRoomId).get();
  if (!chatRoomSnap.exists) {
    logger.log("채팅방을 찾을 수 없습니다:", chatRoomId);
    return;
  }
  const userIds = chatRoomSnap.data().userIds;

  // 2. 참여자 목록에서 메시지를 보낸 사람을 제외하여 수신자를 찾습니다.
  const receiverId = userIds.find((id) => id !== senderId);
  if (!receiverId) {
    logger.log("수신자를 찾을 수 없습니다. (채팅방 참여자 수:", userIds.length, ")");
    return;
  }

  // 3. 수신자의 사용자 문서에서 FCM 토큰(알림 주소)을 가져옵니다.
  const receiverSnap = await getFirestore().collection("users").doc(receiverId).get();
  if (!receiverSnap.exists) {
    logger.log("수신자 사용자 문서를 찾을 수 없습니다:", receiverId);
    return;
  }
  const fcmToken = receiverSnap.data().fcmToken;

  if (!fcmToken) {
    logger.log("수신자가 FCM 토큰을 가지고 있지 않습니다:", receiverId);
    return;
  }

  // 4. 알림에 표시될 보낸 사람의 이름을 가져옵니다.
  const senderSnap = await getFirestore().collection("users").doc(senderId).get();
  const senderName = senderSnap.exists() ? senderSnap.data().name : "새 메시지";

  // 5. 알림 페이로드를 구성합니다.
  // 이 데이터는 앱의 MyFirebaseMessagingService#onMessageReceived 에서 수신됩니다.
  const payload = {
    token: fcmToken,
    data: {
      chatRoomId: chatRoomId,
      senderName: senderName,
      message: messageData.text || "사진을 보냈습니다.", // 텍스트가 없으면 기본 메시지 사용
    },
  };

  logger.log("알림 전송 시도:", payload);

  // 6. FCM을 통해 알림 메시지를 전송합니다.
  try {
    const response = await getMessaging().send(payload);
    logger.log("알림 전송 성공:", response);
  } catch (error) {
    logger.error("알림 전송 중 오류 발생:", error);
  }
});
