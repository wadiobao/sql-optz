package io.github.wadiobao.sqloptz.rule.guard;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wadiobao.sqloptz.api.Mode;
import io.github.wadiobao.sqloptz.api.SqlRule;
import io.github.wadiobao.sqloptz.engine.Outcome;
import io.github.wadiobao.sqloptz.engine.RuleEngine;
import io.github.wadiobao.sqloptz.engine.RuleRegistry;
import io.github.wadiobao.sqloptz.engine.ModeResolver;
import io.github.wadiobao.sqloptz.internal.parse.ParserConfig;
import io.github.wadiobao.sqloptz.internal.parse.SqlParser;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GuardRulesTest {

    private static RuleEngine engine(boolean strict) {
        List<SqlRule> rules = List.of(new MultiStatementRule(strict), new ReadOnlyGuardRule());
        return new RuleEngine(new SqlParser(ParserConfig.defaults()), new RuleRegistry(rules),
                new ModeResolver(Mode.ENFORCE, Map.of()), Clock.systemUTC(), false);
    }

    private static Outcome run(String sql) {
        return engine(false).evaluate(sql);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT 1",
            "SELECT id, name FROM users WHERE id = 1",
            "SELECT u.id, count(*) FROM users u JOIN orders o ON o.user_id = u.id GROUP BY u.id",
            "WITH recent AS (SELECT * FROM orders WHERE created_at > now()) SELECT * FROM recent",
            "SELECT lower(email), upper(name), coalesce(nickname, name) FROM users",
            "SELECT * FROM users ORDER BY id LIMIT 10 OFFSET 20",
            "SELECT * FROM generate_series(1, 10)",
            "SHOW server_version",
            "VALUES (1), (2)",
            "SELECT 1;"
    })
    void plainReadsAreAllowed(String sql) {
        assertThat(run(sql).allowed()).as(sql).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            // DML
            "INSERT INTO t (a) VALUES (1)",
            "UPDATE t SET a = 1 WHERE id = 1",
            "DELETE FROM t WHERE id = 1",
            "MERGE INTO t USING s ON t.id = s.id WHEN MATCHED THEN DELETE",
            "INSERT INTO t SELECT * FROM s",
            // DDL
            "CREATE TABLE t (id int)",
            "CREATE TABLE t2 AS SELECT * FROM t",
            "ALTER TABLE t ADD COLUMN c int",
            "DROP TABLE t",
            "TRUNCATE t",
            "GRANT SELECT ON t TO someone",
            // CTE chứa DML
            "WITH d AS (DELETE FROM t RETURNING *) SELECT * FROM d",
            "WITH i AS (INSERT INTO t VALUES (1) RETURNING *) SELECT * FROM i",
            "WITH u AS (UPDATE t SET a = 1 RETURNING *) SELECT * FROM u",
            // SELECT ... INTO
            "SELECT a INTO new_table FROM t",
            // CALL và các câu không biết hiệu ứng
            "CALL do_something()",
            "DO $$ BEGIN PERFORM 1; END $$",
            "EXECUTE some_prepared_statement",
            // Phiên và giao dịch
            "SET default_transaction_read_only = off",
            "RESET ALL",
            "BEGIN",
            "COMMIT",
            "SELECT * FROM t FOR UPDATE",
            "LOCK TABLE t",
            // Hàm không nằm trong allowlist
            "SELECT nextval('s')",
            "SELECT pg_sleep(10)",
            "SELECT pg_read_file('/etc/passwd')",
            "SELECT lo_import('/tmp/x')",
            // EXPLAIN có thể thực thi
            "EXPLAIN ANALYZE DELETE FROM t"
    })
    void anythingThatMayChangeSomethingIsBlocked(String sql) {
        Outcome o = run(sql);
        assertThat(o.allowed()).as(sql).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT 1; DROP TABLE t",
            "SELECT 1; SELECT 2",
            "SELECT 1;; DROP TABLE t",
            // 'a\' kết thúc chuỗi theo PostgreSQL (backslash không thoát); phần sau là câu thứ hai
            "SELECT 'a\\'; DROP TABLE t; --'"
    })
    void aSecondStatementIsBlocked(String sql) {
        Outcome o = run(sql);
        assertThat(o.allowed()).as(sql).isFalse();
    }

    @Test
    void semicolonsInsideLiteralsAreOneStatementInDefaultMode() {
        assertThat(run("SELECT 'a;b'").allowed()).isTrue();
        assertThat(run("SELECT $$a;b$$").allowed()).isTrue();
        assertThat(run("SELECT 1 /* ; */").allowed()).isTrue();
    }

    @Test
    void strictModeRejectsAnyInteriorSemicolon() {
        RuleEngine strict = engine(true);
        assertThat(strict.evaluate("SELECT 'a;b'").allowed()).isFalse();
        assertThat(strict.evaluate("SELECT 1 /* ; */").allowed()).isFalse();
        assertThat(strict.evaluate("SELECT 1;").allowed()).isTrue();      // dấu ; cuối cùng được phép
        assertThat(strict.evaluate("SELECT 1").allowed()).isTrue();
    }

    @Test
    void twoBlankLinesDoNotSplitAStatement() {
        // PostgreSQL không coi hai dòng trống là dấu phân tách
        assertThat(run("SELECT 1\n\n\nFROM (VALUES (1)) v").allowed()).isTrue();
    }

    @Test
    void blockedStatementsReportTheRuleAndNeverTheSqlText() {
        String secret = "ssn_123456789";
        Outcome o = run("DELETE FROM t WHERE ssn = '" + secret + "'");
        assertThat(o.allowed()).isFalse();
        assertThat(o.violation().getSQLState()).isEqualTo("SO001");
        assertThat(o.violation().getMessage()).doesNotContain(secret);
        assertThat(o.findings()).allSatisfy(f -> assertThat(f.message()).doesNotContain(secret));
    }

    @Test
    void anAllowListedFunctionCanBeAddedThroughConfiguration() {
        ParserConfig config = ParserConfig.defaults().withPureFunctions(List.of("my_safe_fn"));
        RuleEngine e = new RuleEngine(new SqlParser(config),
                new RuleRegistry(List.of(new ReadOnlyGuardRule())),
                new ModeResolver(Mode.ENFORCE, Map.of()), Clock.systemUTC(), false);
        assertThat(e.evaluate("SELECT my_safe_fn(1)").allowed()).isTrue();
        assertThat(run("SELECT my_safe_fn(1)").allowed()).isFalse();   // cấu hình mặc định thì vẫn chặn
    }
}
