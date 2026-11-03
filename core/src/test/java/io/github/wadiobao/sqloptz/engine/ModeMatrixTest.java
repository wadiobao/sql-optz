package io.github.wadiobao.sqloptz.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.wadiobao.sqloptz.api.Mode;
import io.github.wadiobao.sqloptz.api.RuleResult;
import io.github.wadiobao.sqloptz.api.Severity;
import io.github.wadiobao.sqloptz.api.SqlRule;
import io.github.wadiobao.sqloptz.internal.parse.ParserConfig;
import io.github.wadiobao.sqloptz.internal.parse.SqlParser;
import io.github.wadiobao.sqloptz.rule.guard.WriteWithoutWhereRule;

class ModeMatrixTest {

    private final SqlRule deleteGuard = new WriteWithoutWhereRule();
    private static final String BAD = "DELETE FROM accounts";

    @Test
    void offDoesNotRunTheRule() {
        Outcome o = TestRules.engine(Mode.OFF, false, deleteGuard).evaluate(BAD);
        assertThat(o.allowed()).isTrue();
        assertThat(o.findings()).isEmpty();
    }

    @Test
    void observeRecordsButNeverBlocksAndHasNoSuggestion() {
        Outcome o = TestRules.engine(Mode.OBSERVE, false, deleteGuard).evaluate(BAD);
        assertThat(o.allowed()).isTrue();
        assertThat(o.findings()).singleElement().satisfies(f -> {
            assertThat(f.severity()).isEqualTo(Severity.ERROR);
            assertThat(f.suggestion()).isNull();
        });
    }

    @Test
    void suggestAddsTheSuggestionButNeverBlocks() {
        Outcome o = TestRules.engine(Mode.SUGGEST, false, deleteGuard).evaluate(BAD);
        assertThat(o.allowed()).isTrue();
        assertThat(o.findings().get(0).suggestion()).isNotBlank();
    }

    @Test
    void enforceBlocksWithSO001() {
        Outcome o = TestRules.engine(Mode.ENFORCE, false, deleteGuard).evaluate(BAD);
        assertThat(o.violation().getSQLState()).isEqualTo("SO001");
    }

    @Test
    void enforceDoesNotBlockARuleThatOnlyReports() {
        SqlRule advisory = TestRules.lint("LINT_X", c -> RuleResult.report(Severity.WARN, "slow", "add index"));
        Outcome o = TestRules.engine(Mode.ENFORCE, false, advisory).evaluate("SELECT 1");
        assertThat(o.allowed()).isTrue();
        assertThat(o.findings().get(0).severity()).isEqualTo(Severity.WARN);
    }

    @Test
    void severityChosenByTheRuleIsPreserved() {
        SqlRule info = TestRules.lint("LINT_I", c -> RuleResult.report(Severity.INFO, "fyi", null));
        assertThat(TestRules.engine(Mode.OBSERVE, false, info).evaluate("SELECT 1").findings().get(0).severity())
                .isEqualTo(Severity.INFO);
    }

    @Test
    void perRuleOverrideWinsOverTheDefault() {
        RuleRegistry registry = new RuleRegistry(List.of(deleteGuard));
        ModeResolver modes = ModeResolver.of(Mode.OBSERVE, Map.of("GUARD_WRITE_WITHOUT_WHERE", Mode.ENFORCE), registry);
        RuleEngine e = new RuleEngine(new SqlParser(ParserConfig.defaults()), registry, modes, Clock.systemUTC(),
                false);
        assertThat(e.evaluate(BAD).allowed()).isNotNull();
    }
}
