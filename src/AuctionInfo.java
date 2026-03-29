import java.util.Locale;

public class AuctionInfo {

    String category = "";
    long viewCount = 0;
    long commentCount = 0;
    boolean subscribed = false;
    String age = "";
    String gender = "";

    String[] interests = new String[0];

    static AuctionInfo parse(String line) {
        AuctionInfo a = new AuctionInfo();
        final int n = line.length();
        int i = 0;
        while (i < n) {
            int eq = line.indexOf('=', i);
            if (eq < 0) {
                break;
            }
            int comma = indexOfCommaOrEnd(line, eq + 1, n);
            int ks = skipLeading(line, i, eq);
            int ke = skipTrailing(line, ks, eq);
            int vs = skipLeading(line, eq + 1, comma);
            int ve = skipTrailing(line, vs, comma);
            if (ke > ks && ve > vs) {
                assign(a, line, ks, ke, vs, ve);
            }
            i = comma < n ? comma + 1 : n;
        }
        scrapeFallback(a, line);
        return a;
    }

    private static void scrapeFallback(AuctionInfo a, String line) {
        if (a.category.isEmpty()) {
            String low = line.toLowerCase(Locale.ROOT);
            int cat = low.indexOf("video.category");
            if (cat >= 0) {
                int eq = line.indexOf('=', cat);
                if (eq > 0) {
                    int end = line.indexOf(',', eq + 1);
                    if (end < 0) {
                        end = line.length();
                    }
                    int vs = skipLeading(line, eq + 1, end);
                    int ve = skipTrailing(line, vs, end);
                    if (ve > vs) {
                        a.category = line.substring(vs, ve).trim();
                    }
                }
            }
        }
    }

    private static int indexOfCommaOrEnd(String line, int from, int n) {
        for (int p = from; p < n; p++) {
            if (line.charAt(p) == ',') {
                return p;
            }
        }
        return n;
    }

    private static int skipLeading(String s, int from, int to) {
        int p = from;
        while (p < to && s.charAt(p) == ' ') {
            p++;
        }
        return p;
    }

    private static int skipTrailing(String s, int from, int to) {
        int p = to;
        while (p > from && s.charAt(p - 1) == ' ') {
            p--;
        }
        return p;
    }

    private static void assign(AuctionInfo a, String line, int ks, int ke, int vs, int ve) {
        int klen = ke - ks;
        if (klen == 14 && line.regionMatches(true, ks, "video.category", 0, 14)) {
            a.category = line.substring(vs, ve).trim();
        } else if (klen == 15 && line.regionMatches(true, ks, "video.viewCount", 0, 15)) {
            a.viewCount = parseLong(line, vs, ve);
        } else if (klen == 18 && line.regionMatches(true, ks, "video.commentCount", 0, 18)) {
            a.commentCount = parseLong(line, vs, ve);
        } else if (klen == 17 && line.regionMatches(true, ks, "viewer.subscribed", 0, 17)) {
            char c = line.charAt(vs);
            a.subscribed = c == 'Y' || c == 'y';
        } else if (klen == 11 && line.regionMatches(true, ks, "viewer.age", 0, 11)) {
            a.age = line.substring(vs, ve);
        } else if (klen == 14 && line.regionMatches(true, ks, "viewer.gender", 0, 14)) {
            a.gender = line.substring(vs, ve);
        } else if (klen == 17 && line.regionMatches(true, ks, "viewer.interests", 0, 17)) {
            a.interests = splitInterests(line, vs, ve);
        }
    }

    private static String[] splitInterests(String line, int vs, int ve) {
        int count = 1;
        for (int p = vs; p < ve; p++) {
            if (line.charAt(p) == ';') {
                count++;
            }
        }
        String[] out = new String[count];
        int slot = 0;
        int seg = vs;
        for (int p = vs; p <= ve; p++) {
            if (p == ve || line.charAt(p) == ';') {
                int a = skipLeading(line, seg, p);
                int b = skipTrailing(line, a, p);
                out[slot++] = a < b ? line.substring(a, b) : "";
                seg = p + 1;
            }
        }
        return out;
    }

    static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long parseLong(String s, int from, int to) {
        long v = 0L;
        boolean neg = false;
        int i = skipLeading(s, from, to);
        int end = skipTrailing(s, i, to);
        if (i >= end) {
            return 0L;
        }
        if (s.charAt(i) == '-') {
            neg = true;
            i++;
        }
        for (; i < end; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return 0L;
            }
            v = v * 10L + (c - '0');
        }
        return neg ? -v : v;
    }
}
