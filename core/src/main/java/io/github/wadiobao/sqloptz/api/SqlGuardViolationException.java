package io.github.wadiobao.sqloptz.api;

import java.sql.SQLException;

/**
 * Thrown before a statement reaches the database when it is blocked.
 *
 * <p>
 * Extends {@link SQLException} so existing JDBC error handling and transaction
 * clean-up keep
 * working. {@link #getSQLState()} tells why the statement was blocked, see
 * {@link Reason}.
 *
 * <p>
 * Neither the message nor any accessor contains the SQL text or its literal
 * values.
 *
 * @since 0.1.0
 */
public class SqlGuardViolationException extends SQLException {

    private static final long serialVersionUID = 1L;

    /** Why a statement was blocked, and the SQLState reported for it. */
    public enum Reason {

        /**
         * A rule found that the statement violates a policy. SQLState {@code SO001}.
         */
        RULE_VIOLATION("SO001"),

        /**
         * The statement could not be analysed, so it was blocked as a precaution
         * (fail-closed).
         * SQLState {@code SO002}.
         */
        UNPARSEABLE("SO002"),

        /**
         * A security rule or the engine failed while analysing the statement, so it was
         * blocked
         * as a precaution (fail-closed). SQLState {@code SO003}.
         */
        ANALYSIS_ERROR("SO003");

        private final String sqlState;

        Reason(String sqlState) {
            this.sqlState = sqlState;
        }

        /** Returns the SQLState associated with this reason. */
        public String sqlState() {
            return sqlState;
        }
    }

    private final String ruleId;
    private final Reason reason;
    private final String statementId;

    /**
     * Creates the exception.
     *
     * @param ruleId      the rule that blocked the statement, or {@code "engine"}
     * @param reason      why it was blocked
     * @param statementId short hash identifying the statement, safe to log
     * @param message     description that must not contain the SQL text
     */
    public SqlGuardViolationException(String ruleId, Reason reason, String statementId, String message) {
        super(message, reason.sqlState());
        this.ruleId = ruleId;
        this.reason = reason;
        this.statementId = statementId;
    }

    /** Returns the identifier of the rule that blocked the statement. */
    public String getRuleId() {
        return ruleId;
    }

    /** Returns why the statement was blocked. */
    public Reason getReason() {
        return reason;
    }

    /** Returns a short hash identifying the blocked statement. */
    public String getStatementId() {
        return statementId;
    }
}
