public final class ValueEstimator {

    private ValueEstimator() {
    }

    public static int categoryTier(AuctionInfo a, String ourCategory) {
        if (a.category == null || a.category.isEmpty()) {
            return 0;
        }
        String cat = a.category.trim();
        if (cat.equalsIgnoreCase(ourCategory)) {
            return 2;
        }
        if (isAdjacentCategory(cat)) {
            return 1;
        }
        return 0;
    }

    public static double score(AuctionInfo a, String ourCategory) {
        double s = 0.0;

        s += scoreVideoCategory(a.category, ourCategory);
        s += scoreEngagement(a.viewCount, a.commentCount);
        if (a.subscribed) {
            s += 0.16;
        }
        s += scoreInterestsOrdered(a.interests, ourCategory);
        s += scoreAge(a.age);
        s += scoreViewBand(a.viewCount);
        if ("M".equals(a.gender)) {
            s += 0.02;
        }

        return clamp01(s);
    }

    private static double scoreVideoCategory(String videoCat, String ours) {
        if (videoCat == null) {
            return 0.075;
        }
        videoCat = videoCat.trim();
        if (videoCat.equalsIgnoreCase(ours)) {
            return 0.52;
        }
        if (isAdjacentCategory(videoCat)) {
            return 0.17;
        }
        return 0.075;
    }

    private static boolean isAdjacentCategory(String cat) {
        return "ASMR".equalsIgnoreCase(cat)
                || "Kids".equalsIgnoreCase(cat)
                || "Video Games".equalsIgnoreCase(cat)
                || "Beauty".equalsIgnoreCase(cat);
    }

    private static double scoreEngagement(long views, long comments) {
        if (views <= 0L) {
            return 0.0;
        }
        double r = (double) comments / (double) views;
        return 0.032 + Math.min(0.24, r * 3.6);
    }

    private static double scoreInterestsOrdered(String[] interests, String ours) {
        double w0 = 0.16;
        double w1 = 0.09;
        double w2 = 0.04;
        if (interests.length == 0) {
            return 0.035;
        }
        for (int i = 0; i < interests.length && i < 3; i++) {
            if (!interests[i].equalsIgnoreCase(ours)) {
                continue;
            }
            if (i == 0) {
                return w0;
            }
            if (i == 1) {
                return w1;
            }
            return w2;
        }
        return 0.0;
    }

    private static double scoreAge(String age) {
        if (age == null) {
            return 0.03;
        }
        switch (age) {
            case "18-24":
            case "25-34":
                return 0.10;
            case "35-44":
                return 0.06;
            case "13-17":
                return 0.05;
            default:
                return 0.02;
        }
    }

    private static double scoreViewBand(long v) {
        if (v <= 0L) {
            return 0.0;
        }
        double log = Math.log10((double) v + 1.0);
        double mid = 5.0;
        double spread = 3.0;
        double t = 1.0 - Math.min(1.0, Math.abs(log - mid) / spread);
        return 0.09 * t;
    }

    private static double clamp01(double x) {
        if (x < 0.0) {
            return 0.0;
        }
        if (x > 1.0) {
            return 1.0;
        }
        return x;
    }
}
