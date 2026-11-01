package io.github.wadiobao.sqloptz.api;

/**
 * Determines how the engine treats a rule when a statement cannot be analysed.
 *
 * @since 0.1.0
 */
public enum RuleCategory {

    /**
     * A security rule. When running in {@link Mode#ENFORCE}, a statement that
     * cannot be
     * analysed is blocked (fail-closed).
     */
    GUARD,

    /**
     * An advisory rule about performance or style. When a statement cannot be
     * analysed the rule
     * is skipped and the statement continues (fail-open).
     */
    LINT
}
