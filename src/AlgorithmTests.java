import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public final class AlgorithmTests {
    private static int checks = 0;
    private static final Comparator<AhoCorasick.Match> ORDER =
            Comparator.comparingInt(AhoCorasick.Match::patternId)
                      .thenComparingInt(AhoCorasick.Match::startIndex);

    public static void main(String[] args) {
        check("classic example", "ahishers", List.of("he", "she", "his", "hers"),
                List.of(new AhoCorasick.Match(0, 4), new AhoCorasick.Match(1, 3),
                        new AhoCorasick.Match(2, 1), new AhoCorasick.Match(3, 4)));
        check("overlapping occurrences", "ababa", List.of("aba", "ba"),
                List.of(new AhoCorasick.Match(0, 0), new AhoCorasick.Match(0, 2),
                        new AhoCorasick.Match(1, 1), new AhoCorasick.Match(1, 3)));
        check("suffix output links", "aaaa", List.of("a", "aa", "aaa"),
                List.of(new AhoCorasick.Match(0, 0), new AhoCorasick.Match(0, 1),
                        new AhoCorasick.Match(0, 2), new AhoCorasick.Match(0, 3),
                        new AhoCorasick.Match(1, 0), new AhoCorasick.Match(1, 1),
                        new AhoCorasick.Match(1, 2), new AhoCorasick.Match(2, 0),
                        new AhoCorasick.Match(2, 1)));
        check("duplicate patterns", "banana", List.of("ana", "ana"),
                List.of(new AhoCorasick.Match(0, 1), new AhoCorasick.Match(0, 3),
                        new AhoCorasick.Match(1, 1), new AhoCorasick.Match(1, 3)));
        check("case sensitive", "Hello hello", List.of("Hello", "hello"),
                List.of(new AhoCorasick.Match(0, 0), new AhoCorasick.Match(1, 6)));
        check("Unicode", "a東京🙂東京", List.of("東京", "🙂"),
                List.of(new AhoCorasick.Match(0, 1), new AhoCorasick.Match(0, 5),
                        new AhoCorasick.Match(1, 3)));
        check("no matches", "abcdefgh", List.of("xyz"), List.of());
        check("empty text", "", List.of("a"), List.of());
        check("no patterns", "hello", List.of(), List.of());
        expectFailure("empty pattern rejected", () -> new AhoCorasick(List.of("")),
                IllegalArgumentException.class);
        expectFailure("KMP empty pattern rejected", () -> KMP.searchAll("abc", List.of("")),
                IllegalArgumentException.class);

        Random random = new Random(123456789L);
        for (int run = 0; run < 100; run++) {
            String text = randomWord(random, random.nextInt(60), "abc");
            List<String> patterns = new ArrayList<>();
            for (int p = 0; p < 1 + random.nextInt(12); p++) {
                patterns.add(randomWord(random, 1 + random.nextInt(5), "abc"));
            }
            check("random trial " + run, text, patterns, bruteForce(text, patterns));
        }
        System.out.println("PASS: " + checks + " correctness checks passed.");
    }

    private static String randomWord(Random random, int length, String alphabet) {
        StringBuilder out = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            out.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return out.toString();
    }

    private static List<AhoCorasick.Match> bruteForce(String text, List<String> patterns) {
        List<AhoCorasick.Match> matches = new ArrayList<>();
        for (int id = 0; id < patterns.size(); id++) {
            String pattern = patterns.get(id);
            for (int start = 0; start + pattern.length() <= text.length(); start++) {
                if (text.startsWith(pattern, start)) {
                    matches.add(new AhoCorasick.Match(id, start));
                }
            }
        }
        return matches;
    }

    private static void check(String name, String text, List<String> patterns,
                              List<AhoCorasick.Match> expected) {
        List<AhoCorasick.Match> sortedExpected = sorted(expected);
        List<AhoCorasick.Match> aho = sorted(new AhoCorasick(patterns).search(text));
        List<AhoCorasick.Match> kmp = sorted(KMP.searchAll(text, patterns));
        if (!sortedExpected.equals(aho)) {
            throw new AssertionError(name + ": Aho-Corasick expected " + sortedExpected + ", got " + aho);
        }
        if (!sortedExpected.equals(kmp)) {
            throw new AssertionError(name + ": KMP expected " + sortedExpected + ", got " + kmp);
        }
        checks += 2;
    }

    private static List<AhoCorasick.Match> sorted(List<AhoCorasick.Match> values) {
        List<AhoCorasick.Match> copy = new ArrayList<>(values);
        copy.sort(ORDER);
        return copy;
    }

    private static void expectFailure(String name, Runnable action,
                                      Class<? extends Throwable> type) {
        try {
            action.run();
        } catch (Throwable ex) {
            if (type.isInstance(ex)) {
                checks++;
                return;
            }
            throw new AssertionError(name + ": unexpected exception " + ex, ex);
        }
        throw new AssertionError(name + ": expected " + type.getSimpleName());
    }
}
