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
public class StartRound2Job implements Job {

    @Autowired
    private RoundService roundService;

    @Override
    public void execute(JobExecutionContext ctx) {
        UUID roomId = UUID.fromString(ctx.getMergedJobDataMap().getString("roomId"));
        try (var mdc = MdcContext.of("roomId", roomId.toString())) {
            log.info("StartRound2Job executing (finalists-ready timeout)");
            roundService.startRound2AfterTimeout(roomId);
        } catch (Exception e) {
            log.error("StartRound2Job failed: {}", e.getMessage(), e);
        }
    }
}
