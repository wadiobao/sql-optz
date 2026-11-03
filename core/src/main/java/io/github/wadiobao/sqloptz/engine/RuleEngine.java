package io.github.wadiobao.sqloptz.engine;

import io.github.wadiobao.sqloptz.api.*;
import io.github.wadiobao.sqloptz.api.SqlGuardViolationException.Reason;
import io.github.wadiobao.sqloptz.internal.parse.ParsedSql;
import io.github.wadiobao.sqloptz.internal.parse.SqlParser;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs the registered rules against a SQL text and decides whether it may
 * proceed.
 *
 * <h2>What each {@link Mode} does</h2>
 * <ul>
 * <li>{@link Mode#OFF}: the rule is not run.</li>
 * <li>{@link Mode#OBSERVE}: findings are recorded, without suggestions; nothing
 * is blocked.</li>
 * <li>{@link Mode#SUGGEST}: findings are recorded with suggestions; nothing is
 * blocked.</li>
 * <li>{@link Mode#ENFORCE}: as above, and a rule that asks for blocking blocks
 * the statement.</li>
 * </ul>
 *
 * <h2>Fail-closed policy</h2>
 * A statement that cannot be analysed is blocked when at least one
 * {@link RuleCategory#GUARD}
 * rule runs in {@link Mode#ENFORCE}. Otherwise it is let through and a finding
 * is recorded. The
 * same applies when a guard rule or the engine itself fails while analysing.
 *
 * <h2>Dry run</h2>
 * With {@code dryRun}, every decision to block is made and reported as a
 * finding, but nothing is
 * actually blocked. Use it to see what {@link Mode#ENFORCE} would do before
 * turning it on.
 *
 * <p>
 * Instances are thread-safe if the rules are.
 */
public final class RuleEngine {

    private static final String ENGINE = "engine";

    private final SqlParser parser;
    private final RuleRegistry registry;
    private final ModeResolver modes;
    private final Clock clock;
    private final boolean dryRun;

    /**
     * Creates an engine.
     *
     * @param parser   parses and caches SQL texts
     * @param registry the rules to run
     * @param modes    decides the mode of each rule
     * @param clock    source of timestamps for findings
     * @param dryRun   if {@code true}, report what would be blocked without
     *                 blocking it
     */
    public RuleEngine(SqlParser parser, RuleRegistry registry, ModeResolver modes, Clock clock, boolean dryRun) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.modes = Objects.requireNonNull(modes, "modes");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.dryRun = dryRun;
    }

    /**
     * Analyses a SQL text. Never throws; an unexpected failure is handled by the
     * same
     * fail-closed policy.
     *
     * @param sql the text about to be sent to the database; may be {@code null}
     * @return the findings and, if the statement must not run, the exception to
     *         throw
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
        for (SqlRule rule : registry.rules()) {
            Mode mode = modes.modeOf(rule);
            if (mode == Mode.OFF) {
                continue;
            }

            RuleResult result;
            try {
                result = Objects.requireNonNull(rule.detect(context), "rule returned null");
            } catch (RuntimeException e) {
                if (mode == Mode.ENFORCE && rule.category() == RuleCategory.GUARD) {
                    findings.add(finding(rule.id(), Severity.ERROR, mode, id, "rule failed while analysing", null));
                    return blocked(findings, rule.id(), Reason.ANALYSIS_ERROR, id,
                            "Statement blocked: a security rule failed while analysing it");
                }
                findings.add(finding(rule.id(), Severity.WARN, mode, id, "rule failed to evaluate", null));
                continue;
            }

            if (result instanceof RuleResult.Detected detected) {
                String suggestion = mode == Mode.OBSERVE ? null : detected.suggestion();
                findings.add(finding(rule.id(), detected.severity(), mode, id, detected.message(), suggestion));

                if (detected.blocking() && mode == Mode.ENFORCE) {
                    if (dryRun) {
                        findings.add(dryRunNotice(rule.id(), id));
                    } else {
                        return blocked(findings, rule.id(), Reason.RULE_VIOLATION, id,
                                "Statement blocked by rule " + rule.id() + ": " + detected.message());
                    }
                }
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

        SqlRule enforcingGuard = registry.rules().stream()
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
        boolean enforcesGuard = registry.rules().stream()
                .anyMatch(r -> r.category() == RuleCategory.GUARD && modes.modeOf(r) == Mode.ENFORCE);
        if (enforcesGuard) {
            findings.add(finding(ENGINE, Severity.ERROR, Mode.ENFORCE, id, message, null));
            return blocked(findings, ENGINE, Reason.ANALYSIS_ERROR, id, "Statement blocked: " + message);
        }
        findings.add(finding(ENGINE, Severity.WARN, Mode.OBSERVE, id, message, null));
        return new Outcome(findings, null);
    }

    /**
     * Ends evaluation with a block, or with a notice only when running in dry-run
     * mode.
     */
    private Outcome blocked(List<Finding> findings, String ruleId, Reason reason, String id, String message) {
        if (dryRun) {
            findings.add(dryRunNotice(ruleId, id));
            return new Outcome(findings, null);
        }
        return new Outcome(findings, new SqlGuardViolationException(ruleId, reason, id, message));
    }

    private Finding dryRunNotice(String ruleId, String id) {
        return finding("ENGINE_DRY_RUN", Severity.WARN, Mode.OBSERVE, id,
                "dry run: the statement would have been blocked (" + ruleId + ")", null);
    }

    private Finding finding(String ruleId, Severity severity, Mode mode, String id, String message, String suggestion) {
        return new Finding(ruleId, severity, mode, id, message, suggestion, null, Instant.now(clock));
    }
}
