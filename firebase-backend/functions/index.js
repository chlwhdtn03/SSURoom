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

initializeApp();

exports.sendChatNotification = onDocumentCreated("chat_rooms/{chatRoomId}/messages/{messageId}", async (event) => {
  const chatRoomId = event.params.chatRoomId;
  const messageData = event.data.data();
  const senderId = messageData.senderId;

  const chatRoomSnap = await getFirestore().collection("chat_rooms").doc(chatRoomId).get();
  if (!chatRoomSnap.exists) {
    logger.log("채팅방을 찾을 수 없습니다:", chatRoomId);
    return;
  }
  const userIds = chatRoomSnap.data().userIds;

  const receiverId = userIds.find((id) => id !== senderId);
  if (!receiverId) {
    logger.log("수신자를 찾을 수 없습니다. (채팅방 참여자 수:", userIds.length, ")");
    return;
  }

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

  const senderSnap = await getFirestore().collection("users").doc(senderId).get();
  const senderName = senderSnap.exists ? senderSnap.data().name : "새 메시지";

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

  try {
    const response = await getMessaging().send(payload);
    logger.log("알림 전송 성공:", response);
  } catch (error) {
    logger.error("알림 전송 중 오류 발생:", error);
  }
});

exports.sendPropertyNotification = onDocumentCreated("short_terms/{propertyId}", async (event) => {
  const newProperty = event.data.data();
  const propertyId = event.params.propertyId;

  logger.log(`새로운 단기임대 매물 감지: ${propertyId}`, newProperty);

  if (!newProperty.pricing || newProperty.pricing.weeklyPrice === undefined) {
    logger.log("가격 정보가 없는 매물이라 알림을 보내지 않습니다.");
    return;
  }

  const propertyPrice = newProperty.pricing.weeklyPrice;

  const usersSnap = await getFirestore().collection("users").get();
  if (usersSnap.empty) {
    logger.log("알림 설정을 한 사용자가 없습니다.");
    return;
  }

  const notificationPromises = [];
  const notifiedUsers = new Set();

  usersSnap.forEach((userDoc) => {
    const user = userDoc.data();
    const userId = userDoc.id;

    if (user.notificationFilters && user.notificationFilters.length > 0 && user.fcmToken) {
      for (const filter of user.notificationFilters) {
        if (notifiedUsers.has(userId)) {
          continue;
        }

        const isTypeMatch = filter.propertyType === "short_term";
        const isPriceMatch = propertyPrice >= filter.minPrice && propertyPrice <= filter.maxPrice;

        if (isTypeMatch && isPriceMatch) {
          logger.log(`매물(${propertyId})이 사용자(${userId})의 필터와 일치합니다.`, filter);
          notifiedUsers.add(userId);

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
          break;
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
