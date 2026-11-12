package io.github.wadiobao.sqloptz.rule.guard;

import io.github.wadiobao.sqloptz.api.Effects;
import io.github.wadiobao.sqloptz.api.RuleCategory;
import io.github.wadiobao.sqloptz.api.RuleContext;
import io.github.wadiobao.sqloptz.api.RuleResult;
import io.github.wadiobao.sqloptz.api.SqlRule;
import io.github.wadiobao.sqloptz.api.StatementInfo;
import io.github.wadiobao.sqloptz.api.StatementKind;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows only statements that can be shown to read data and nothing else.
 *
 * <p>It blocks data changes ({@code INSERT}, {@code UPDATE}, {@code DELETE}, {@code MERGE},
 * {@code SELECT ... INTO}, a data-modifying {@code WITH} clause), schema changes, session changes
 * ({@code SET}, {@code RESET}), transaction control and row locks ({@code FOR UPDATE}), statements
 * whose effect cannot be known ({@code CALL}, {@code EXECUTE}, {@code DO}, {@code EXPLAIN},
 * unsupported syntax) and any function that is not on the allow-list.
 *
 * <p>Two independent checks run on every statement: the effects reported by the parser, and the
 * kind of the outermost statement. Either one is enough to block.
 *
 * <h2>Limits</h2>
 * The decision is made from the SQL text only. A view, a trigger or an allowed function can still
 * do more than the text shows, and a function name is resolved by the database through its
 * search path. This rule is an additional layer; the account used for read-only connections must
 * also have no write privileges.
 *
 * @since 0.1.0
 */
public final class ReadOnlyGuardRule implements SqlRule {

    private static final int MAX_NAMES_IN_MESSAGE = 3;
    private static final int MAX_NAME_LENGTH = 40;

    @Override
    public String id() {
        return "GUARD_READ_ONLY";
    }

    @Override
    public RuleCategory category() {
        return RuleCategory.GUARD;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public RuleResult detect(RuleContext context) {
        for (StatementInfo statement : context.info().statements()) {
            String reason = reasonToBlock(statement);
            if (reason != null) {
                return RuleResult.block(reason, "Use a connection that is allowed to write, or rewrite the statement as a plain SELECT");
            }
        }
        return RuleResult.pass();
    }

    private static String reasonToBlock(StatementInfo statement) {
        Effects effects = statement.effects();

        // 1. Effects reported by the parser
        if (effects.opaque()) {
            return "the effect of the statement cannot be determined (CALL, EXECUTE, DO, EXPLAIN or unsupported syntax)";
        }
        if (effects.mayModifySchema()) {
            return "the statement may change the schema";
        }
        if (effects.mayModifyData()) {
            return "the statement may modify data (INSERT, UPDATE, DELETE, SELECT INTO, a data-modifying WITH, "
                    + "or a function that is not on the allow-list" + describe(effects) + ")";
        }
        if (effects.mayModifySession()) {
            return "the statement may change session settings (SET, RESET)";
        }
        if (effects.mayModifyTransaction()) {
            return "the statement may change transaction state or take locks (COMMIT, ROLLBACK, LOCK, FOR UPDATE)";
        }

        // 2. Independent check on the kind of the outermost statement
        StatementKind kind = statement.kind();
        if (kind == StatementKind.INSERT || kind == StatementKind.UPDATE || kind == StatementKind.DELETE
                || kind == StatementKind.MERGE) {
            return "the statement is a data change (" + kind + ")";
        }
        if (kind == StatementKind.DDL) {
            return "the statement is a schema change";
        }
        if (kind == StatementKind.UNKNOWN) {
            return "the statement could not be interpreted";
        }
        return null;
    }

    /** Lists a few unresolved names, shortened, so the message stays readable and cannot flood a log. */
    private static String describe(Effects effects) {
        if (effects.unresolved().isEmpty()) {
            return "";
        }
        List<String> names = effects.unresolved().stream()
                .sorted()
                .limit(MAX_NAMES_IN_MESSAGE)
                .map(n -> n.length() > MAX_NAME_LENGTH ? n.substring(0, MAX_NAME_LENGTH) + "..." : n)
                .collect(Collectors.toList());
        return ": " + String.join(", ", names);
    }
}
