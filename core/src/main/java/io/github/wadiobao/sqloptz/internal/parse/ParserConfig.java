package io.github.wadiobao.sqloptz.internal.parse;

import java.time.Duration;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Limits and settings for {@link SqlParser}.
 *
 * @param maxSqlLength          longest text that is parsed; longer texts are rejected
 * @param parseTimeout          total time allowed to parse one text
 * @param cacheMaxWeight        cache size, measured in characters of cached SQL text
 * @param cacheExpireAfterAccess how long an unused cache entry is kept
 * @param pureFunctions         names of functions known not to modify data (case-insensitive);
 *                              any other function makes a statement "possibly modifying"
 */
public record ParserConfig(
        int maxSqlLength,
        Duration parseTimeout,
        long cacheMaxWeight,
        Duration cacheExpireAfterAccess,
        Set<String> pureFunctions) {

    /**
     * Creates a configuration, validating the limits and normalising function names to lower case.
     *
     * @throws IllegalArgumentException if a limit is not positive
     */
    public ParserConfig {
        if (maxSqlLength <= 0) {
            throw new IllegalArgumentException("maxSqlLength must be positive");
        }
        if (parseTimeout.isZero() || parseTimeout.isNegative()) {
            throw new IllegalArgumentException("parseTimeout must be positive");
        }
        if (cacheMaxWeight <= 0) {
            throw new IllegalArgumentException("cacheMaxWeight must be positive");
        }
        pureFunctions = pureFunctions.stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Creates a configuration with the default allow-list of functions. */
    public ParserConfig(int maxSqlLength, Duration parseTimeout, long cacheMaxWeight,
            Duration cacheExpireAfterAccess) {
        this(maxSqlLength, parseTimeout, cacheMaxWeight, cacheExpireAfterAccess, DefaultFunctions.PURE);
    }

    /**
     * Returns the default configuration.
     *
     * @return limits suitable as a starting point; measure before relying on them
     */
    public static ParserConfig defaults() {
        return new ParserConfig(100_000, Duration.ofSeconds(2), 8_000_000L, Duration.ofHours(1));
    }

    /**
     * Returns a copy whose allow-list also contains {@code extra}.
     *
     * @param extra function names to allow in addition to the current ones; never {@code null}
     * @return a new configuration
     */
    public ParserConfig withPureFunctions(Collection<String> extra) {
        Set<String> merged = new HashSet<>(pureFunctions);
        merged.addAll(extra);
        return new ParserConfig(maxSqlLength, parseTimeout, cacheMaxWeight, cacheExpireAfterAccess, merged);
    }
}
