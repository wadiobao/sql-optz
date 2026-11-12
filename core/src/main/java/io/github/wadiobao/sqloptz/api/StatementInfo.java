package io.github.wadiobao.sqloptz.api;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable summary of one parsed SQL statement.
 *
 * <p>Instances are safe to share between threads. They contain no literal values from the
 * original SQL text.
 *
 * <p>The flags {@link #hasWhere()}, {@link #hasLimit()} and {@link #selectsAllColumns()} are
 * computed for the statement kinds they make sense for, and are {@code false} for all others.
 *
 * @param kind              the category of the outermost statement; never {@code null}
 * @param tables            names of the tables the statement refers to, as reported by the parser
 *                          (best effort, not normalised); never {@code null}; unmodifiable
 * @param hasWhere          {@code true} if a {@code SELECT}, {@code UPDATE} or {@code DELETE}
 *                          has a {@code WHERE} clause
 * @param hasLimit          {@code true} if a plain {@code SELECT} has a row limit clause
 * @param selectsAllColumns {@code true} if the select list of a plain {@code SELECT} contains a
 *                          wildcard such as {@code *}
 * @param effects           what the statement may do, including inside nested parts such as a
 *                          {@code WITH} clause; never {@code null}
 * @since 0.1.0
 */
public record StatementInfo(
        StatementKind kind,
        Set<String> tables,
        boolean hasWhere,
        boolean hasLimit,
        boolean selectsAllColumns,
        Effects effects) {

    /**
     * Creates a summary, making a defensive copy of {@code tables}.
     *
     * @throws NullPointerException if {@code kind}, {@code tables}, {@code effects}, or any
     *                              element of {@code tables}, is {@code null}
     */
    public StatementInfo {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(effects, "effects");
        tables = Set.copyOf(tables);
    }
}
