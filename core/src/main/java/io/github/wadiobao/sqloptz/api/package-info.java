/**
 * Public API of sql-optz.
 *
 * <p>This package contains the types that describe the result of analysing a SQL statement.
 * Only this package is covered by compatibility guarantees; anything under
 * {@code io.github.wadiobao.sqloptz.internal} may change without notice.
 *
 * <p>None of the types here expose classes of the underlying SQL parser, so the parser can be
 * upgraded or replaced without breaking callers.
 *
 * @since 0.1.0
 */
package io.github.wadiobao.sqloptz.api;