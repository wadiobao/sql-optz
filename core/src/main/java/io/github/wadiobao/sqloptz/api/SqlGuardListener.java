package io.github.wadiobao.sqloptz.api;

/**
 * Receives every {@link Finding} produced while statements are analysed, for
 * example to forward
 * them to a monitoring system.
 *
 * <p>
 * Implementations are called on the thread that runs the statement, so they
 * must be fast and
 * must not block. An exception thrown by a listener is logged and ignored; it
 * never affects the
 * statement.
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface SqlGuardListener {

    /**
     * Called once for each finding.
     *
     * @param finding the finding; never {@code null}
     */
    void onFinding(Finding finding);
}
