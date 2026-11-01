package io.github.wadiobao.sqloptz.engine;

import io.github.wadiobao.sqloptz.api.*;
import io.github.wadiobao.sqloptz.api.SqlGuardViolationException.Reason;
import io.github.wadiobao.sqloptz.internal.parse.ParsedSql;
import io.github.wadiobao.sqloptz.internal.parse.SqlParser;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the configured rules against a SQL text and decides whether it may
 * proceed.
 *
 * <h2>Fail-closed policy</h2>
 * A statement that cannot be analysed is blocked when at least one
 * {@link RuleCategory#GUARD}
 * rule runs in {@link Mode#ENFORCE}. Otherwise it is let through and a finding
 * is recorded.
 * The same applies when a guard rule or the engine itself fails while
 * analysing.
 *
 * <p>
 * Instances are thread-safe if the rules are.
 */
public final class RuleEngine {

    private static final String ENGINE = "engine";

    private final SqlParser parser;
    private final List<SqlRule> rules;
    private final ModeResolver modes;
    private final Clock clock;

    public RuleEngine(SqlParser parser, List<SqlRule> rules, ModeResolver modes, Clock clock) {
        this.parser = parser;
        this.rules = List.copyOf(rules);
        this.modes = modes;
        this.clock = clock;
    }

    /**
     * Analyses a SQL text.
     *
     * <p>
     * Never throws. If something unexpected happens, the result is decided by the
     * same
     * fail-closed policy.
     *
     * @param sql the text about to be sent to the database; may be {@code null}
     */
    public Outcome evaluate(String sql) {
        String id = Fingerprints.hash(sql == null ? "" : sql);
        List<Finding> findings = new ArrayList<>();
        try {
            return doEvaluate(sql, id, findings);
        } catch (RuntimeException e) {
            return failure(findings, id, "internal error while analysing the statement");
        }
    }

    private Outcome doEvaluate(String sql, String id, List<Finding> findings) {
        ParsedSql parsed = parser.parse(sql);
        SqlStatementInfo info = parsed.info();

        if (!analysable(info)) {
            return unanalysable(info, id, findings);
        }

        RuleContext context = new RuleContext(info, id);
        for (SqlRule rule : rules) {
            Mode mode = modes.modeOf(rule);
            if (mode == Mode.OFF) {
                continue;
            }
            RuleResult result;
            try {
                result = rule.evaluate(context);
            } catch (RuntimeException e) {
                if (mode == Mode.ENFORCE && rule.category() == RuleCategory.GUARD) {
                    findings.add(finding(rule.id(), Severity.ERROR, mode, id, "rule failed while analysing", null));
                    return blocked(findings, rule.id(), Reason.ANALYSIS_ERROR, id,
                            "Statement blocked: a security rule failed while analysing it");
                }
                findings.add(finding(rule.id(), Severity.WARN, mode, id, "rule failed to evaluate", null));
                continue;
            }

            if (result instanceof RuleResult.Block block) {
                findings.add(finding(rule.id(), Severity.ERROR, mode, id, block.message(), null));
                if (mode == Mode.ENFORCE) {
                    return blocked(findings, rule.id(), Reason.RULE_VIOLATION, id,
                            "Statement blocked by rule " + rule.id() + ": " + block.message());
                }
            } else if (result instanceof RuleResult.Report report) {
                String suggestion = mode == Mode.OBSERVE ? null : report.suggestion();
                findings.add(finding(rule.id(), report.severity(), mode, id, report.message(), suggestion));
            }
        }
        return new Outcome(findings, null);
    }

    private static boolean analysable(SqlStatementInfo info) {
        return info.parsed()
                && info.statements().stream().noneMatch(s -> s.kind() == StatementKind.UNKNOWN);
    }

    private Outcome unanalysable(SqlStatementInfo info, String id, List<Finding> findings) {
        // Deliberately generic: the parser's own message may contain fragments of the
        // SQL text.
        String why = info.parsed()
                ? "contains a statement that could not be interpreted"
                : "could not be parsed (" + info.status() + ")";

        SqlRule enforcingGuard = rules.stream()
                .filter(r -> r.category() == RuleCategory.GUARD && modes.modeOf(r) == Mode.ENFORCE)
                .findFirst()
                .orElse(null);

        if (enforcingGuard != null) {
            findings.add(finding(enforcingGuard.id(), Severity.ERROR, Mode.ENFORCE, id, "statement " + why, null));
            return blocked(findings, enforcingGuard.id(), Reason.UNPARSEABLE, id,
                    "Statement blocked: it could not be analysed (" + info.status() + ")");
        }
        findings.add(finding("ENGINE_UNANALYSABLE", Severity.WARN, Mode.OBSERVE, id,
                "statement " + why + "; rules were skipped", null));
        return new Outcome(findings, null);
    }

    private Outcome failure(List<Finding> findings, String id, String message) {
        boolean enforcesGuard = rules.stream()
                .anyMatch(r -> r.category() == RuleCategory.GUARD && modes.modeOf(r) == Mode.ENFORCE);
        if (enforcesGuard) {
            findings.add(finding(ENGINE, Severity.ERROR, Mode.ENFORCE, id, message, null));
            return blocked(findings, ENGINE, Reason.ANALYSIS_ERROR, id, "Statement blocked: " + message);
        }
        findings.add(finding(ENGINE, Severity.WARN, Mode.OBSERVE, id, message, null));
        return new Outcome(findings, null);
    }

    private static Outcome blocked(List<Finding> findings, String ruleId, Reason reason, String id, String message) {
        return new Outcome(findings, new SqlGuardViolationException(ruleId, reason, id, message));
    }

    private Finding finding(String ruleId, Severity severity, Mode mode, String id, String message, String suggestion) {
        return new Finding(ruleId, severity, mode, id, message, suggestion, null, Instant.now(clock));
    }
}
