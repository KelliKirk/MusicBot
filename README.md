# Playtech Summer Internship 2026 — Test Assignment

This repository contains the **Attention Bid** solution: a small Java process (`MusicBot`) that speaks the harness **stdin/stdout** protocol, selects the **Music** category, and places **start / max** bids from a single budget of **ebucks** passed on the command line.

The brief prioritises **correct protocol behaviour** and **clean, maintainable source code**. Third-party libraries, extra threads, file I/O (beyond standard streams), and sockets are out of scope for the assignment rules we followed.

## Contents

| Path | Role |
|------|------|
| `src/MusicBot.java` | Main loop: category line, bid per auction, budget updates |
| `src/AuctionInfo.java` | Parses `key=value` auction lines into fields |
| `src/ValueEstimator.java` | Heuristic score for bidding (visible PDF-style fields only) |
| `src/META-INF/MANIFEST.MF` | `Main-Class` entry for optional JAR packaging (local / harness use) |

## Requirements

- **JDK** up to Java 25 (stdlib only; no Spring, Lombok, etc.).
- **One program argument:** total ebucks, e.g. `10000000`.
- **Memory:** suitable for a **192MB** heap limit in the test environment.
- **No** additional threads; **no** reading/writing arbitrary files (stdin/stdout/stderr only).

## Build

From the `src` directory:

```bash
javac -encoding UTF-8 MusicBot.java AuctionInfo.java ValueEstimator.java
```

## Run

```bash
java MusicBot 10000000
```

The process prints the category on the first line of **stdout**, then one bid (`<start> <max>`) per auction line read from **stdin**, as required by the harness.

## Submission note

For Playtech, submit **source code** (e.g. this repository or a zip). **Do not** rely on committing **`.jar`** files or **`out/`** build trees; they are listed in `.gitignore` and are regenerated locally or by the harness/CI as needed.
