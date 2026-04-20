package com.vsrna.game.infrastructure.scheduler;

import com.vsrna.game.application.gameroom.GameRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenScheduledRoomJob implements Job {

    private final GameRoomService gameRoomService;

    @Override
    public void execute(JobExecutionContext context) {
        String roomIdStr = context.getMergedJobDataMap().getString("roomId");
        UUID roomId = UUID.fromString(roomIdStr);
        log.info("OpenScheduledRoomJob: opening room {}", roomId);
        try {
            gameRoomService.openScheduledRoom(roomId);
        } catch (Exception e) {
            log.error("OpenScheduledRoomJob: failed to open room {}: {}", roomId, e.getMessage(), e);
        }
    }
}
