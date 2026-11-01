package io.github.wadiobao.sqloptz.api;

/**
 * Broad category of a single SQL statement.
 *
 * <p>The category is derived from the <em>outermost</em> statement only. A statement can
 * contain other operations inside it, for example a {@code SELECT} whose {@code WITH} clause
 * contains a data-modifying statement is reported as {@link #SELECT}. Checks that need to
 * know what a statement does internally must not rely on this value alone.
 *
 * @since 0.1.0
 */
public enum StatementKind {

    /** A query that reads data. */
    SELECT,

    /** {@code INSERT}. */
    INSERT,

    /** {@code UPDATE}. */
    UPDATE,

    /** {@code DELETE}. */
    DELETE,

    /** {@code MERGE}. */
    MERGE,

    /**
     * A statement that changes the structure of the database, such as {@code CREATE TABLE},
     * {@code ALTER TABLE}, {@code DROP} or {@code TRUNCATE}.
     */
    DDL,

    /** A statement that was parsed successfully but belongs to none of the other categories. */
    OTHER,

    /**
     * A statement the parser could not interpret and kept as raw text. Its effect cannot be
     * determined, so it should be treated as potentially dangerous.
     */
    UNKNOWN
}