package com.vsrna.game.domain.gameroom;

import java.math.BigDecimal;
import java.util.UUID;

public record GameRoomQuery(
        UUID id,
        GameRoomStatus status,
        UUID createdByUserId,
        int page,
        int size,
        BigDecimal entryFeeMin,
        BigDecimal entryFeeMax,
        Integer maxPlayersFilter,
        Boolean onlyWithSlots,
        BigDecimal boostCostMin,
        BigDecimal boostCostMax
) {
    public static GameRoomQuery byId(UUID id) {
        return new GameRoomQuery(id, null, null, 0, 20, null, null, null, null, null, null);
    }

    public static GameRoomQuery byStatus(GameRoomStatus status) {
        return new GameRoomQuery(null, status, null, 0, 20, null, null, null, null, null, null);
    }

    public static GameRoomQuery list(int page, int size) {
        return new GameRoomQuery(null, null, null, page, size, null, null, null, null, null, null);
    }

    public static GameRoomQuery listByStatus(GameRoomStatus status, int page, int size) {
        return new GameRoomQuery(null, status, null, page, size, null, null, null, null, null, null);
    }

    public static GameRoomQuery filtered(GameRoomStatus status, BigDecimal entryFeeMin,
            BigDecimal entryFeeMax, Integer maxPlayersFilter, Boolean onlyWithSlots, int page, int size) {
        return new GameRoomQuery(null, status, null, page, size,
                entryFeeMin, entryFeeMax, maxPlayersFilter, onlyWithSlots, null, null);
    }

    public static GameRoomQuery filteredFull(GameRoomStatus status, BigDecimal entryFeeMin,
            BigDecimal entryFeeMax, Integer maxPlayersFilter, Boolean onlyWithSlots,
            BigDecimal boostCostMin, BigDecimal boostCostMax, int page, int size) {
        return new GameRoomQuery(null, status, null, page, size,
                entryFeeMin, entryFeeMax, maxPlayersFilter, onlyWithSlots, boostCostMin, boostCostMax);
    }
}
