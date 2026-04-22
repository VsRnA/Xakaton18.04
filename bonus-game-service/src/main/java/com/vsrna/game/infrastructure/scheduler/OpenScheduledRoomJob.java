package com.vsrna.game.infrastructure.scheduler;

import com.vsrna.game.application.gameroom.GameRoomService;
import com.vsrna.game.infrastructure.util.MdcContext;
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
        UUID roomId = UUID.fromString(context.getMergedJobDataMap().getString("roomId"));
        try (var mdc = MdcContext.of("roomId", roomId.toString())) {
            log.info("OpenScheduledRoomJob: opening room");
            gameRoomService.openScheduledRoom(roomId);
        } catch (Exception e) {
            log.error("OpenScheduledRoomJob: failed to open room: {}", e.getMessage(), e);
        }
    }
}
