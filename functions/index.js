const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// Exported Cloud Function to update a user's password securely
exports.updateUserPassword = functions.https.onCall(async (data, context) => {
    const email = data.email;
    const newPassword = data.newPassword;

    // Validate inputs
    if (!email || !newPassword) {
        throw new functions.https.HttpsError(
            "invalid-argument",
            "The function must be called with an email and newPassword."
        );
    }

    try {
        // Fetch the user by their email address
        const userRecord = await admin.auth().getUserByEmail(email);

        // Update their password in Firebase Auth natively
        await admin.auth().updateUser(userRecord.uid, {
            password: newPassword
        });

        // Return a successful response indicating the password change worked
        return { success: true, message: "Password updated successfully." };
    } catch (error) {
        console.error("Error updating password:", error);
        throw new functions.https.HttpsError(
            "internal",
            "Failed to update password: " + error.message
        );
    }
});
