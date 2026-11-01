package io.github.wadiobao.sqloptz.engine;

import io.github.wadiobao.sqloptz.api.Mode;
import io.github.wadiobao.sqloptz.api.SqlRule;
import java.util.Map;
import java.util.Objects;

/**
 * Decides which {@link Mode} applies to a rule: a per-rule override, else the
 * default.
 */
public final class ModeResolver {

    private final Mode defaultMode;
    private final Map<String, Mode> overrides;

    public ModeResolver(Mode defaultMode, Map<String, Mode> overrides) {
        this.defaultMode = Objects.requireNonNull(defaultMode, "defaultMode");
        this.overrides = Map.copyOf(overrides);
    }

    public Mode modeOf(SqlRule rule) {
        return overrides.getOrDefault(rule.id(), defaultMode);
    }
}
