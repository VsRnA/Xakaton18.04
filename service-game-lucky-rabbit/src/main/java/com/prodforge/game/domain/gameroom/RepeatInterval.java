package com.prodforge.game.domain.gameroom;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public enum RepeatInterval {
    EVERY_30_MIN,
    EVERY_HOUR,
    EVERY_DAY,
    EVERY_WEEK,
    EVERY_MONTH;

    public Instant next(Instant from) {
        return switch (this) {
            case EVERY_30_MIN -> from.plus(Duration.ofMinutes(30));
            case EVERY_HOUR   -> from.plus(Duration.ofHours(1));
            case EVERY_DAY    -> from.plus(Duration.ofDays(1));
            case EVERY_WEEK   -> from.plus(Duration.ofDays(7));
            case EVERY_MONTH  -> ZonedDateTime.ofInstant(from, ZoneOffset.UTC)
                    .plusMonths(1).toInstant();
        };
    }
}
