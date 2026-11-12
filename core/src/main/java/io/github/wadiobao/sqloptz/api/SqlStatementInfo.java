package io.github.wadiobao.sqloptz.api;

import java.util.List;
import java.util.Objects;

/**
 * Immutable result of parsing a SQL text, which may contain several statements.
 *
 * <p>When {@link #status()} is {@link ParseStatus#OK}, {@link #statements()} holds one entry per
 * statement, in the order they appear. For every other status the list is empty and nothing is
 * known about the text. A text is never reported as {@code OK} when only part of it could be
 * parsed.
 *
 * <p>Instances are safe to share between threads and may be returned for repeated calls with the
 * same text.
 *
 * @param status            the outcome of parsing; never {@code null}
 * @param statements        the statements found, in order; never {@code null}, unmodifiable, empty
 *                          unless {@code status} is {@link ParseStatus#OK}
 * @param errorMessage      short description of the failure, or {@code null} when {@code status}
 *                          is {@link ParseStatus#OK}; never contains the original SQL text
 * @param interiorSemicolon {@code true} if a semicolon appears anywhere other than as the final
 *                          character of the text. This is a plain character check that ignores
 *                          strings and comments, so it can be {@code true} for a single statement
 *                          (for example {@code SELECT 'a;b'}). It exists as a safety net against
 *                          disagreements between the parser and the database about where a
 *                          statement ends
 * @since 0.1.0
 */
public record SqlStatementInfo(
        ParseStatus status,
        List<StatementInfo> statements,
        String errorMessage,
        boolean interiorSemicolon) {

    /**
     * Creates a result, making a defensive copy of {@code statements}.
     *
     * @throws NullPointerException if {@code status} or {@code statements}, or any element of
     *                              {@code statements}, is {@code null}
     */
    public SqlStatementInfo {
        Objects.requireNonNull(status, "status");
        statements = List.copyOf(statements);
    }

    /**
     * Tells whether the text was parsed completely.
     *
     * @return {@code true} if {@link #status()} is {@link ParseStatus#OK}
     */
    public boolean parsed() {
        return status == ParseStatus.OK;
    }

    /**
     * Tells whether the text contains more than one statement, according to the parser.
     *
     * @return {@code true} if {@link #statements()} has more than one element
     */
    public boolean multiStatement() {
        return statements.size() > 1;
    }

    /**
     * Creates the result for a text that could not be analysed.
     *
     * @param status  the reason; should not be {@link ParseStatus#OK}
     * @param message a short description that must not contain the original SQL text
     * @return a result with an empty statement list
     */
    public static SqlStatementInfo failed(ParseStatus status, String message) {
        return new SqlStatementInfo(status, List.of(), message, false);
    }
}
