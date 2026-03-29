import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class MusicBot {

    static final String CATEGORY = "Music";

    // Budget management
    static long initialBudget;
    static long remaining;

    // Round tracking
    static int  roundNumber      = 0;
    static long totalPointsWon   = 0;
    static long totalEbucksSpent = 0;

    // Adaptive state (updated every 100 rounds via summary)
    // Tracks efficiency: points per ebuck. Used to adjust aggression.
    static double currentEfficiency     = 0.0;
    static double targetEfficiencyFloor = 0.3;

    // Bid sizing constants
    static final int    FLAT_LOW_BID    = 1;   // minimum bid for low-value rounds
    static final double HIGH_VALUE_FRAC = 0.002; // 0.2% of remaining per high-value round

    public static void main(String[] args) {
        initialBudget = (args.length > 0) ? Long.parseLong(args[0]) : 10_000_000L;
        remaining     = initialBudget;

        BufferedReader in  = new BufferedReader(new InputStreamReader(System.in));
        PrintStream    out = System.out;

        out.println(CATEGORY);
        out.flush();

        try {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("S ")) {
                    // Summary every 100 rounds and update adaptive state
                    handleSummary(line);

                } else if (line.startsWith("W ") || line.equals("L")) {
                    // Win/Loss result for the last round
                    handleResult(line);

                } else if (line.contains("video.") || line.contains("viewer.")) {
                    // New auction round
                    roundNumber++;
                    AuctionInfo auction = AuctionInfo.parse(line);

                    int[] bid = decideBid(auction);
                    out.println(bid[0] + " " + bid[1]);
                    out.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
        }
    }

    static int[] decideBid(AuctionInfo a) {
        double score = ValueEstimator.score(a, CATEGORY);

        // Dynamic threshold: starts at 0.55, adjusts based on performance
        double threshold = computeThreshold();

        if (score >= threshold) {
            // HIGH VALUE auction - bid meaningfully
            long maxBidLong = (long)(remaining * HIGH_VALUE_FRAC);
            int  maxBid     = (int) Math.max(2, Math.min(maxBidLong, remaining));

            // Safety: never drop below 2% of initial budget (elimination threshold)
            long safeFloor = (long)(initialBudget * 0.025);
            if (remaining - maxBid < safeFloor) {
                maxBid = (int) Math.max(1, remaining - safeFloor);
            }

            return new int[]{maxBid, maxBid};

        } else {
            // LOW VALUE auction - participate minimally
            return new int[]{FLAT_LOW_BID, FLAT_LOW_BID};
        }
    }

    static double computeThreshold() {
        // Base threshold - only bid on clearly above-average auctions
        double base = 0.55;

        if (roundNumber < 100) return base;

        if (currentEfficiency > targetEfficiencyFloor * 2) {
            return base + 0.10; // doing very well - be more selective
        } else if (currentEfficiency < targetEfficiencyFloor) {
            return base - 0.10; // underperforming - open up more
        }
        return base;
    }

    static void handleResult(String line) {
        if (line.startsWith("W ")) {
            int cost = Integer.parseInt(line.substring(2).trim());
            remaining        -= cost;
            totalEbucksSpent += cost;
            log("Round " + roundNumber + " WON cost=" + cost
                    + " remaining=" + remaining);
        } else {
            log("Round " + roundNumber + " LOST");
        }
    }

    static void handleSummary(String line) {
        // Format: S {points} {ebucks}
        String[] p = line.split("\\s+");
        if (p.length >= 3) {
            totalPointsWon   = Long.parseLong(p[1]);
            totalEbucksSpent = Long.parseLong(p[2]);

            // Recalculate efficiency
            if (totalEbucksSpent > 0) {
                currentEfficiency = (double) totalPointsWon / totalEbucksSpent;
            }

            log("=== SUMMARY round=" + roundNumber
                    + " points=" + totalPointsWon
                    + " spent=" + totalEbucksSpent
                    + " efficiency=" + String.format("%.4f", currentEfficiency)
                    + " remaining=" + remaining + " ===");
        }
    }

    static void log(String msg) {
        System.err.println("[MusicBot] " + msg);
    }
}