package com.augustine.gplfantasyleaague.domain.engagement.entity;

public enum NotificationType {
    DEADLINE,
    RANK,
    GOAL,
    CAPTAIN,
    // League join requests/accept/reject (see V33__leagues.sql, which adds
    // this value to the Postgres notification_type_enum too).
    LEAGUE
}
