package Utility;

import android.util.Log;

import Models.Feedback;

public class FeedbackEmailTemplate {
    private static final String TEMPLATE =
            "<html><body>" +
                    "<h2>New Feedback Received:</h2>" +
                    "<h2>-------------------------------------</h2>" +
                    "<hr/>" +
                    "<p><strong>Rating:</strong><br/>%s</p>" +
                    "<p><strong>Feedback:</strong><br/>%s</p>" +
                    "%s" + // User section placeholder
                    "<p><strong>Device Information:</strong><br/>%s</p>" +
                    "<hr/>" +
                    "<h2>-------------------------------------</h2>" +
                    "<p><small>Submitted on: %s</small></p>" +
                    "</body></html>";

    private static final String USER_SECTION_TEMPLATE =
            "<p><strong>User Contact</strong><br/>" +
                    "Email: %s</p>";

    public static String createEmailContent(Feedback feedback) {
        try {
            // Generate star rating
            String stars = generateStarRating(feedback.getRating());

            // Get feedback text
            String feedbackText = feedback.getFeedbackText() != null ?
                    escapeHtml(feedback.getFeedbackText()).replace("\n", "<br/>") : "";

            // Generate user section if not anonymous
            String userSection = "";
            if (!feedback.isAnonymous() && feedback.getUserEmail() != null
                    && !feedback.getUserEmail().isEmpty()) {
                userSection = String.format(USER_SECTION_TEMPLATE,
                        escapeHtml(feedback.getUserEmail()));
            }

            // Get device info
            String deviceInfo = feedback.getDeviceInfo() != null ?
                    escapeHtml(feedback.getDeviceInfo()).replace("\n", "<br/>") : "";

            // Combine everything into the template
            return String.format(TEMPLATE,
                    stars,
                    feedbackText,
                    userSection,
                    deviceInfo,
                    escapeHtml(feedback.getTimestamp())
            );
        } catch (Exception e) {
            Log.e("FeedbackEmailTemplate", "Error creating email content", e);
            return "Error generating email content: " + e.getMessage();
        }
    }

    private static String generateStarRating(float rating) {
        StringBuilder stars = new StringBuilder();
        int fullStars = (int) rating;
        boolean hasHalfStar = rating % 1 != 0;

        // Add filled stars
        for (int i = 0; i < fullStars; i++) {
            stars.append("★");
        }

        // Add half star if needed
        if (hasHalfStar) {
            stars.append("✬");
        }

        // Add empty stars
        int emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);
        for (int i = 0; i < emptyStars; i++) {
            stars.append("☆");
        }

        stars.append(String.format(" (%.1f/5)", rating));
        return stars.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }
}