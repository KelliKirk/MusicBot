public class ValueEstimator {
    public static double score(AuctionInfo a, String ourCategory) {
        double score = 0.0;

        // Category match
        if(a.category.equalsIgnoreCase(ourCategory)) {
            score += 0.35;
        }

        // Subscriber status
        if (a.unsubscribed) {
            score += 0.20;
        }

        // Engagement ratio
        if(a.viewCount > 0) {
            double ratio = (double) a.commentCount / a.viewCount;
            double engagementScore = Math.min(0.20, ratio * 4.0);
            score += engagementScore;
        }

        // Viewer interests ordered by relevance
        for (int i = 0; i < a.interests.length, i++) {
            if (a.interests[1].equalsIgnoreCase(ourCategory)) {
                if (i == 0) score += 0.10; // primary interest
                else if (i == 1) score += 0.04; // secondary
                else score += 0.01; // tertiary
                break;
            }
        }

        // Age bracket
        switch (a.age) {
            case "18-24": score += 0.10; break;
            case "25-34": score += 0.10; break;
            case "35-44": score += 0.05; break;
            case "13-17": score += 0.04; break;
            default: score += 0.0; break; // 45+ less valuable for music ads
        }

        return Math.min(1.0, Math.max(0.0, score));
    }
}
