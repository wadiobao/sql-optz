package io.github.wadiobao.sqloptz.engine;

import io.github.wadiobao.sqloptz.api.Mode;
import io.github.wadiobao.sqloptz.api.SqlRule;
import java.util.Map;
import java.util.Objects;

/**
 * Decides which {@link Mode} applies to a rule: a per-rule override if there is
 * one, otherwise
 * the default.
 */
public final class ModeResolver {

    private final Mode defaultMode;
    private final Map<String, Mode> overrides;

    /**
     * Creates a resolver without checking that the overrides name real rules.
     * Prefer
     * {@link #of(Mode, Map, RuleRegistry)} outside tests.
     *
     * @param defaultMode mode for rules without an override; never {@code null}
     * @param overrides   mode per rule identifier; never {@code null}
     */
    public ModeResolver(Mode defaultMode, Map<String, Mode> overrides) {
        this.defaultMode = Objects.requireNonNull(defaultMode, "defaultMode");
        this.overrides = Map.copyOf(overrides);
    }

    /**
     * Creates a resolver and verifies that every override refers to a registered
     * rule.
     *
     * @throws IllegalArgumentException if an override names an unknown rule
     */
    public static ModeResolver of(Mode defaultMode, Map<String, Mode> overrides, RuleRegistry registry) {
        registry.requireKnown(overrides.keySet());
        return new ModeResolver(defaultMode, overrides);
    }

    /**
     * Returns the mode that applies to {@code rule}.
     *
     * @param rule the rule; never {@code null}
     * @return never {@code null}
     */
    public Mode modeOf(SqlRule rule) {
        return overrides.getOrDefault(rule.id(), defaultMode);
    }
}
