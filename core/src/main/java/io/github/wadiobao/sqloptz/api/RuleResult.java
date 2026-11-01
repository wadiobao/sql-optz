package io.github.wadiobao.sqloptz.api;

import java.util.Objects;

/**
 * Outcome of evaluating one rule against one statement.
 *
 * @since 0.1.0
 */
public sealed interface RuleResult permits RuleResult.Pass, RuleResult.Report, RuleResult.Block {

    /** The shared {@link Pass} instance. */
    RuleResult PASS = new Pass();

    /** Returns the result meaning "nothing to report". */
    static RuleResult pass() {
        return PASS;
    }

    /**
     * Returns a result that reports a finding without blocking.
     *
     * @param severity   how serious the finding is
     * @param message    short description; must not contain literal values from the
     *                   SQL
     * @param suggestion suggested fix, or {@code null}
     */
    static RuleResult report(Severity severity, String message, String suggestion) {
        return new Report(severity, message, suggestion);
    }

    /**
     * Returns a result that asks for the statement to be blocked. The statement is
     * blocked only
     * when the rule runs in {@link Mode#ENFORCE}; in other modes the request is
     * reported as a
     * finding.
     *
     * @param message short description; must not contain literal values from the
     *                SQL
     */
    static RuleResult block(String message) {
        return new Block(message);
    }

    /** Nothing to report. */
    record Pass() implements RuleResult {
    }

    /**
     * A finding that does not block.
     *
     * @param severity   how serious it is; never {@code null}
     * @param message    description; never {@code null}
     * @param suggestion suggested fix, or {@code null}
     */
    record Report(Severity severity, String message, String suggestion) implements RuleResult {
        public Report {
            Objects.requireNonNull(severity, "severity");
            Objects.requireNonNull(message, "message");
        }
    }

    /**
     * A request to block the statement.
     *
     * @param message description; never {@code null}
     */
    record Block(String message) implements RuleResult {
        public Block {
            Objects.requireNonNull(message, "message");
        }
    }
}
