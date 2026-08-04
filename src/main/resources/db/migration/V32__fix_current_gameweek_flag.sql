-- V25 flagged gameweek 34 of the OLD 2025/2026 season as "current" as a
-- dev/testing shortcut (its own comment says as much) so fantasy features
-- had something to point at before a real season existed. That's stale now
-- that the real 2026/2027 season (starting late August) is being set up -
-- clear every is_current flag outside 2026/2027, then make sure exactly one
-- 2026/2027 gameweek is flagged: the earliest-numbered one whose deadline
-- hasn't passed yet (falls back to the smallest gameweek number if every
-- deadline has already passed, which shouldn't happen in practice but keeps
-- this a no-op-safe UPDATE either way).
UPDATE gameweeks SET is_current = FALSE WHERE season <> '2026/2027';

UPDATE gameweeks SET is_current = FALSE WHERE season = '2026/2027';

UPDATE gameweeks
SET is_current = TRUE
WHERE id = (
    SELECT id FROM gameweeks
    WHERE season = '2026/2027'
    ORDER BY
        CASE WHEN deadline > NOW() THEN 0 ELSE 1 END,
        gameweek_number ASC
    LIMIT 1
);
