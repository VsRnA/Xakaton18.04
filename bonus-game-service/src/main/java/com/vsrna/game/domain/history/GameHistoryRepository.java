package com.vsrna.game.domain.history;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GameHistoryRepository {
    GameHistory create(GameHistory gameHistory);
    Optional<GameHistory> find(GameHistoryQuery query);
    GameHistory get(GameHistoryQuery query);
    List<GameHistory> list(GameHistoryQuery query);
    List<GameHistory> listByPeriod(Instant from, Instant to);

    /** Кумулятивный баланс: Σ(realPlayersRevenue) − Σ(prizeAwarded живым победителям) по всем играм. */
    BigDecimal getCumulativeSystemBalance();
}
