package io.github.wadiobao.sqloptz.internal.parse;

import java.util.Arrays;
import java.util.Set;

/**
 * Built-in functions that are known not to modify data, used as the default allow-list of the
 * read-only guard.
 *
 * <p>The list is deliberately short. A function missing from it is treated as possibly dangerous,
 * which is the safe direction; add what an application needs through
 * {@link ParserConfig#withPureFunctions(java.util.Collection)}.
 */
final class DefaultFunctions {

    private DefaultFunctions() { }

    static final Set<String> PURE = Set.copyOf(Arrays.asList(
            // aggregates
            "count", "sum", "avg", "min", "max", "bool_and", "bool_or", "string_agg", "array_agg",
            // window functions
            "row_number", "rank", "dense_rank", "ntile", "lag", "lead", "first_value", "last_value",
            // text
            "lower", "upper", "initcap", "length", "char_length", "substr", "substring", "trim", "ltrim",
            "rtrim", "btrim", "concat", "concat_ws", "replace", "left", "right", "position", "strpos",
            "lpad", "rpad", "repeat", "reverse", "split_part", "to_char",
            // numeric
            "abs", "ceil", "ceiling", "floor", "round", "trunc", "mod", "power", "sqrt", "sign",
            "greatest", "least",
            // null handling
            "coalesce", "nullif",
            // date and time (no side effects)
            "now", "current_date", "current_timestamp", "date_trunc", "date_part", "extract", "age",
            "to_date", "to_timestamp", "make_date",
            // set-returning helpers
            "generate_series", "unnest"));
}
