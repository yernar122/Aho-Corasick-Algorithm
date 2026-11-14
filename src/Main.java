import java.util.List;

public final class Main {
    public static void main(String[] args) {
        List<String> patterns = List.of("he", "she", "his", "hers");
        String text = "ahishers";
        AhoCorasick matcher = new AhoCorasick(patterns);

        System.out.println("Text: " + text);
        System.out.println("Patterns: " + patterns);
        for (AhoCorasick.Match match : matcher.search(text)) {
            System.out.printf("Found '%s' at index %d%n",
                    patterns.get(match.patternId()), match.startIndex());
        }
    }
}
