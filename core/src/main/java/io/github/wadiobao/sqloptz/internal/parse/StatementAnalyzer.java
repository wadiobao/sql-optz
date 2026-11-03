package io.github.wadiobao.sqloptz.internal.parse;

import io.github.wadiobao.sqloptz.api.*;
import net.sf.jsqlparser.expression.operators.relational.*; // nếu cần
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.UnsupportedStatement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import java.util.Set;

final class StatementAnalyzer {

    StatementInfo analyze(Statement s) {
        StatementKind kind = kindOf(s);
        boolean hasWhere = false, hasLimit = false, selectsAll = false;

        if (s instanceof PlainSelect ps) {
            hasWhere = ps.getWhere() != null;
            hasLimit = ps.getLimit() != null;
            selectsAll = ps.getSelectItems() != null
                    && ps.getSelectItems().stream().anyMatch(i -> i.getExpression() instanceof AllColumns);
        } else if (s instanceof Update u) {
            hasWhere = u.getWhere() != null;
        } else if (s instanceof Delete d) {
            hasWhere = d.getWhere() != null;
        }
        return new StatementInfo(kind, tablesOf(s), hasWhere, hasLimit, selectsAll);
    }

    private StatementKind kindOf(Statement s) {
        if (s instanceof UnsupportedStatement)
            return StatementKind.UNKNOWN;
        if (s instanceof Select)
            return StatementKind.SELECT;
        if (s instanceof Insert)
            return StatementKind.INSERT;
        if (s instanceof Update)
            return StatementKind.UPDATE;
        if (s instanceof Delete)
            return StatementKind.DELETE;
        if (s instanceof Merge)
            return StatementKind.MERGE;
        if (s instanceof CreateTable || s instanceof Drop || s instanceof Alter || s instanceof Truncate)
            return StatementKind.DDL;
        return StatementKind.OTHER;
    }

    private Set<String> tablesOf(Statement s) {
        if (s instanceof UnsupportedStatement)
            return Set.of();
        try {
            // Tạo MỚI mỗi lần (visitor có trạng thái). Chữ ký thay đổi giữa 4.x và 5.x:
            // nếu IDE báo lỗi, đổi "new TablesNamesFinder<Void>()" cho khớp bản bạn dùng.
            return new TablesNamesFinder<Void>().getTables(s);
        } catch (RuntimeException e) {
            return Set.of();
        }
    }
}
