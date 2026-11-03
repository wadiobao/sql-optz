package io.github.wadiobao.sqloptz.api;

/**
 * How serious a {@link Finding} is. Values are ordered from least to most serious.
 *
 * @since 0.1.0
 */
public enum Severity {

    /** Informational; no action is required. */
    INFO,

    /** A likely problem that deserves attention, such as a probable performance issue. */
    WARN,

    /** A serious problem, such as a statement that violates a security policy. */
    ERROR;

    /**
     * Tells whether this severity is at least as serious as {@code other}.
     *
     * @param other the threshold to compare with; never {@code null}
     * @return {@code true} if this is {@code other} or more serious
     */
    public boolean isAtLeast(Severity other) {
        return compareTo(other) >= 0;
    }
}
