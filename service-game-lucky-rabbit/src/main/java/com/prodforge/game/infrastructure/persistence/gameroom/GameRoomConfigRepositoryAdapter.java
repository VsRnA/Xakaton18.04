package com.prodforge.game.infrastructure.persistence.gameroom;

import com.prodforge.game.domain.exception.ApiException;
import com.prodforge.game.domain.gameroom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class GameRoomConfigRepositoryAdapter implements GameRoomConfigRepository {

    private final GameRoomConfigJpaRepository jpa;

    @Override
    public GameRoomConfig create(GameRoomConfig config) {
        return toDomain(jpa.save(toJpa(config)));
    }

    @Override
    public GameRoomConfig get(GameRoomConfigQuery query) {
        return jpa.findByGameRoomId(query.gameRoomId())
                .map(this::toDomain)
                .orElseThrow(() -> ApiException.notFound("GameRoomConfig", query.gameRoomId().toString()));
    }

    @Override
    public List<GameRoomConfig> listByRoomIds(List<UUID> roomIds) {
        return jpa.findByGameRoomIdIn(roomIds).stream().map(this::toDomain).toList();
    }

    private GameRoomConfig toDomain(GameRoomConfigJpa e) {
        return new GameRoomConfig(
                e.getGameRoomId(),
                e.getMaxPlayers(),
                e.getEntryFeeAmount(),
                e.getWinnerPayoutPercentage(),
                e.getBoostCostAmount(),
                e.isBoostEnabled(),
                e.getMaxBarrelSelection(),
                e.getScheduledStartAt(),
                e.getRepeatInterval());
    }

    private GameRoomConfigJpa toJpa(GameRoomConfig config) {
        GameRoomConfigJpa e = new GameRoomConfigJpa();
        e.setGameRoomId(config.getGameRoomId());
        e.setMaxPlayers(config.getMaxPlayers());
        e.setEntryFeeAmount(config.getEntryFeeAmount());
        e.setWinnerPayoutPercentage(config.getWinnerPayoutPercentage());
        e.setBoostCostAmount(config.getBoostCostAmount());
        e.setBoostEnabled(config.isBoostEnabled());
        e.setMaxBarrelSelection(config.getMaxBarrelSelection());
        e.setScheduledStartAt(config.getScheduledStartAt());
        e.setRepeatInterval(config.getRepeatInterval());
        return e;
    }
}
