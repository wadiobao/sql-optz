package io.github.wadiobao.sqloptz.engine;

import io.github.wadiobao.sqloptz.api.Finding;
import io.github.wadiobao.sqloptz.api.SqlGuardViolationException;
import java.util.List;

/**
 * Result of evaluating one SQL text.
 *
 * @param findings  everything the rules reported, in order; unmodifiable
 * @param violation the exception to throw to block the statement, or
 *                  {@code null} to let it run
 */
public record Outcome(List<Finding> findings, SqlGuardViolationException violation) {

    public Outcome {
        findings = List.copyOf(findings);
    }

    public boolean allowed() {
        return violation == null;
    }
}
