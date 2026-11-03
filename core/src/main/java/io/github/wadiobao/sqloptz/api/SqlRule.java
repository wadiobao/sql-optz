package io.github.wadiobao.sqloptz.api;

/**
 * A check applied to every statement that passes through the proxy.
 *
 * <h2>Contract for implementors</h2>
 * <ul>
 *   <li><b>Thread safety.</b> One instance is called concurrently from many threads; it must be
 *       stateless or thread-safe.</li>
 *   <li><b>Detect only.</b> A rule describes what it found. It does not decide whether the
 *       statement is blocked; that depends on the configured {@link Mode}.</li>
 *   <li><b>Read-only.</b> Do not modify anything reachable from the {@link RuleContext}.</li>
 *   <li><b>No side effects.</b> Do not touch the database, the network or the connection.</li>
 *   <li><b>No literals in messages.</b> Messages end up in logs; they must not contain values
 *       from the SQL text.</li>
 *   <li><b>Never return {@code null}.</b> Use {@link RuleResult#pass()} for "nothing found".</li>
 *   <li><b>Exceptions.</b> If a {@link RuleCategory#GUARD} rule throws while enforcing, the
 *       statement is blocked. If a {@link RuleCategory#LINT} rule throws, it is reported and the
 *       statement continues.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public final class NoSelectStar implements SqlRule {
 *     public String id() { return "LINT_SELECT_STAR"; }
 *     public RuleCategory category() { return RuleCategory.LINT; }
 *
 *     public RuleResult detect(RuleContext ctx) {
 *         boolean star = ctx.info().statements().stream().anyMatch(StatementInfo::selectsAllColumns);
 *         return star
 *                 ? RuleResult.report(Severity.INFO, "SELECT * returns every column",
 *                                     "List only the columns you need")
 *                 : RuleResult.pass();
 *     }
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public interface SqlRule {

    /**
     * Returns the stable identifier of this rule, used in findings, metrics and configuration.
     * It must never change once released.
     *
     * @return an identifier matching {@code [A-Z][A-Z0-9_]*}, unique among the registered rules
     */
    String id();

    /**
     * Returns the category of this rule, which decides how failures to analyse a statement are
     * handled and in which order rules run.
     *
     * @return never {@code null}
     */
    RuleCategory category();

    /**
     * Returns the position of this rule among rules of the same category; lower runs first.
     * Rules with equal order run in alphabetical order of their identifier.
     *
     * @return the order; defaults to {@code 100}
     */
    default int order() {
        return 100;
    }

    /**
     * Looks for a problem in a statement that was parsed completely.
     *
     * @param context read-only view of the statement; never {@code null}
     * @return the outcome; never {@code null}
     */
    RuleResult detect(RuleContext context);
}
