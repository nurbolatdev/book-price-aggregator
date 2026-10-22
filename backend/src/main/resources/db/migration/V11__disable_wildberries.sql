-- Wildberries answers the parser with its antibot challenge (HTTP 498, a captcha site key), robots.txt
-- included, and never issues the challenge token (checked 2026-10-22, plain HTTP and headless browser).
-- docs/05-parsing-policy.md: such a store is switched off, not worked around.
UPDATE stores SET enabled = FALSE WHERE code = 'WILDBERRIES';
