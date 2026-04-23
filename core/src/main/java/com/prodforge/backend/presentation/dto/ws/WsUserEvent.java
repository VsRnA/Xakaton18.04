package com.prodforge.backend.presentation.dto.ws;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public sealed interface WsUserEvent permits
        WsUserEvent.BarrelsDealt,
        WsUserEvent.BalanceUpdated {

    @Schema(description = "Персональный порядок бочек для текущего пользователя (перемешан по userId+round)")
    record BarrelsDealt(
            @Schema(example = "BARRELS_DEALT") String type,
            @Schema(description = "ID комнаты") String roomId,
            @ArraySchema(
                    schema = @Schema(description = "Бочка в персональном порядке показа"),
                    minItems = 12, maxItems = 12,
                    arraySchema = @Schema(description = "Ровно 12 бочек — фиксированный набор раунда, перемешанный для данного игрока")
            )
            List<BarrelInfo> barrels
    ) implements WsUserEvent {

        @Schema(description = "Информация о бочке")
        record BarrelInfo(
                @Schema(description = "ID бочки") String id,
                @Schema(description = "Читаемое название", example = "R1B03") String name,
                @Schema(description = "Номер раунда", example = "1") int roundNumber
        ) {}
    }

    @Schema(description = "Обновление баланса пользователя (резерв, списание, начисление)")
    record BalanceUpdated(
            @Schema(example = "BALANCE_UPDATED") String type,
            @Schema(description = "Новый доступный баланс", example = "1500.00") String availableBalance,
            @Schema(description = "Зарезервированная сумма", example = "200.00") String reservedBalance
    ) implements WsUserEvent {}
}
