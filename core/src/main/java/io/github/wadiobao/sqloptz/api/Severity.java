package io.github.wadiobao.sqloptz.api;

/**
 * How serious a {@link Finding} is.
 *
 * @since 0.1.0
 */
public enum Severity {

    /** Informational; no action is required. */
    INFO,

    /** A likely problem that deserves attention, such as a probable performance issue. */
    WARN,

    /** A serious problem, such as a statement that violates a security policy. */
    ERROR
}