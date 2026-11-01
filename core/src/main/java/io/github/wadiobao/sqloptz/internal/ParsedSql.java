package io.github.wadiobao.sqloptz.internal;

import java.util.List;

import io.github.wadiobao.sqloptz.api.SqlStatementInfo;
import net.sf.jsqlparser.statement.Statement;

/***
 * ONLY READ
 */
public record ParsedSql(SqlStatementInfo info, List<Statement> ast) {
    public ParsedSql {
        ast = List.copyOf(ast);
    }
    static ParsedSql failed(io.github.wadiobao.sqloptz.api.ParseStatus status, String message) {
        return new ParsedSql(SqlStatementInfo.failed(status, message), List.of());
    }
}