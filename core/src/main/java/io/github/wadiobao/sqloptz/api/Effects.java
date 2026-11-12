package io.github.wadiobao.sqloptz.api;

import java.util.Set;

/**
 * What a statement may do, as far as that can be told without running it.
 *
 * <p>Every flag except {@link #returnsResultSet()} is <em>pessimistic</em>: {@code true} means
 * the effect is certain or cannot be ruled out. A function that is not known to be free of side
 * effects therefore makes {@link #mayModifyData()} {@code true}. The flags are a syntactic
 * judgement, not a guarantee: a view, a trigger or a function can do more than the SQL text shows.
 *
 * <p>Instances are immutable and safe to share between threads.
 *
 * @param readsData            the statement may read persistent rows
 * @param returnsResultSet     the statement is certain to send rows back to the client
 * @param mayModifyData        the statement may write or destroy rows
 * @param mayModifySchema      the statement may change the schema, such as {@code CREATE} or {@code DROP}
 * @param mayModifySession     the statement may change session settings, such as {@code SET}
 * @param mayModifyTransaction the statement may change transaction state or take locks
 * @param opaque               nothing further can be known, as for {@code CALL}, {@code EXECUTE},
 *                             dynamic SQL or unsupported syntax
 * @param unresolved           names the analysis could not classify, such as functions that are not
 *                             on the allow-list; never {@code null}
 * @since 0.1.0
 */
public record Effects(
        boolean readsData,
        boolean returnsResultSet,
        boolean mayModifyData,
        boolean mayModifySchema,
        boolean mayModifySession,
        boolean mayModifyTransaction,
        boolean opaque,
        Set<String> unresolved) {

    /** The answer when nothing is known: every effect is possible. */
    public static final Effects UNKNOWN = new Effects(true, false, true, true, true, true, true, Set.of());

    /**
     * Creates the description, making a defensive copy of {@code unresolved}.
     *
     * @throws NullPointerException if {@code unresolved} or one of its elements is {@code null}
     */
    public Effects {
        unresolved = Set.copyOf(unresolved);
    }

    /**
     * Tells whether the statement can be shown to leave data, schema, session and transaction
     * state untouched.
     *
     * @return {@code true} if no effect other than reading is possible
     */
    public boolean readOnly() {
        return !opaque && !mayModifyData && !mayModifySchema && !mayModifySession && !mayModifyTransaction;
    }
}
