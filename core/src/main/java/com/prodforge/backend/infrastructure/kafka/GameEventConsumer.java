package com.prodforge.backend.infrastructure.kafka;

import com.prodforge.backend.application.balance.UserBalanceService;
import com.prodforge.backend.domain.balance.BalanceCommandTypes;
import com.prodforge.backend.infrastructure.kafka.event.BalanceCommandEvent;
import com.prodforge.backend.infrastructure.kafka.event.GameEntryReservedEvent;
import com.prodforge.backend.infrastructure.kafka.event.GameFinishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventConsumer {

    private final UserBalanceService userBalanceService;

    @KafkaListener(topics = "game.finished", groupId = "core",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleGameFinished(GameFinishedEvent event) {
        log.info("Game finished: roomId={}, winnerId={}, prizeAwarded={}, systemRevenue={}",
                event.roomId(), event.winnerId(), event.prizeAwarded(), event.systemRevenue());
    }

    @KafkaListener(topics = "game.entry.reserved", groupId = "core",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleEntryReserved(GameEntryReservedEvent event) {
        log.info("Entry reserved: userId={}, roomId={}, amount={}",
                event.userId(), event.roomId(), event.amount());
    }

    @KafkaListener(topics = "balance.command", groupId = "core",
            containerFactory = "kafkaListenerContainerFactory")
    public void handleBalanceCommand(BalanceCommandEvent event) {
        log.info("Balance command: type={}, userId={}, amount={}, roomId={}",
                event.commandType(), event.userId(), event.amount(), event.roomId());
        try {
            switch (event.commandType()) {
                case BalanceCommandTypes.RESERVE         -> userBalanceService.reservePoints(event.userId(), event.amount(), event.roomId());
                case BalanceCommandTypes.RELEASE         -> userBalanceService.returnReservedPoints(event.userId(), event.amount(), event.roomId());
                case BalanceCommandTypes.AWARD           -> userBalanceService.creditPoints(event.userId(), event.amount(), event.roomId());
                case BalanceCommandTypes.DEDUCT          -> userBalanceService.deductPoints(event.userId(), event.amount(), event.roomId());
                case BalanceCommandTypes.DEDUCT_RESERVED -> userBalanceService.deductReserved(event.userId(), event.amount(), event.roomId());
                default -> log.warn("Unknown balance command type: {}", event.commandType());
            }
        } catch (Exception e) {
            log.error("Failed to process balance command type={}, userId={}, roomId={}: {}",
                    event.commandType(), event.userId(), event.roomId(), e.getMessage(), e);
            throw e;
        }
    }
}
