package com.vsrna.backend.infrastructure.persistence.gameroom;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.gameroom.GameRoomConfig;
import com.vsrna.backend.domain.gameroom.GameRoomConfigQuery;
import com.vsrna.backend.domain.gameroom.GameRoomConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GameRoomConfigRepositoryAdapter implements GameRoomConfigRepository {

    private final GameRoomConfigJpaRepository jpa;

    @Override
    public GameRoomConfig create(GameRoomConfig config) {
        return jpa.save(config);
    }

    @Override
    public GameRoomConfig get(GameRoomConfigQuery query) {
        return jpa.findByGameRoomId(query.gameRoomId())
                .orElseThrow(() -> ApiException.notFound("GameRoomConfig", query.gameRoomId().toString()));
    }
}
