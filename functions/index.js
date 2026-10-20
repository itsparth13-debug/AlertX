const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

/**
 * Cloud Function triggered on incident document update in Firestore.
 * Sends FCM notification to the reporter if their status changes.
 */
exports.onIncidentStatusChange = functions.firestore
  .document("incidents/{incidentId}")
  .onUpdate(async (change, context) => {
    const beforeData = change.before.data();
    const afterData = change.after.data();

    if (!beforeData || !afterData) {
      return null;
    }

    // Check if status changed
    if (beforeData.status === afterData.status) {
      return null;
    }

    const reporterId = afterData.reporterId;
    if (!reporterId) {
      console.log("Incident is anonymous; no user FCM token to notify directly.");
      return null;
    }

    // Fetch reporter's FCM token from users collection
    const userDoc = await admin.firestore().collection("users").doc(reporterId).get();
    if (!userDoc.exists) {
      console.log(`User ${reporterId} not found.`);
      return null;
    }

    const fcmToken = userDoc.data().fcmToken;
    if (!fcmToken) {
      console.log(`User ${reporterId} has no FCM token.`);
      return null;
    }

    const title = afterData.title || "Incident Report Update";
    const status = afterData.status;
    const trackingCode = afterData.trackingCode || context.params.incidentId;

    const message = {
      token: fcmToken,
      notification: {
        title: `Incident Update: ${trackingCode}`,
        body: `Your report "${title}" status changed to ${status}.`,
      },
      data: {
        incidentId: context.params.incidentId,
        trackingCode: trackingCode,
        status: status,
      },
    };

    try {
      const response = await admin.messaging().send(message);
      console.log("Successfully sent status change notification:", response);
      return response;
    } catch (error) {
      console.error("Error sending status change notification:", error);
      return null;
    }
  });
