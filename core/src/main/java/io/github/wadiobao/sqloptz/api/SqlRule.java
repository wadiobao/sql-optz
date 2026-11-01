package io.github.wadiobao.sqloptz.api;

/**
 * A check applied to every statement that passes through the proxy.
 *
 * <h2>Contract for implementors</h2>
 * <ul>
 * <li><b>Thread safety.</b> One instance is called concurrently from many
 * threads; it must be
 * stateless or thread-safe.</li>
 * <li><b>Read-only.</b> Do not modify anything reachable from the
 * {@link RuleContext}.</li>
 * <li><b>No side effects.</b> Do not touch the database, the network or the
 * connection.</li>
 * <li><b>No literals in messages.</b> Messages end up in logs; they must not
 * contain values
 * from the SQL text.</li>
 * <li><b>Exceptions.</b> If a {@link RuleCategory#GUARD} rule throws while
 * enforcing, the
 * statement is blocked. If a {@link RuleCategory#LINT} rule throws, it is
 * reported and the
 * statement continues.</li>
 * </ul>
 *
 * @since 0.1.0
 */
public interface SqlRule {

    /**
     * Returns the stable identifier of this rule, used in findings, metrics and
     * configuration.
     * It must never change once released.
     *
     * @return a non-empty identifier in {@code UPPER_SNAKE_CASE}
     */
    String id();

    /**
     * Returns the category of this rule, which decides how failures to analyse a
     * statement are
     * handled.
     *
     * @return never {@code null}
     */
    RuleCategory category();

    /**
     * Evaluates a statement that was parsed completely.
     *
     * @param context read-only view of the statement; never {@code null}
     * @return the outcome; never {@code null}
     */
    RuleResult evaluate(RuleContext context);
}
