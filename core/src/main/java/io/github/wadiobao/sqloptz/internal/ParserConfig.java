package io.github.wadiobao.sqloptz.internal;

import java.time.Duration;

public record ParserConfig(
        int maxSqlLength,
        Duration parseTimeout,
        long cacheMaxWeight,
        Duration cacheExpireAfterAccess) {

    public static ParserConfig defaults() {
        return new ParserConfig(100_000, Duration.ofSeconds(2), 8_000_000L, Duration.ofHours(1));
    }
}