import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.wadiobao.sqloptz.api.Mode;
import io.github.wadiobao.sqloptz.api.RuleCategory;
import io.github.wadiobao.sqloptz.api.SqlRule;
import io.github.wadiobao.sqloptz.engine.ModeResolver;
import io.github.wadiobao.sqloptz.engine.Outcome;
import io.github.wadiobao.sqloptz.engine.RuleEngine;
import io.github.wadiobao.sqloptz.internal.ParserConfig;
import io.github.wadiobao.sqloptz.rule.guard.WriteWithoutWhereRule;
import io.micrometer.core.instrument.Clock;
import net.sf.jsqlparser.expression.Function;

class FailClosedTest {

    private final SqlParser parser = new SqlParser(ParserConfig.defaults());

    private RuleEngine engine(Mode mode, SqlRule... rules) {
        return new RuleEngine(parser, List.of(rules), new ModeResolver(mode, Map.of()), Clock.systemUTC());
    }

    @Test
    void unparseableIsBlockedWhenGuardEnforces() {
        Outcome o = engine(Mode.ENFORCE, new WriteWithoutWhereRule()).evaluate("selec ??? from (((");
        assertThat(o.violation()).isNotNull();
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
        assertThat(o.violation()).isNotNull();
    }

    @Test
    void tooLongIsBlocked() {
        ParserConfig small = new ParserConfig(50, Duration.ofSeconds(2), 1_000_000L, Duration.ofHours(1));
        RuleEngine e = new RuleEngine(new SqlParser(small), List.of(new WriteWithoutWhereRule()),
                new ModeResolver(Mode.ENFORCE, Map.of()), Clock.systemUTC());
        assertThat(e.evaluate("SELECT " + "a,".repeat(100) + "b FROM t").violation().getSQLState()).isEqualTo("SO002");
    }

    @Test
    void guardRuleCrashBlocks_butLintRuleCrashDoesNot() {
        SqlRule crashingGuard = rule("G", RuleCategory.GUARD, ctx -> {
            throw new IllegalStateException("boom");
        });
        SqlRule crashingLint = rule("L", RuleCategory.LINT, ctx -> {
            throw new IllegalStateException("boom");
        });
        assertThat(engine(Mode.ENFORCE, crashingGuard).evaluate("SELECT 1").violation().getSQLState())
                .isEqualTo("SO003");
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
        assertThat(o.violation().getMessage()).doesNotContain(secret);
        assertThat(o.findings()).allSatisfy(f -> assertThat(f.message()).doesNotContain(secret));
    }

    private static SqlRule rule(String id, RuleCategory cat, Function<RuleContext, RuleResult> fn) {
        return new SqlRule() {
            public String id() {
                return id;
            }

            public RuleCategory category() {
                return cat;
            }

            public RuleResult evaluate(RuleContext c) {
                return fn.apply(c);
            }
        };
    }
}
