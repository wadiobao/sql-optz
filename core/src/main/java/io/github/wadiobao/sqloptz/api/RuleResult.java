package io.github.wadiobao.sqloptz.api;

import java.util.Objects;

/**
 * Outcome of running one rule against one statement.
 *
 * <p>
 * A rule only <em>detects</em>. What happens next depends on the {@link Mode}
 * the rule runs
 * in; see {@link Mode}.
 *
 * @since 0.1.0
 */
public sealed interface RuleResult permits RuleResult.Pass, RuleResult.Detected {

    /** The shared {@link Pass} instance. */
    RuleResult PASS = new Pass();

    /**
     * Returns the result meaning "nothing to report".
     *
     * @return the shared instance
     */
    static RuleResult pass() {
        return PASS;
    }

    /**
     * Returns a result that reports a problem without asking for the statement to
     * be blocked.
     *
     * @param severity   how serious the problem is; never {@code null}
     * @param message    short description; must not contain literal values from the
     *                   SQL text
     * @param suggestion suggested fix, or {@code null}
     * @return the result
     */
    static RuleResult report(Severity severity, String message, String suggestion) {
        return new Detected(severity, message, suggestion, false);
    }

    /**
     * Returns a result that reports an {@link Severity#ERROR} and asks for the
     * statement to be
     * blocked. The statement is only blocked when the rule runs in
     * {@link Mode#ENFORCE}.
     *
     * @param message short description; must not contain literal values from the
     *                SQL text
     * @return the result
     */
    static RuleResult block(String message) {
        return block(message, null);
    }

    /**
     * Same as {@link #block(String)}, with a suggested fix.
     *
     * @param message    short description; must not contain literal values from the
     *                   SQL text
     * @param suggestion suggested fix, or {@code null}
     * @return the result
     */
    static RuleResult block(String message, String suggestion) {
        return new Detected(Severity.ERROR, message, suggestion, true);
    }

    /** Nothing to report. */
    record Pass() implements RuleResult {
    }

    /**
     * A problem was detected.
     *
     * @param severity   how serious it is; never {@code null}
     * @param message    description; never {@code null}
     * @param suggestion suggested fix, or {@code null}
     * @param blocking   {@code true} if the rule asks for the statement to be
     *                   blocked
     */
    record Detected(Severity severity, String message, String suggestion, boolean blocking)
            implements RuleResult {
        public Detected {
            Objects.requireNonNull(severity, "severity");
            Objects.requireNonNull(message, "message");
        }
    }
}
