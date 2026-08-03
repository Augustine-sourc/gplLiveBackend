-- FreeHitSnapShot.java and ChipService have depended on this table since
-- Free Hit was built, but no migration ever created it - activating Free
-- Hit would fail immediately with "relation free_hit_snapshots does not
-- exist". One row per player in the squad at the moment Free Hit is
-- activated (see ChipService.activateFreeHit/restoreFreeHit).
CREATE TABLE free_hit_snapshots (
    id SERIAL PRIMARY KEY,
    fantasy_id INT NOT NULL REFERENCES fantasy_teams(id) ON DELETE CASCADE,
    gameweek_id INT NOT NULL REFERENCES gameweeks(id) ON DELETE RESTRICT,
    player_id INT NOT NULL REFERENCES players(id) ON DELETE RESTRICT,
    purchase_price NUMERIC(15, 2) NOT NULL,
    current_price NUMERIC(15, 2) NOT NULL,
    is_part_of_xi BOOLEAN NOT NULL DEFAULT FALSE,
    is_captain BOOLEAN NOT NULL DEFAULT FALSE,
    is_vice_captain BOOLEAN NOT NULL DEFAULT FALSE
);
