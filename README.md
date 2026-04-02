# MusicBot — Playtech Internship 2026

A Java bidding bot for the Attention Economy auction assignment. The bot
speaks the harness stdin/stdout protocol, selects the **Music** category,
and places bids from a single budget of ebucks passed on the command line.

---

## Contents

| Path | Role |
|------|------|
| `src/MusicBot.java` | Main loop: category declaration, bid per auction, budget and efficiency tracking |
| `src/AuctionInfo.java` | Parses `key=value` auction lines into typed fields with fallback scraping |
| `src/ValueEstimator.java` | Heuristic scoring and category tier filtering |
| `src/META-INF/MANIFEST.MF` | `Main-Class` entry for JAR packaging |

---

## Strategy

### Category choice — Music
Music has broad cross-demographic appeal and frequent appearance in viewer
interest lists, giving the bot more bidding opportunities than niche
categories. Competing bots that also pick Music must be outbid on quality,
not just volume.

### Two-stage filtering
Before scoring, `ValueEstimator.categoryTier()` gates each auction:

- **Tier 2** (Music video): eligible for a real bid
- **Tier 1** (adjacent category — ASMR, Kids, Video Games, Beauty): skipped
- **Tier 0** (unrelated category): skipped

Skipped rounds receive a 1-ebuck minimum bid to satisfy the protocol
requirement to respond, at essentially zero cost.

### Scoring signals
Eligible auctions are scored 0–1 using signals the spec hints at directly:

| Signal | Weight | Reasoning |
|--------|--------|-----------|
| Video category match | 0.52 | Spec: matching category drives engagement |
| Viewer subscribed | 0.16 | Spec: subscriptions result in higher value |
| Viewer interests (ordered) | 0.04–0.16 | Spec: interests ordered by relevance; primary worth more |
| Engagement ratio (comments/views) | up to 0.24 | Spec: higher ratio suggests higher engagement |
| Age bracket | 0.02–0.10 | 18–34 most valuable for music advertising |
| View count band | up to 0.09 | Bell curve around log₁₀ ≈ 5 (100k views); avoids chasing viral outliers |
| Gender | 0.02 | Small signal only |

View count is treated as a **non-monotonic bell curve** rather than a
linear signal, consistent with the spec footnote warning that the
relationship between view count and value is not monotonic.

### Bid sizing
Bid size is a cubic function of the auction score:

```
frac = 0.000055 + 0.00035 * q² + 0.00105 * q³
maxBid = usable_budget * frac * spendPressure * efficiencyFactor
```

This means low-scoring auctions cost almost nothing while high-scoring
auctions receive meaningfully larger bids without overcommitting.

### Budget pacing
`spendPressureMusic()` uses an exponential curve to target natural spend
pacing across the full simulation. If actual spend falls behind the target
curve, a small multiplier (1.04–1.08) nudges bids upward to avoid
elimination for failing to spend.

### Adaptive efficiency
Every 100-round summary updates a smoothed efficiency estimate
(`currentEfficiency`). `efficiencyTieBreak()` reduces bid size when
efficiency is low (we are overpaying per point) and restores it when
efficiency recovers — a simple feedback control loop.

---

## Requirements

- JDK up to Java 25 (standard library only — no Spring, Lombok, etc.)
- One program argument: total ebucks, e.g. `10000000`
- Heap: suitable for the 192 MB limit in the test environment
- No additional threads, no file I/O beyond stdin/stdout/stderr

---

## Build

From the `src` directory:

```bash
javac -encoding UTF-8 MusicBot.java AuctionInfo.java ValueEstimator.java
jar cfm MusicBot.jar META-INF/MANIFEST.MF *.class
```

---

## Run

```bash
java -jar MusicBot.jar 10000000
```

The process prints the chosen category on the first stdout line, then
responds with one bid (`<start> <max>`) per auction line read from stdin.

---

## Submission note

Submit source code only — either this repository or a zip of the `src`
directory. Compiled `.class` files, `.jar` artifacts and IDE output
directories are excluded via `.gitignore` and should be regenerated
locally.