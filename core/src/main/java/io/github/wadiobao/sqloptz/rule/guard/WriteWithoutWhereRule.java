package io.github.wadiobao.sqloptz.rule.guard;

import io.github.wadiobao.sqloptz.api.*;

/**
 * Blocks {@code UPDATE} and {@code DELETE} statements that have no
 * {@code WHERE} clause, since
 * they would affect every row of the table.
 *
 * <p>
 * This is a guard rule: when running in {@link Mode#ENFORCE}, statements that
 * cannot be
 * analysed are blocked too.
 *
 * @since 0.1.0
 */
public final class WriteWithoutWhereRule implements SqlRule {

    @Override
    public String id() {
        return "GUARD_WRITE_WITHOUT_WHERE";
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.GUARD;
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        for (StatementInfo s : context.info().statements()) {
            boolean write = s.kind() == StatementKind.UPDATE || s.kind() == StatementKind.DELETE;
            if (write && !s.hasWhere()) {
                return RuleResult.block(s.kind() + " without a WHERE clause would affect every row");
            }
        }
        return RuleResult.pass();
    }
}
