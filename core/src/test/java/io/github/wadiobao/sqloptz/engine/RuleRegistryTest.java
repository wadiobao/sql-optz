package io.github.wadiobao.sqloptz.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import io.github.wadiobao.sqloptz.api.Mode;
import io.github.wadiobao.sqloptz.api.RuleCategory;
import io.github.wadiobao.sqloptz.api.RuleResult;
import io.github.wadiobao.sqloptz.api.SqlRule;

class RuleRegistryTest {

    @Test
    void guardsRunBeforeLintsRegardlessOfRegistrationOrder() {
        List<String> ran = new ArrayList<>();
        SqlRule lint = TestRules.lint("LINT_A", c -> {
            ran.add("LINT_A");
            return RuleResult.pass();
        });
        SqlRule guard = TestRules.guard("GUARD_Z", c -> {
            ran.add("GUARD_Z");
            return RuleResult.pass();
        });
        TestRules.engine(Mode.OBSERVE, false, lint, guard).evaluate("SELECT 1");
        assertThat(ran).containsExactly("GUARD_Z", "LINT_A");
    }

    @Test
    void orderThenIdBreakTiesWithinACategory() {
        List<String> ran = new ArrayList<>();
        SqlRule b = TestRules.rule("LINT_B", RuleCategory.LINT, 10, c -> {
            ran.add("B");
            return RuleResult.pass();
        });
        SqlRule a = TestRules.rule("LINT_A", RuleCategory.LINT, 20, c -> {
            ran.add("A");
            return RuleResult.pass();
        });
        TestRules.engine(Mode.OBSERVE, false, a, b).evaluate("SELECT 1");
        assertThat(ran).containsExactly("B", "A");
    }

    @Test
    void duplicateIdIsRejected() {
        SqlRule one = TestRules.lint("LINT_DUP", c -> RuleResult.pass());
        SqlRule two = TestRules.lint("LINT_DUP", c -> RuleResult.pass());
        assertThatThrownBy(() -> new RuleRegistry(List.of(one, two)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Duplicate");
    }

    @Test
    void malformedIdIsRejected() {
        SqlRule bad = TestRules.lint("not-valid", c -> RuleResult.pass());
        assertThatThrownBy(() -> new RuleRegistry(List.of(bad))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void configurationNamingAnUnknownRuleIsRejected() {
        RuleRegistry registry = new RuleRegistry(List.of(TestRules.lint("LINT_X", c -> RuleResult.pass())));
        assertThatThrownBy(() -> ModeResolver.of(Mode.OBSERVE, Map.of("LINT_TYPO", Mode.ENFORCE), registry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LINT_TYPO").hasMessageContaining("LINT_X");
    }

    @Test
    void aBlockingGuardStopsLaterRulesFromRunning() {
        AtomicBoolean lintRan = new AtomicBoolean();
        SqlRule guard = TestRules.guard("GUARD_BLOCK", c -> RuleResult.block("no"));
        SqlRule lint = TestRules.lint("LINT_AFTER", c -> {
            lintRan.set(true);
            return RuleResult.pass();
        });
        assertThat(TestRules.engine(Mode.ENFORCE, false, guard, lint).evaluate("SELECT 1").allowed()).isNotNull();
        assertThat(lintRan).isFalse();
    }
}
