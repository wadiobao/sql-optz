package io.github.wadiobao.sqloptz.api;

import java.util.Objects;

/**
 * Read-only view of a statement handed to a {@link SqlRule}.
 *
 * <p>
 * The view is only built for statements that were parsed completely, so a rule
 * can rely on
 * {@link SqlStatementInfo#parsed()} being {@code true}. Statements that could
 * not be analysed
 * never reach a rule; the engine handles them (see {@link RuleCategory}).
 *
 * @param info        what is known about the statements in the text; never
 *                    {@code null}
 * @param statementId short identifier of the text, safe to log; it is a hash,
 *                    not the SQL
 * @since 0.1.0
 */
public record RuleContext(SqlStatementInfo info, String statementId) {
    public RuleContext {
        Objects.requireNonNull(info, "info");
        Objects.requireNonNull(statementId, "statementId");
    }
}
