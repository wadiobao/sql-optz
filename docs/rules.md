# Rules

## GUARD_READ_ONLY

Blocks anything that is not provably a plain read: data and schema changes, session and
transaction control, locks, statements whose effect is unknown (CALL, EXECUTE, DO, EXPLAIN),
and functions that are not on the allow-list.

| Mode | Behaviour |
|---|---|
| OBSERVE / SUGGEST | Reports an ERROR finding; the statement runs |
| ENFORCE | Blocks with SQLState SO001 |

Not a substitute for a database role without write privileges.

## GUARD_MULTI_STATEMENT

Blocks texts with more than one statement. Strict mode also blocks any semicolon before the
end of the text, which does not depend on the parser agreeing with the database.

| Mode | Behaviour |
|---|---|
| OBSERVE / SUGGEST | Reports an ERROR finding; the statements run |
| ENFORCE | Blocks with SQLState SO001 |
