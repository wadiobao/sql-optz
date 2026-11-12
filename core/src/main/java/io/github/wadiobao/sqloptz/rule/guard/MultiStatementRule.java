package io.github.wadiobao.sqloptz.rule.guard;

import io.github.wadiobao.sqloptz.api.RuleCategory;
import io.github.wadiobao.sqloptz.api.RuleContext;
import io.github.wadiobao.sqloptz.api.RuleResult;
import io.github.wadiobao.sqloptz.api.SqlRule;
import io.github.wadiobao.sqloptz.api.SqlStatementInfo;

/**
 * Blocks SQL texts that contain more than one statement.
 *
 * <p>Sending several statements in one text is the usual way to smuggle a second, harmful
 * statement behind a harmless one ({@code SELECT 1; DROP TABLE t}).
 *
 * <p>In <em>strict</em> mode the rule also blocks any text with a semicolon other than the final
 * character, even inside a string or a comment. That rejects some harmless statements, such as
 * {@code SELECT 'a;b'}, but it does not depend on the parser and the database agreeing about where
 * a statement ends, so it is the right choice for untrusted input such as generated SQL.
 *
 * <p>This is a guard rule: when running in {@link io.github.wadiobao.sqloptz.api.Mode#ENFORCE},
 * texts that cannot be analysed are blocked too.
 *
 * @since 0.1.0
 */
public final class MultiStatementRule implements SqlRule {

    private final boolean strict;

    /**
     * Creates the rule.
     *
     * @param strict if {@code true}, also block any interior semicolon
     */
    public MultiStatementRule(boolean strict) {
        this.strict = strict;
    }

    /** Creates the rule in the default (non-strict) mode. */
    public MultiStatementRule() {
        this(false);
    }

    @Override
    public String id() {
        return "GUARD_MULTI_STATEMENT";
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.GUARD;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public RuleResult detect(RuleContext context) {
        SqlStatementInfo info = context.info();
        if (info.multiStatement()) {
            return RuleResult.block(
                    "the text contains " + info.statements().size() + " statements; only one is allowed",
                    "Send each statement separately");
        }
        if (strict && info.interiorSemicolon()) {
            return RuleResult.block(
                    "the text contains a semicolon before its end (strict single-statement mode)",
                    "Remove the semicolon, or turn strict mode off for this connection");
        }
        return RuleResult.pass();
    }
}
