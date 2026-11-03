

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import io.github.wadiobao.sqloptz.api.Finding;
import io.github.wadiobao.sqloptz.api.Mode;
import io.github.wadiobao.sqloptz.api.RuleCategory;
import io.github.wadiobao.sqloptz.api.RuleContext;
import io.github.wadiobao.sqloptz.api.RuleResult;
import io.github.wadiobao.sqloptz.api.SqlRule;
import io.github.wadiobao.sqloptz.engine.ModeResolver;
import io.github.wadiobao.sqloptz.engine.Outcome;
import io.github.wadiobao.sqloptz.engine.RuleEngine;
import io.github.wadiobao.sqloptz.engine.RuleRegistry;
import io.github.wadiobao.sqloptz.internal.parse.ParserConfig;
import io.github.wadiobao.sqloptz.internal.parse.SqlParser;
import io.github.wadiobao.sqloptz.rule.guard.WriteWithoutWhereRule;

class FailClosedTest {

    private final SqlParser parser = new SqlParser(ParserConfig.defaults());

    private RuleEngine engine(Mode mode, SqlRule... rules) {
        return engine(parser, mode, rules);
    }

    private static RuleEngine engine(SqlParser parser, Mode mode, SqlRule... rules) {
        RuleRegistry registry = new RuleRegistry(List.of(rules));
        return new RuleEngine(parser, registry, new ModeResolver(mode, Map.of()), Clock.systemUTC(), false);
    }

    @Test
    void unparseableIsBlockedWhenGuardEnforces() {
        Outcome o = engine(Mode.ENFORCE, new WriteWithoutWhereRule()).evaluate("selec ??? from (((");
        assertThat(o.allowed()).isFalse();
        assertThat(o.violation().getSQLState()).isEqualTo("SO002");
    }

    @Test
    void unparseableIsAllowedInObserve() {
        Outcome o = engine(Mode.OBSERVE, new WriteWithoutWhereRule()).evaluate("selec ??? from (((");
        assertThat(o.allowed()).isTrue();
        assertThat(o.findings()).extracting(Finding::ruleId).contains("ENGINE_UNANALYSABLE");
    }

    @Test
    void validViolationIsBlockedWithSO001() {
        Outcome o = engine(Mode.ENFORCE, new WriteWithoutWhereRule()).evaluate("DELETE FROM accounts");
        assertThat(o.allowed()).isFalse();
        assertThat(o.violation().getSQLState()).isEqualTo("SO001");
    }

    @Test
    void validStatementPasses() {
        assertThat(engine(Mode.ENFORCE, new WriteWithoutWhereRule())
                .evaluate("DELETE FROM accounts WHERE id = ?").allowed()).isTrue();
    }

    @Test
    void secondStatementGarbageBlocksTheWholeText() {
        Outcome o = engine(Mode.ENFORCE, new WriteWithoutWhereRule()).evaluate("SELECT 1; ??? ((");
        assertThat(o.allowed()).isFalse();
    }

    @Test
    void tooLongIsBlocked() {
        ParserConfig small = new ParserConfig(50, Duration.ofSeconds(2), 1_000_000L, Duration.ofHours(1));
        RuleEngine e = engine(new SqlParser(small), Mode.ENFORCE, new WriteWithoutWhereRule());
        Outcome o = e.evaluate("SELECT " + "a,".repeat(100) + "b FROM t");
        assertThat(o.allowed()).isFalse();
        assertThat(o.violation().getSQLState()).isEqualTo("SO002");
    }

    @Test
    void guardRuleCrashBlocks_butLintRuleCrashDoesNot() {
        SqlRule crashingGuard = rule("GUARD_CRASH", RuleCategory.GUARD, ctx -> {
            throw new IllegalStateException("boom");
        });
        SqlRule crashingLint = rule("LINT_CRASH", RuleCategory.LINT, ctx -> {
            throw new IllegalStateException("boom");
        });

        Outcome guard = engine(Mode.ENFORCE, crashingGuard).evaluate("SELECT 1");
        assertThat(guard.allowed()).isFalse();
        assertThat(guard.violation().getSQLState()).isEqualTo("SO003");

        assertThat(engine(Mode.ENFORCE, crashingLint).evaluate("SELECT 1").allowed()).isTrue();
    }

    @Test
    void offModeSkipsRules() {
        assertThat(engine(Mode.OFF, new WriteWithoutWhereRule()).evaluate("DELETE FROM t").allowed()).isTrue();
    }

    @Test
    void neverLeaksSqlText() {
        String secret = "ssn_123456789";
        Outcome o = engine(Mode.ENFORCE, new WriteWithoutWhereRule()).evaluate("SELECT '" + secret + "' FROM (((");
        assertThat(o.allowed()).isFalse();
        assertThat(o.violation().getMessage()).doesNotContain(secret);
        assertThat(o.findings()).allSatisfy(f -> assertThat(f.message()).doesNotContain(secret));
    }

    private static SqlRule rule(String id, RuleCategory category, Function<RuleContext, RuleResult> fn) {
        return new SqlRule() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public RuleCategory category() {
                return category;
            }

            @Override
            public RuleResult detect(RuleContext context) {
                return fn.apply(context);
            }
        };
    }
}