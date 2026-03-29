import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public class MusicBot {

    static final String CATEGORY = "Music";

    static long initialBudget;
    static long remaining;

    static int roundNumber = 0;

    static double currentEfficiency = 0.35;
    static final double EFF_SMOOTH = 0.3;
    static final double targetEfficiencyFloor = 0.26;

    /** Tiny liquidity only — large reserves violate the “30% implicit spend” incentive. */
    static final double RESERVE_FRAC = 0.0008;
    static final int DEBUG_ROUNDS = 0;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java MusicBot <total_ebucks>");
            System.exit(1);
            return;
        }
        initialBudget = Long.parseLong(args[0]);
        if (initialBudget <= 0L) {
            System.err.println("Budget must be positive.");
            System.exit(1);
            return;
        }
        remaining = initialBudget;

        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);

        out.println(CATEGORY);
        out.flush();

        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = stripBom(line.trim());
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("S ") || line.startsWith("S\t")) {
                    handleSummary(line);
                } else if (line.startsWith("W") || line.equals("L") || line.startsWith("L ")) {
                    handleResult(line);
                } else if (looksLikeAuctionPayload(line)) {
                    roundNumber++;
                    AuctionInfo auction = AuctionInfo.parse(line);
                    int[] bid = decideBid(auction, line);
                    out.println(bid[0] + " " + bid[1]);
                    out.flush();
                    if (roundNumber <= DEBUG_ROUNDS) {
                        System.err.println("[DBG] r=" + roundNumber
                                + " cat='" + auction.category + "'"
                                + " q=" + ValueEstimator.score(auction, CATEGORY)
                                + " bid=" + bid[0]
                                + " rem=" + remaining);
                    }
                } else {
                    // Ignore non-auction, non-result lines to avoid protocol desync.
                    if (roundNumber < DEBUG_ROUNDS) {
                        System.err.println("[DBG] ignored: " + line);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    static String stripBom(String line) {
        if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
            return line.substring(1);
        }
        return line;
    }

    static boolean looksLikeAuctionPayload(String line) {
        if (line.indexOf('=') >= 0) {
            return true;
        }
        String lc = line.toLowerCase(Locale.ROOT);
        return lc.contains("video") || lc.contains("viewer");
    }

    static long reserveAmount() {
        long r = (long) (initialBudget * RESERVE_FRAC);
        return Math.max(1L, Math.min(r, remaining));
    }

    static int[] decideBid(AuctionInfo a, String rawLine) {
        long reserve = reserveAmount();
        if (remaining <= reserve + 1L) {
            return new int[]{1, 1};
        }
        long usable = remaining - reserve;

        int tier = ValueEstimator.categoryTier(a, CATEGORY);
        if (tier != 2) {
            return new int[]{1, 1};
        }

        double q = ValueEstimator.score(a, CATEGORY);
        if (!looksLikeAuctionPayload(rawLine)) {
            q *= 0.25;
        }
        q = Math.min(1.0, Math.max(0.0, q));
        if (a.interests.length > 0 && CATEGORY.equalsIgnoreCase(a.interests[0].trim())) {
            q = Math.min(1.0, q + 0.07);
        }

        double q2 = q * q;
        double q3 = q2 * q;
        double frac = 0.000055 + 0.00035 * q2 + 0.00105 * q3;
        long maxBid = (long) (usable * frac);

        maxBid = (long) (maxBid * spendPressureMusic() * efficiencyTieBreak());

        long perRoundCap = Math.min(usable / 14, initialBudget / 26);
        maxBid = Math.min(maxBid, perRoundCap);

        maxBid = Math.max(1L, Math.min(maxBid, usable));

        if (maxBid > Integer.MAX_VALUE) {
            maxBid = Integer.MAX_VALUE;
        }
        int b = (int) maxBid;
        return new int[]{b, b};
    }

    static double spendPressureMusic() {
        long spent = initialBudget - remaining;
        double targetFrac = 1.0 - Math.exp(-roundNumber / 4200.0);
        targetFrac = Math.min(0.9, Math.max(0.1, targetFrac));
        long targetSpend = (long) (initialBudget * targetFrac * 0.82);
        if (spent + initialBudget / 250L < targetSpend) {
            return 1.08;
        }
        if (spent + initialBudget / 120L < targetSpend) {
            return 1.04;
        }
        return 1.0;
    }

    static double efficiencyTieBreak() {
        if (currentEfficiency < 0.04) {
            return 0.82;
        }
        if (currentEfficiency < 0.12) {
            return 0.92;
        }
        if (currentEfficiency < targetEfficiencyFloor) {
            return 0.98;
        }
        return 1.0;
    }

    static void handleResult(String line) {
        if (line.startsWith("W")) {
            long cost = firstNumberAfterPrefix(line, 'W');
            if (cost > 0L) {
                remaining -= cost;
                if (remaining < 0L) {
                    remaining = 0L;
                }
            }
        }
    }

    static long firstNumberAfterPrefix(String s, char prefix) {
        int i = 0;
        while (i < s.length() && s.charAt(i) != prefix) {
            i++;
        }
        if (i >= s.length()) {
            return 0L;
        }
        // Move past prefix char
        i++;
        // Scan to first digit
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') {
                break;
            }
            i++;
        }
        long v = 0L;
        boolean saw = false;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                break;
            }
            v = v * 10L + (c - '0');
            saw = true;
            i++;
        }
        return saw ? v : 0L;
    }

    static void handleSummary(String line) {
        ArrayList<Long> nums = new ArrayList<>();
        for (String t : line.split("\\s+")) {
            if (t.isEmpty() || t.equals("S")) {
                continue;
            }
            try {
                nums.add(Long.parseLong(t));
            } catch (NumberFormatException ignored) {
            }
        }
        if (nums.size() < 2) {
            return;
        }
        long windowPoints = nums.get(nums.size() - 2);
        long windowSpent = nums.get(nums.size() - 1);
        if (windowSpent <= 0L) {
            currentEfficiency = 0.65 * currentEfficiency + 0.35 * 0.12;
            currentEfficiency = Math.max(0.0, Math.min(50.0, currentEfficiency));
            return;
        }
        double instant = (double) windowPoints / (double) windowSpent;
        if (Double.isFinite(instant) && instant >= 0.0) {
            currentEfficiency = EFF_SMOOTH * instant + (1.0 - EFF_SMOOTH) * currentEfficiency;
            currentEfficiency = Math.max(0.0, Math.min(50.0, currentEfficiency));
        }
    }
}
