package io.github.wadiobao.sqloptz.engine;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.wadiobao.sqloptz.api.Finding;
import io.github.wadiobao.sqloptz.api.Severity;
import io.github.wadiobao.sqloptz.api.SqlGuardListener;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Delivers findings to the log and to registered listeners.
 *
 * <ul>
 * <li><b>Severity filter.</b> Findings below the configured minimum are
 * dropped.</li>
 * <li><b>Log throttling.</b> The same finding for the same statement is logged
 * at most once per
 * interval, so a query that runs a million times does not write a million log
 * lines.
 * Listeners always receive every finding that passes the severity filter.</li>
 * <li><b>Isolation.</b> A failing listener is logged and ignored; it never
 * affects the
 * statement or other listeners.</li>
 * <li><b>Safe log lines.</b> Line breaks and control characters are removed
 * from messages.</li>
 * </ul>
 *
 * <p>
 * Instances are thread-safe.
 */
public final class FindingPublisher {

    private static final Logger LOG = LoggerFactory.getLogger("io.github.wadiobao.sqloptz.findings");

    private final Severity minSeverity;
    private final List<SqlGuardListener> listeners;
    private final Cache<String, Boolean> recentlyLogged;

    /**
     * Creates a publisher.
     *
     * @param minSeverity lowest severity that is delivered; never {@code null}
     * @param logThrottle minimum time between two identical log lines; zero
     *                    disables throttling
     * @param listeners   listeners to notify; never {@code null}
     */
    public FindingPublisher(Severity minSeverity, Duration logThrottle, List<SqlGuardListener> listeners) {
        this.minSeverity = Objects.requireNonNull(minSeverity, "minSeverity");
        this.listeners = List.copyOf(listeners);
        this.recentlyLogged = logThrottle.isZero() || logThrottle.isNegative()
                ? null
                : Caffeine.newBuilder().expireAfterWrite(logThrottle).maximumSize(10_000).build();
    }

    /**
     * Delivers findings to the log and listeners.
     *
     * @param findings the findings from one evaluation; never {@code null}
     */
    public void publish(List<Finding> findings) {
        for (Finding finding : findings) {
            if (!finding.severity().isAtLeast(minSeverity)) {
                continue;
            }
            if (shouldLog(finding)) {
                log(finding);
            }
            for (SqlGuardListener listener : listeners) {
                try {
                    listener.onFinding(finding);
                } catch (RuntimeException e) {
                    LOG.warn("listener {} failed: {}", listener.getClass().getName(), e.toString());
                }
            }
        }
    }

    private boolean shouldLog(Finding finding) {
        if (recentlyLogged == null) {
            return true;
        }
        String key = finding.ruleId() + ':' + finding.fingerprint() + ':' + finding.mode();
        return recentlyLogged.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }

    private void log(Finding f) {
        String message = sanitize(f.message());
        String suggestion = f.suggestion() == null ? "-" : sanitize(f.suggestion());
        String format = "rule={} severity={} mode={} stmt={} message={} suggestion={}";
        switch (f.severity()) {
            case ERROR -> LOG.error(format, f.ruleId(), f.severity(), f.mode(), f.fingerprint(), message, suggestion);
            case WARN -> LOG.warn(format, f.ruleId(), f.severity(), f.mode(), f.fingerprint(), message, suggestion);
            case INFO -> LOG.info(format, f.ruleId(), f.severity(), f.mode(), f.fingerprint(), message, suggestion);
        }
    }

    /**
     * Replaces line breaks and other control characters so one finding is always
     * one log line.
     */
    static String sanitize(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            out.append(Character.isISOControl(c) ? ' ' : c);
        }
        return out.toString();
    }
}
