package io.github.wadiobao.sqloptz.api;

/**
 * How a rule reacts when it detects a problem.
 *
 * <p>Modes are ordered from least to most intrusive. Only {@link #ENFORCE} can change what
 * reaches the database.
 *
 * @since 0.1.0
 */
public enum Mode {

    /** The rule is disabled and is never evaluated. */
    OFF,

    /** Findings are logged and counted. Statement execution is never altered. */
    OBSERVE,

    /** Same as {@link #OBSERVE}, and every finding carries a suggested fix. */
    SUGGEST,

    /** The rule may block the statement or rewrite it before it reaches the driver. */
    ENFORCE
}