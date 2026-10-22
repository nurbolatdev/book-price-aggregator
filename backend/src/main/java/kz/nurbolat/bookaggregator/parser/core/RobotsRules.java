package kz.nurbolat.bookaggregator.parser.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Allow / Disallow rules of one robots.txt for our user agent (RFC 9309): the group naming our
 * product token, otherwise the {@code *} group, otherwise nothing is restricted. The longest
 * matching rule wins; on a tie Allow wins. Patterns support {@code *} and a trailing {@code $}.
 */
record RobotsRules(List<Rule> rules) {

    static final RobotsRules ALLOW_ALL = new RobotsRules(List.of());
    static final RobotsRules DISALLOW_ALL = new RobotsRules(List.of(new Rule(false, "/", Pattern.compile("/.*"))));

    record Rule(boolean allow, String path, Pattern pattern) {
    }

    /**
     * @param pathAndQuery URL path with the query string, e.g. {@code /search?search=abc}
     */
    boolean allows(String pathAndQuery) {
        Rule best = null;
        for (Rule rule : rules) {
            if (!rule.pattern().matcher(pathAndQuery).matches()) {
                continue;
            }
            if (best == null || rule.path().length() > best.path().length()
                    || (rule.path().length() == best.path().length() && rule.allow())) {
                best = rule;
            }
        }
        return best == null || best.allow();
    }

    static RobotsRules parse(String content, String productToken) {
        String token = productToken.toLowerCase(Locale.ROOT);
        List<Rule> ours = new ArrayList<>();
        List<Rule> wildcard = new ArrayList<>();
        boolean oursFound = false;
        boolean wildcardFound = false;

        List<String> groupAgents = new ArrayList<>();
        boolean readingAgents = false;
        for (String rawLine : content.split("\\R")) {
            String line = stripComment(rawLine).strip();
            int colon = line.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String field = line.substring(0, colon).strip().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).strip();
            if (field.equals("user-agent")) {
                if (!readingAgents) {
                    groupAgents.clear();
                }
                String agent = value.toLowerCase(Locale.ROOT);
                groupAgents.add(agent);
                readingAgents = true;
                oursFound |= isOurs(agent, token);
                wildcardFound |= agent.equals("*");
                continue;
            }
            readingAgents = false;
            if (!field.equals("allow") && !field.equals("disallow")) {
                continue;
            }
            if (value.isEmpty()) {
                // "Disallow:" with no path allows everything
                continue;
            }
            Rule rule = new Rule(field.equals("allow"), value, toPattern(value));
            if (groupAgents.stream().anyMatch(agent -> isOurs(agent, token))) {
                ours.add(rule);
            } else if (groupAgents.contains("*")) {
                wildcard.add(rule);
            }
        }
        if (oursFound) {
            return new RobotsRules(List.copyOf(ours));
        }
        return wildcardFound ? new RobotsRules(List.copyOf(wildcard)) : ALLOW_ALL;
    }

    // "KitapRadarBot" matches the product token "kitapradarbot" (case-insensitive prefix match)
    private static boolean isOurs(String agent, String token) {
        return !agent.isEmpty() && !agent.equals("*") && token.startsWith(agent);
    }

    private static Pattern toPattern(String path) {
        boolean anchored = path.endsWith("$");
        String body = anchored ? path.substring(0, path.length() - 1) : path;
        StringBuilder regex = new StringBuilder();
        for (String part : body.split("\\*", -1)) {
            if (!regex.isEmpty()) {
                regex.append(".*");
            }
            regex.append(Pattern.quote(part));
        }
        return Pattern.compile(regex + (anchored ? "" : ".*"), Pattern.DOTALL);
    }

    private static String stripComment(String line) {
        int hash = line.indexOf('#');
        return hash < 0 ? line : line.substring(0, hash);
    }
}
