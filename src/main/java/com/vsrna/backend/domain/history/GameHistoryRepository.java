package com.vsrna.backend.domain.history;

import java.util.List;
import java.util.Optional;

public interface GameHistoryRepository {
    GameHistory create(GameHistory gameHistory);
    Optional<GameHistory> find(GameHistoryQuery query);
    GameHistory get(GameHistoryQuery query);
    List<GameHistory> list(GameHistoryQuery query);
}
