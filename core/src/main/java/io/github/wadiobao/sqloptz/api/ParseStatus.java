package io.github.wadiobao.sqloptz.api;

/**
 * Outcome of parsing a SQL text.
 *
 * <p>Anything other than {@link #OK} means that no information about the statement is
 * available. Callers that enforce a security policy should treat every status other than
 * {@link #OK} as "unknown" and decide explicitly, rather than assuming the statement is safe.
 *
 * @since 0.1.0
 */
public enum ParseStatus {

    /** The text was parsed completely and every statement in it was recognised. */
    OK,

    /**
     * The text could not be parsed. This includes empty input, syntax the parser does not
     * support, and texts where only part of the input could be parsed.
     */
    FAILED,

    /** Parsing exceeded the configured time limit and was abandoned. */
    TIMEOUT,

    /** The text is longer than the configured maximum length and was not parsed at all. */
    TOO_LONG
}