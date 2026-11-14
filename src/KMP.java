import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KMP {
    private KMP() {}

    public static List<AhoCorasick.Match> searchAll(String text, List<String> patterns) {
        Objects.requireNonNull(text, "text must not be null");
        Objects.requireNonNull(patterns, "patterns must not be null");
        List<AhoCorasick.Match> results = new ArrayList<>();
        for (int id = 0; id < patterns.size(); id++) {
            String pattern = Objects.requireNonNull(patterns.get(id), "pattern must not be null");
            if (pattern.isEmpty()) {
                throw new IllegalArgumentException("Empty patterns are not supported");
            }
            int[] lps = buildLps(pattern);
            int i = 0;
            int j = 0;
            while (i < text.length()) {
                if (text.charAt(i) == pattern.charAt(j)) {
                    i++;
                    j++;
                    if (j == pattern.length()) {
                        results.add(new AhoCorasick.Match(id, i - j));
                        j = lps[j - 1];
                    }
                } else if (j > 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        return results;
    }

    private static int[] buildLps(String pattern) {
        int[] lps = new int[pattern.length()];
        int len = 0;
        for (int i = 1; i < pattern.length();) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                lps[i++] = ++len;
            } else if (len > 0) {
                len = lps[len - 1];
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }
}
