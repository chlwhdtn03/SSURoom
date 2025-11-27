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
  const senderName = senderSnap.exists ? senderSnap.data().name : "새 메시지";

  // 5. 알림 페이로드를 구성합니다.
  // 이 데이터는 앱의 MyFirebaseMessagingService#onMessageReceived 에서 수신됩니다.
  const payload = {
    token: fcmToken,
    data: {
      type: "chat",
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

// --- 아래 함수가 새로 추가됩니다 ---
exports.sendPropertyNotification = onDocumentCreated("short_terms/{propertyId}", async (event) => {
  const newProperty = event.data.data();
  const propertyId = event.params.propertyId;

  logger.log(`새로운 단기임대 매물 감지: ${propertyId}`, newProperty);

  // 가격 정보가 없으면 함수 종료
  if (!newProperty.pricing || newProperty.pricing.weeklyPrice === undefined) {
    logger.log("가격 정보가 없는 매물이라 알림을 보내지 않습니다.");
    return;
  }

  const propertyPrice = newProperty.pricing.weeklyPrice;

  // 1. 모든 사용자를 가져옵니다.
  const usersSnap = await getFirestore().collection("users").get();
  if (usersSnap.empty) {
    logger.log("알림 설정을 한 사용자가 없습니다.");
    return;
  }

  // 알림을 보낼 사용자 목록 (중복 방지용)
  const notificationPromises = [];
  const notifiedUsers = new Set();

  // 2. 각 사용자의 필터와 새 매물을 비교합니다.
  usersSnap.forEach((userDoc) => {
    const user = userDoc.data();
    const userId = userDoc.id;

    if (user.notificationFilters && user.notificationFilters.length > 0 && user.fcmToken) {
      // 3. 사용자의 각 필터 조건을 확인합니다.
      for (const filter of user.notificationFilters) {
        // 이미 이 매물에 대해 알림을 보낸 유저라면 건너뜁니다.
        if (notifiedUsers.has(userId)) {
          continue;
        }

        const isTypeMatch = filter.propertyType === "short_term";
        const isPriceMatch = propertyPrice >= filter.minPrice && propertyPrice <= filter.maxPrice;

        // 4. 모든 조건이 일치하면 알림을 보냅니다.
        if (isTypeMatch && isPriceMatch) {
          logger.log(`매물(${propertyId})이 사용자(${userId})의 필터와 일치합니다.`, filter);
          notifiedUsers.add(userId); // 알림 보냈다고 기록

          const payload = {
            token: user.fcmToken,
            data: {
              type: "new_property",
              propertyId: propertyId,
              title: "새로운 맞춤 매물 도착!",
              body: `'${newProperty.title}' 매물이 등록되었습니다.`,
            },
          };

          notificationPromises.push(getMessaging().send(payload));
          break; // 이 사용자는 알림을 보냈으니 다음 사용자로 넘어감
        }
      }
    }
  });

  if (notificationPromises.length > 0) {
    logger.log(`${notificationPromises.length}명의 사용자에게 알림을 보냅니다.`);
    await Promise.all(notificationPromises);
    logger.log("모든 맞춤 매물 알림을 성공적으로 보냈습니다.");
  } else {
    logger.log("이 매물과 일치하는 필터를 가진 사용자가 없습니다.");
  }
});
