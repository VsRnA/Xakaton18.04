package com.vsrna.game.infrastructure.scheduler;

import com.vsrna.game.application.gameroom.GameRoomService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Slf4j
@DisallowConcurrentExecution
public class FillWithBotsJob implements Job {

    @Autowired
    private GameRoomService gameRoomService;

    @Override
    public void execute(JobExecutionContext ctx) {
        UUID roomId = UUID.fromString(ctx.getMergedJobDataMap().getString("roomId"));
        log.info("FillWithBotsJob executing for room {}", roomId);
        try {
            gameRoomService.fillWithBots(roomId);
        } catch (Exception e) {
            log.error("FillWithBotsJob failed for room {}: {}", roomId, e.getMessage(), e);
        }
    }
}
