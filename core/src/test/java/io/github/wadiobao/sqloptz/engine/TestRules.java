package io.github.wadiobao.sqloptz.engine;

import io.github.wadiobao.sqloptz.api.Mode;
import io.github.wadiobao.sqloptz.api.RuleCategory;
import io.github.wadiobao.sqloptz.api.RuleContext;
import io.github.wadiobao.sqloptz.api.RuleResult;
import io.github.wadiobao.sqloptz.api.SqlRule;
import io.github.wadiobao.sqloptz.engine.ModeResolver;
import io.github.wadiobao.sqloptz.engine.RuleEngine;
import io.github.wadiobao.sqloptz.engine.RuleRegistry;
import io.github.wadiobao.sqloptz.internal.parse.ParserConfig;
import io.github.wadiobao.sqloptz.internal.parse.SqlParser;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class TestRules {
    private TestRules() {
    }

    static SqlRule rule(String id, RuleCategory category, int order, Function<RuleContext, RuleResult> fn) {
        return new SqlRule() {
            public String id() {
                return id;
            }

            public RuleCategory category() {
                return category;
            }

            public int order() {
                return order;
            }

            public RuleResult detect(RuleContext c) {
                return fn.apply(c);
            }
        };
    }

    static SqlRule guard(String id, Function<RuleContext, RuleResult> fn) {
        return rule(id, RuleCategory.GUARD, 100, fn);
    }

    static SqlRule lint(String id, Function<RuleContext, RuleResult> fn) {
        return rule(id, RuleCategory.LINT, 100, fn);
    }

    static RuleEngine engine(Mode mode, boolean dryRun, SqlRule... rules) {
        RuleRegistry registry = new RuleRegistry(List.of(rules));
        return new RuleEngine(new SqlParser(ParserConfig.defaults()), registry,
                new ModeResolver(mode, Map.of()), Clock.systemUTC(), dryRun);
    }
}
