package io.github.wadiobao.sqloptz.testkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostgresFixtureIT {

    @BeforeEach
    void cleanDatabase() {
        PostgresFixture.reset();
    }

    @Test
    void connectsToARealPostgres() throws Exception {
        try (Connection c = PostgresFixture.dataSource().getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT version()")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString(1)).startsWith("PostgreSQL");
        }
    }

    @Test
    void executeRunsStatementsInOrder() throws Exception {
        PostgresFixture.execute(
                "CREATE TABLE items (id int PRIMARY KEY, name text)",
                "INSERT INTO items VALUES (1, 'a'), (2, 'b')");

        try (Connection c = PostgresFixture.dataSource().getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery("SELECT count(*) FROM items")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(2);
        }
    }

    @Test
    void resetRemovesTablesLeftByEarlierTests() throws Exception {
        PostgresFixture.execute("CREATE TABLE leftover (id int)");

        PostgresFixture.reset();

        try (Connection c = PostgresFixture.dataSource().getConnection();
                Statement s = c.createStatement();
                ResultSet rs = s.executeQuery(
                        "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isZero();
        }
    }
}
