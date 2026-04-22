package com.vsrna.game.infrastructure.scheduler;

import com.vsrna.game.application.round.RoundService;
import com.vsrna.game.infrastructure.util.MdcContext;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Slf4j
@DisallowConcurrentExecution
public class BoostWindowStartJob implements Job {

    @Autowired
    private RoundService roundService;

    @Override
    public void execute(JobExecutionContext ctx) {
        UUID roomId = UUID.fromString(ctx.getMergedJobDataMap().getString("roomId"));
        int roundNumber = ctx.getMergedJobDataMap().getInt("roundNumber");
        try (var mdc = MdcContext.of("roomId", roomId.toString(), "round", String.valueOf(roundNumber))) {
            log.info("BoostWindowStartJob executing");
            roundService.startBoostWindow(roomId, roundNumber);
        } catch (Exception e) {
            log.error("BoostWindowStartJob failed: {}", e.getMessage(), e);
        }
    }
}
