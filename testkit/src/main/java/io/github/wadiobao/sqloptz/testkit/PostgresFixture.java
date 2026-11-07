package io.github.wadiobao.sqloptz.testkit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One real PostgreSQL shared by every integration test in the JVM.
 *
 * <p>The container starts the first time this class is used and is stopped by Testcontainers
 * when the JVM exits. Tests call {@link #reset()} in {@code @BeforeEach} to get an empty
 * database without paying for a new container.
 *
 * <p>The image can be changed with the environment variable {@code SQLOPTZ_PG_IMAGE}, which the
 * CI workflow uses to run the tests against several PostgreSQL versions.
 *
 * <p>The database user is a superuser (the default of the {@code postgres} image). Tests about
 * permissions must create their own restricted roles.
 */
public final class PostgresFixture {

    private static final String IMAGE =
            System.getenv().getOrDefault("SQLOPTZ_PG_IMAGE", "postgres:16-alpine");

    private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(IMAGE)
            .withDatabaseName("sqloptz")
            .withUsername("sqloptz")
            .withPassword("sqloptz");

    private static final PGSimpleDataSource DATA_SOURCE;

    static {
        CONTAINER.start();
        DATA_SOURCE = new PGSimpleDataSource();
        DATA_SOURCE.setUrl(CONTAINER.getJdbcUrl());
        DATA_SOURCE.setUser(CONTAINER.getUsername());
        DATA_SOURCE.setPassword(CONTAINER.getPassword());
    }

    private PostgresFixture() { }

    /**
     * Returns a data source for the shared database. It is not pooled: every call to
     * {@code getConnection()} opens a new connection, which the caller must close.
     *
     * @return never {@code null}
     */
    public static DataSource dataSource() {
        return DATA_SOURCE;
    }

    /**
     * Returns the JDBC URL, for code that needs to open its own connections.
     *
     * @return never {@code null}
     */
    public static String jdbcUrl() {
        return CONTAINER.getJdbcUrl();
    }

    /**
     * Returns the image in use, for example to print it in a failure message.
     *
     * @return never {@code null}
     */
    public static String image() {
        return IMAGE;
    }

    /**
     * Empties the database by dropping and recreating the {@code public} schema.
     *
     * <p>This also removes extensions installed in that schema, so a test that needs one (for
     * example {@code pg_trgm}) must create it after calling this method.
     *
     * @throws IllegalStateException if the database cannot be reset
     */
    public static void reset() {
        execute("DROP SCHEMA public CASCADE", "CREATE SCHEMA public");
    }

    /**
     * Runs statements one after another on a fresh connection.
     *
     * @param statements SQL to run; never {@code null}
     * @throws IllegalStateException if a statement fails
     */
    public static void execute(String... statements) {
        try (Connection connection = DATA_SOURCE.getConnection();
                Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("could not run setup SQL on " + IMAGE, e);
        }
    }
}
