package com.prodforge.game.infrastructure.scheduler;

import com.prodforge.game.application.round.RoundService;
import com.prodforge.game.infrastructure.util.MdcContext;
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
        try (var mdc = MdcContext.of("roomId", roomId.toString(), "round", String.valueOf(roundNumber))) {
            log.info("FinalizeRoundJob executing");
            roundService.finalizeRound(roomId, roundNumber);
        } catch (Exception e) {
            log.error("FinalizeRoundJob failed: {}", e.getMessage(), e);
        }
    }
}
