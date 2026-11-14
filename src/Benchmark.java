import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class Benchmark {
    private static final int[] PATTERN_COUNTS = {10, 100, 1_000};
    private static final int[] TEXT_LENGTHS = {1_000, 10_000, 100_000};
    private static final int REPETITIONS = 5;
    private static final int WARMUPS = 5;
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final Comparator<AhoCorasick.Match> ORDER =
            Comparator.comparingInt(AhoCorasick.Match::patternId)
                    .thenComparingInt(AhoCorasick.Match::startIndex);

    private record TimedResult(long nanos, List<AhoCorasick.Match> matches) {}

    public static void main(String[] args) throws IOException {
        Path rawCsv = Path.of(args.length > 0 ? args[0] : "results.csv");
        Path summaryCsv = Path.of(args.length > 1 ? args[1] : "results_summary.csv");
        StringBuilder raw = new StringBuilder(
                "patterns,text_length,run,aho_ms,kmp_ms,aho_matches,kmp_matches\n");
        StringBuilder summary = new StringBuilder(
                "patterns,text_length,aho_average_ms,kmp_average_ms,kmp_over_aho,match_count\n");

        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("Timer: System.nanoTime(); warmups=" + WARMUPS
                + "; measured runs=" + REPETITIONS);
        System.out.println("Both timings include preprocessing + searching, not data generation.");
        System.out.printf("%9s %12s %14s %14s %12s %10s%n",
                "patterns", "text length", "AC avg (ms)", "KMP avg (ms)", "KMP / AC", "matches");

        for (int count : PATTERN_COUNTS) {
            List<String> patterns = generatePatterns(count);
            for (int length : TEXT_LENGTHS) {
                String text = generateText(length, patterns);

                for (int w = 0; w < WARMUPS; w++) {
                    // Warm up Java methods/JIT, without recording these runs.
                    assertSame(runAho(text, patterns).matches(), runKmp(text, patterns).matches());
                }

                double totalAhoMs = 0;
                double totalKmpMs = 0;
                int matchedCount = -1;
                for (int run = 1; run <= REPETITIONS; run++) {
                    TimedResult aho;
                    TimedResult kmp;
                    if ((run & 1) == 1) {
                        aho = runAho(text, patterns);
                        kmp = runKmp(text, patterns);
                    } else {
                        kmp = runKmp(text, patterns);
                        aho = runAho(text, patterns);
                    }
                    assertSame(aho.matches(), kmp.matches());
                    matchedCount = aho.matches().size();
                    double ahoMs = aho.nanos() / 1_000_000.0;
                    double kmpMs = kmp.nanos() / 1_000_000.0;
                    totalAhoMs += ahoMs;
                    totalKmpMs += kmpMs;
                    raw.append(String.format(Locale.US, "%d,%d,%d,%.6f,%.6f,%d,%d%n",
                            count, length, run, ahoMs, kmpMs,
                            aho.matches().size(), kmp.matches().size()));
                }

                double ahoAverage = totalAhoMs / REPETITIONS;
                double kmpAverage = totalKmpMs / REPETITIONS;
                double speedRatio = kmpAverage / ahoAverage;
                summary.append(String.format(Locale.US, "%d,%d,%.6f,%.6f,%.3f,%d%n",
                        count, length, ahoAverage, kmpAverage, speedRatio, matchedCount));
                System.out.printf(Locale.US, "%9d %12d %14.3f %14.3f %12.2f %10d%n",
                        count, length, ahoAverage, kmpAverage, speedRatio, matchedCount);
            }
        }
        Files.writeString(rawCsv, raw.toString(), StandardCharsets.UTF_8);
        Files.writeString(summaryCsv, summary.toString(), StandardCharsets.UTF_8);
        System.out.println("Saved: " + rawCsv + ", " + summaryCsv);
    }

    private static TimedResult runAho(String text, List<String> patterns) {
        long start = System.nanoTime();
        AhoCorasick machine = new AhoCorasick(patterns);
        List<AhoCorasick.Match> matches = machine.search(text);
        return new TimedResult(System.nanoTime() - start, matches);
    }

    private static TimedResult runKmp(String text, List<String> patterns) {
        long start = System.nanoTime();
        List<AhoCorasick.Match> matches = KMP.searchAll(text, patterns);
        return new TimedResult(System.nanoTime() - start, matches);
    }

    private static List<String> generatePatterns(int count) {
        Random random = new Random(17029L + count);
        List<String> result = new ArrayList<>();
        Set<String> unique = new HashSet<>();
        while (result.size() < count) {
            String word = randomString(random, 5 + random.nextInt(6));
            if (unique.add(word)) {
                result.add(word);
            }
        }
        return result;
    }

    private static String generateText(int length, List<String> patterns) {
        Random random = new Random(5_000_000L + length);
        char[] chars = randomString(random, length).toCharArray();
        int step = Math.max(16, length / 40);
        // Insert known patterns at non-overlapping positions, ensuring real matches.
        for (int k = 0; k < 40; k++) {
            String pattern = patterns.get((k * 37) % patterns.size());
            int start = 5 + k * step;
            if (start + pattern.length() > chars.length) {
                break;
            }
            pattern.getChars(0, pattern.length(), chars, start);
        }
        return new String(chars);
    }

    private static String randomString(Random random, int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return builder.toString();
    }

    private static void assertSame(List<AhoCorasick.Match> aho,
                                   List<AhoCorasick.Match> kmp) {
        List<AhoCorasick.Match> sortedAho = new ArrayList<>(aho);
        List<AhoCorasick.Match> sortedKmp = new ArrayList<>(kmp);
        sortedAho.sort(ORDER);
        sortedKmp.sort(ORDER);
        if (!sortedAho.equals(sortedKmp)) {
            throw new AssertionError("AC and KMP produced different occurrences: "
                    + sortedAho + " vs " + sortedKmp);
        }
    }
}
