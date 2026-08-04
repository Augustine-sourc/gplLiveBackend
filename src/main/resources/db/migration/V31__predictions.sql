-- Predictions feature: for each fixture, a user picks an outcome
-- (HOME/DRAW/AWAY) plus an exact scoreline, optionally flags one fixture per
-- gameweek as their "Banker" (double points), and results are scored once
-- FixtureResultsService.recordResults() marks the fixture FINISHED.
--
-- gameweek_id is denormalized onto predictions (also derivable via
-- fixture -> gameweek) purely so the one-banker-per-gameweek rule below can
-- be enforced as a real DB constraint instead of only in application code.
CREATE TABLE predictions (
    id SERIAL PRIMARY KEY,
    fixture_id INT NOT NULL REFERENCES fixtures(id) ON DELETE CASCADE,
    gameweek_id INT NOT NULL REFERENCES gameweeks(id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    outcome VARCHAR(10) NOT NULL,
    exact_home_goals INT NOT NULL,
    exact_away_goals INT NOT NULL,
    is_banker BOOLEAN NOT NULL DEFAULT FALSE,
    submitted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    points_earned INT,
    scored BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_predictions_user_fixture UNIQUE (user_id, fixture_id)
);

-- One Banker per user per gameweek (partial unique index - rows where
-- is_banker is false are unrestricted).
CREATE UNIQUE INDEX uq_predictions_one_banker_per_gameweek
    ON predictions (user_id, gameweek_id)
    WHERE is_banker = TRUE;

-- High-stakes/derby fixtures carry a flat scoring bonus - admin-settable per
-- fixture (no rivalry-pairs table; simplest thing that works given fixtures
-- are entered manually already).
ALTER TABLE fixtures ADD COLUMN is_derby BOOLEAN NOT NULL DEFAULT FALSE;

-- Running totals used by the prediction leaderboard and the streak
-- multiplier. prediction_streak counts consecutive fixtures (in the order
-- they were scored) where the user got at least the outcome right; it resets
-- to 0 the moment a wrong-outcome prediction is scored.
ALTER TABLE users ADD COLUMN prediction_points INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN prediction_streak INT NOT NULL DEFAULT 0;
