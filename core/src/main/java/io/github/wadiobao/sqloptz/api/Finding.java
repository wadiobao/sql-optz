package io.github.wadiobao.sqloptz.api;

import java.time.Instant;
import java.util.Objects;

/**
 * A single observation produced by a rule.
 *
 * <p>Instances are immutable and safe to share between threads. They never contain parameter
 * values: {@link #fingerprint()} is the statement with every literal replaced by {@code ?}.
 *
 * @param ruleId       stable identifier of the rule, for example {@code "GUARD_READ_ONLY"};
 *                     never {@code null}
 * @param severity     how serious the finding is; never {@code null}
 * @param mode         the mode the rule ran in when it produced this finding; never
 *                     {@code null}
 * @param fingerprint  normalised statement text, never the original SQL; never {@code null}
 * @param message      short human-readable description; never {@code null}
 * @param suggestion   suggested fix, or {@code null} when the rule offers none
 * @param rewrittenSql the statement actually sent to the database, or {@code null} when the
 *                     statement was not rewritten
 * @param at           when the finding was produced; never {@code null}
 * @since 0.1.0
 */
public record Finding(
        String ruleId,
        Severity severity,
        Mode mode,
        String fingerprint,
        String message,
        String suggestion,
        String rewrittenSql,
        Instant at) {

    /**
     * Creates a finding.
     *
     * @throws NullPointerException if any of the arguments documented as never {@code null}
     *                              is {@code null}
     */
    public Finding {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(fingerprint, "fingerprint");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(at, "at");
    }
}