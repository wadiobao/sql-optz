package io.github.wadiobao.sqloptz.engine;

import io.github.wadiobao.sqloptz.api.SqlRule;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * The validated, ordered set of rules an engine runs.
 *
 * <p>
 * Guard rules always run before lint rules, so a statement that must be blocked
 * is stopped
 * before any advisory work is done. Within a category, rules run by
 * {@link SqlRule#order()} and
 * then by identifier, which makes the order deterministic.
 *
 * <p>
 * Invalid input fails immediately, at start-up, instead of misbehaving later.
 */
public final class RuleRegistry {

    private static final Pattern ID = Pattern.compile("[A-Z][A-Z0-9_]*");

    private final List<SqlRule> ordered;
    private final Set<String> ids;

    /**
     * Creates a registry.
     *
     * @param rules the rules to register; never {@code null}
     * @throws NullPointerException     if a rule, an identifier or a category is
     *                                  {@code null}
     * @throws IllegalArgumentException if an identifier is malformed or used twice
     */
    public RuleRegistry(Collection<? extends SqlRule> rules) {
        Objects.requireNonNull(rules, "rules");
        Set<String> seen = new HashSet<>();
        for (SqlRule rule : rules) {
            Objects.requireNonNull(rule, "rule");
            String id = Objects.requireNonNull(rule.id(), "rule id");
            Objects.requireNonNull(rule.category(), "category of rule " + id);
            if (!ID.matcher(id).matches()) {
                throw new IllegalArgumentException(
                        "Invalid rule id '" + id + "': use UPPER_SNAKE_CASE, for example GUARD_READ_ONLY");
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("Duplicate rule id: " + id);
            }
        }
        // RuleCategory declares GUARD before LINT, so natural enum order puts guards
        // first.
        this.ordered = rules.stream()
                .sorted(Comparator.comparing((SqlRule r) -> r.category().ordinal())
                        .thenComparingInt(SqlRule::order)
                        .thenComparing(SqlRule::id))
                .map(r -> (SqlRule) r)
                .toList();
        this.ids = Set.copyOf(seen);
    }

    /**
     * Returns the rules in the order they run.
     *
     * @return an unmodifiable list
     */
    public List<SqlRule> rules() {
        return ordered;
    }

    /**
     * Checks that every identifier refers to a registered rule. Use it to reject
     * configuration
     * that mentions a rule which does not exist, usually a typo that would
     * otherwise silently do
     * nothing.
     *
     * @param ruleIds identifiers taken from configuration
     * @throws IllegalArgumentException naming the unknown identifiers and the known
     *                                  ones
     */
    public void requireKnown(Set<String> ruleIds) {
        Set<String> unknown = new TreeSet<>(ruleIds);
        unknown.removeAll(ids);
        if (!unknown.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown rule id(s) in configuration: " + unknown + ". Known rules: " + new TreeSet<>(ids));
        }
    }
}
