package io.github.wadiobao.sqloptz.internal.parse;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import io.github.wadiobao.sqloptz.api.ParseStatus;
import io.github.wadiobao.sqloptz.api.SqlStatementInfo;
import io.github.wadiobao.sqloptz.api.StatementInfo;
import net.sf.jsqlparser.parser.AbstractJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Parses SQL text with JSqlParser and caches the outcome.
 *
 * <h2>Guarantees</h2>
 * <ul>
 * <li><b>Never throws.</b> Every failure is reported through
 * {@link ParseStatus}.</li>
 * <li><b>All or nothing.</b> A text is reported as {@link ParseStatus#OK} only
 * when every
 * statement in it was parsed. Partial results are never returned, because
 * skipping the
 * part that failed would let an unanalysed statement through.</li>
 * <li><b>Bounded.</b> Input length, parse time and cache size are all limited
 * by
 * {@link ParserConfig}.</li>
 * <li><b>Thread-safe.</b> Concurrent calls with the same text parse it
 * once.</li>
 * </ul>
 *
 * <h2>The cached syntax tree is shared and read-only</h2>
 * The {@link ParsedSql#ast()} returned for a text is the same object for every
 * caller. Callers
 * must only read it. Anything that needs to change a statement must parse its
 * own copy.
 *
 * <p>
 * Failures are cached as well, so a statement that cannot be parsed is not
 * parsed again on
 * every call. Texts that are empty or longer than the configured limit are
 * rejected without
 * being cached.
 *
 * <p>
 * The messages in failed results never contain the SQL text, because it may
 * hold sensitive
 * data.
 */
public final class SqlParser {

    private final ParserConfig config;
    private final StatementAnalyzer analyzer;
    private final Cache<String, ParsedSql> cache;

    /**
     * Creates a parser with the given limits.
     *
     * @param config limits for length, parse time and cache; never {@code null}
     * @throws NullPointerException if {@code config} is {@code null}
     */
    public SqlParser(ParserConfig config) {
        this(config, Ticker.systemTicker(), ForkJoinPool.commonPool());
    }

    /**
     * Creates a parser with a controllable clock and executor, so that tests can
     * check cache
     * expiry and eviction deterministically.
     */
    SqlParser(ParserConfig config, Ticker ticker, Executor executor) {
        this.config = Objects.requireNonNull(config, "config");
        this.analyzer = new StatementAnalyzer(config.pureFunctions());
        this.cache = Caffeine.newBuilder()
                .maximumWeight(config.cacheMaxWeight())
                .weigher((String sql, ParsedSql parsed) -> Math.max(1, sql.length()))
                .expireAfterAccess(config.cacheExpireAfterAccess())
                .ticker(ticker)
                .executor(executor)
                .recordStats()
                .build();
    }

    /**
     * Parses a SQL text, using the cache when the same text was parsed before.
     *
     * @param sql the text to parse; may be {@code null}
     * @return the outcome; never {@code null}. The syntax tree inside it is shared
     *         and must
     *         not be modified
     */
    public ParsedSql parse(String sql) {
        if (sql == null || sql.isBlank()) {
            return ParsedSql.failed(ParseStatus.FAILED, "empty sql");
        }
        if (sql.length() > config.maxSqlLength()) {
            return ParsedSql.failed(ParseStatus.TOO_LONG,
                    "sql longer than " + config.maxSqlLength() + " characters");
        }
        // Caffeine runs the loader at most once per key, even with many concurrent
        // callers.
        return cache.get(sql, this::parseUncached);
    }

    /**
     * Returns the cache statistics.
     *
     * @return hit and miss counts, load times and evictions since creation
     */
    public CacheStats cacheStats() {
        return cache.stats();
    }

    /**
     * Returns the cache itself so that metrics can be attached; not part of any
     * contract.
     */
    Cache<String, ParsedSql> cache() {
        return cache;
    }

    /** Applies pending cache maintenance; used by tests. */
    void cleanUp() {
        cache.cleanUp();
    }

    private ParsedSql parseUncached(String sql) {
        long budgetNanos = config.parseTimeout().toNanos();
        long start = System.nanoTime();
        try {
            Statements statements;
            try {
                // Fast mode first: it handles almost every statement.
                statements = parseStatements(sql, false, budgetNanos);
            } catch (Exception | StackOverflowError fast) {
                // Retry in complex mode only if the first attempt failed quickly. The second
                // attempt gets whatever is left of the same time budget.
                long remaining = budgetNanos - (System.nanoTime() - start);
                if (remaining <= 0) {
                    return ParsedSql.failed(ParseStatus.TIMEOUT, "parsing exceeded the time limit");
                }
                statements = parseStatements(sql, true, remaining);
            }
            return toParsed(statements, sql);
        } catch (Exception | StackOverflowError e) {
            boolean timedOut = System.nanoTime() - start >= budgetNanos;
            return ParsedSql.failed(timedOut ? ParseStatus.TIMEOUT : ParseStatus.FAILED, shortMessage(e));
        }
    }

    private Statements parseStatements(String sql, boolean complex, long budgetNanos) throws Exception {
        long timeoutMillis = Math.max(1L, budgetNanos / 1_000_000L);
        return CCJSqlParserUtil.parseStatements(sql, parser -> parser
                .withDialect(AbstractJSqlParser.Dialect.POSTGRESQL) // dollar strings, E'...', PostgreSQL string rules
                .withBlankLinesAsSeparator(false) // PostgreSQL does not treat blank lines as separators
                .withUnsupportedStatements() // keep unknown statements as raw text instead of failing
                .withAllowComplexParsing(complex) // slower, accepts deeply nested expressions
                .withTimeOut(timeoutMillis));
        // Deliberately NOT using withErrorRecovery(): it skips statements that fail to
        // parse.
    }

    private ParsedSql toParsed(Statements statements, String sql) {
        if (statements == null || statements.size() == 0) {
            return ParsedSql.failed(ParseStatus.FAILED, "no statement parsed");
        }
        List<Statement> ast = new ArrayList<>();
        List<StatementInfo> infos = new ArrayList<>();
        for (int i = 0; i < statements.size(); i++) {
            Statement statement = statements.get(i);
            if (statement == null) {
                // A hole means part of the text was skipped. Never report that as success.
                return ParsedSql.failed(ParseStatus.FAILED, "statement " + i + " could not be parsed");
            }
            ast.add(statement);
            infos.add(analyzer.analyze(statement));
        }
        return new ParsedSql(
                new SqlStatementInfo(ParseStatus.OK, infos, null, hasInteriorSemicolon(sql)), ast);
    }

    /** True if a semicolon appears anywhere except as the last non-blank character. */
    static boolean hasInteriorSemicolon(String sql) {
        int end = sql.length();
        while (end > 0 && Character.isWhitespace(sql.charAt(end - 1))) {
            end--;
        }
        if (end > 0 && sql.charAt(end - 1) == ';') {
            end--;
        }
        return sql.substring(0, end).indexOf(';') >= 0;
    }

    /**
     * First line of the message, truncated. Parser messages may quote the SQL, so
     * keep them short.
     */
    private static String shortMessage(Throwable e) {
        String message = e.getMessage();
        if (message == null) {
            return e.getClass().getSimpleName();
        }
        int newline = message.indexOf('\n');
        String first = newline > 0 ? message.substring(0, newline) : message;
        return first.length() > 200 ? first.substring(0, 200) : first;
    }
}
