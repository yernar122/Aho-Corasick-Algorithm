import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

public final class AhoCorasick {
    public record Match(int patternId, int startIndex) {}

    private static final class Node {
        final int[] next;
        final List<Integer> endingPatterns = new ArrayList<>();
        int failure = 0;
        int outputLink = -1;

        Node(int alphabetSize) {
            next = new int[alphabetSize];
            Arrays.fill(next, -1);
        }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final Map<Character, Integer> alphabet = new LinkedHashMap<>();
    private final int[] patternLengths;

    public AhoCorasick(List<String> patterns) {
        Objects.requireNonNull(patterns, "patterns must not be null");
        patternLengths = new int[patterns.size()];

        for (int i = 0; i < patterns.size(); i++) {
            String pattern = Objects.requireNonNull(patterns.get(i), "pattern must not be null");
            if (pattern.isEmpty()) {
                throw new IllegalArgumentException("Empty patterns are not supported");
            }
            patternLengths[i] = pattern.length();
            for (int j = 0; j < pattern.length(); j++) {
                char c = pattern.charAt(j);
                if (!alphabet.containsKey(c)) {
                    alphabet.put(c, alphabet.size());
                }
            }
        }

        nodes.add(new Node(alphabet.size())); // root = state 0
        for (int id = 0; id < patterns.size(); id++) {
            addPattern(patterns.get(id), id);
        }
        buildFailureAndOutputLinks();
    }

    private void addPattern(String pattern, int id) {
        int state = 0;
        for (int j = 0; j < pattern.length(); j++) {
            int c = alphabet.get(pattern.charAt(j));
            int child = nodes.get(state).next[c];
            if (child == -1) {
                child = nodes.size();
                nodes.get(state).next[c] = child;
                nodes.add(new Node(alphabet.size()));
            }
            state = child;
        }

        nodes.get(state).endingPatterns.add(id);
    }

    private void buildFailureAndOutputLinks() {
        Queue<Integer> queue = new ArrayDeque<>();
        Node root = nodes.get(0);

        for (int c = 0; c < alphabet.size(); c++) {
            int child = root.next[c];
            if (child == -1) {
                root.next[c] = 0;
            } else {
                nodes.get(child).failure = 0;
                queue.add(child);
            }
        }

        while (!queue.isEmpty()) {
            int state = queue.remove();
            Node node = nodes.get(state);
            Node failNode = nodes.get(node.failure);

            for (int c = 0; c < alphabet.size(); c++) {
                int child = node.next[c];
                if (child == -1) {
                    node.next[c] = failNode.next[c];
                } else {
                    Node childNode = nodes.get(child);
                    childNode.failure = failNode.next[c];
                    Node failTarget = nodes.get(childNode.failure);
                    childNode.outputLink = !failTarget.endingPatterns.isEmpty()
                            ? childNode.failure : failTarget.outputLink;
                    queue.add(child);
                }
            }
        }
    }

    public List<Match> search(String text) {
        Objects.requireNonNull(text, "text must not be null");
        List<Match> matches = new ArrayList<>();
        int state = 0;
        for (int i = 0; i < text.length(); i++) {
            Integer c = alphabet.get(text.charAt(i));
            state = c == null ? 0 : nodes.get(state).next[c];

            int outputState = state;
            while (outputState != -1) {
                Node outputNode = nodes.get(outputState);
                for (int id : outputNode.endingPatterns) {
                    matches.add(new Match(id, i - patternLengths[id] + 1));
                }
                outputState = outputNode.outputLink;
            }
        }
        return matches;
    }

    public int numberOfStates() {
        return nodes.size();
    }

    public int alphabetSize() {
        return alphabet.size();
    }
}
