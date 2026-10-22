-- Kaspi.kz closes connections from the parser without any answer, robots.txt included
-- (checked 2026-10-22). docs/05-parsing-policy.md: such a store is switched off, not worked around.
-- It can be switched back on once it answers the parser.
UPDATE stores SET enabled = FALSE WHERE code = 'KASPI';
