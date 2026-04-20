package com.vsrna.game.infrastructure.scheduler;

import com.vsrna.game.application.round.RoundService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Slf4j
@DisallowConcurrentExecution
public class FinalizeRoundJob implements Job {

    @Autowired
    private RoundService roundService;

    @Override
    public void execute(JobExecutionContext ctx) {
        UUID roomId = UUID.fromString(ctx.getMergedJobDataMap().getString("roomId"));
        int roundNumber = ctx.getMergedJobDataMap().getInt("roundNumber");
        log.info("FinalizeRoundJob executing for room {} round {}", roomId, roundNumber);
        try {
            roundService.finalizeRound(roomId, roundNumber);
        } catch (Exception e) {
            log.error("FinalizeRoundJob failed for room {} round {}: {}", roomId, roundNumber, e.getMessage(), e);
        }
    }
}
