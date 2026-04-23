package com.vsrna.game.infrastructure.scheduler;

import com.vsrna.game.application.port.GamePhase;
import com.vsrna.game.application.port.GameSchedulerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameScheduler implements GameSchedulerPort {

    private final Scheduler quartzScheduler;

    @Value("${app.game.wait-timer-seconds:60}")
    private int waitTimerSeconds;

    @Value("${app.game.round-duration-seconds:30}")
    private int roundDurationSeconds;

    @Value("${app.game.boost-decision-seconds:5}")
    private int boostDecisionSeconds;

    @Value("${app.game.boost-window-seconds:5}")
    private int boostWindowSeconds;

    @Value("${app.game.finalists-ready-timeout-seconds:15}")
    private int finalistsReadyTimeoutSeconds;

    public Instant scheduleWaitTimerExpiry(UUID roomId) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        return schedule(FillWithBotsJob.class, GamePhase.FILL_BOTS.jobKey(roomId), data, waitTimerSeconds);
    }

    public Instant scheduleRoundEnd(UUID roomId, int roundNumber) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        data.put("roundNumber", roundNumber);
        return schedule(ResolveRoundJob.class, GamePhase.RESOLVE_ROUND.jobKey(roomId, roundNumber), data, roundDurationSeconds);
    }

    public Instant scheduleBoostDecisionEnd(UUID roomId, int roundNumber) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        data.put("roundNumber", roundNumber);
        return schedule(BoostWindowStartJob.class, GamePhase.BOOST_DECISION_END.jobKey(roomId, roundNumber), data, boostDecisionSeconds);
    }

    public Instant scheduleBoostWindowEnd(UUID roomId, int roundNumber) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        data.put("roundNumber", roundNumber);
        return schedule(FinalizeRoundJob.class, GamePhase.FINALIZE_ROUND.jobKey(roomId, roundNumber), data, boostWindowSeconds);
    }

    public void scheduleFinalistsReadyTimeout(UUID roomId) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        schedule(StartRound2Job.class, GamePhase.START_ROUND2.jobKey(roomId), data, finalistsReadyTimeoutSeconds);
    }

    public void scheduleRoomOpen(UUID roomId, Instant startAt) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        scheduleAt(OpenScheduledRoomJob.class, GamePhase.OPEN_ROOM.jobKey(roomId), data, startAt);
    }

    public void cancel(UUID roomId, GamePhase phase) {
        JobKey key = JobKey.jobKey(phase.jobKey(roomId));
        try {
            if (quartzScheduler.checkExists(key)) {
                quartzScheduler.deleteJob(key);
                log.debug("Cancelled scheduler job: {}", key);
            }
        } catch (SchedulerException ex) {
            log.warn("Failed to cancel job {}: {}", key, ex.getMessage());
        }
    }

    private Instant schedule(Class<? extends Job> jobClass, String jobName,
                             JobDataMap data, int delaySeconds) {
        Instant expiresAt = Instant.now().plusSeconds(delaySeconds);
        scheduleAt(jobClass, jobName, data, expiresAt);
        return expiresAt;
    }

    private void scheduleAt(Class<? extends Job> jobClass, String jobName,
                             JobDataMap data, Instant startAt) {
        try {
            JobKey key = JobKey.jobKey(jobName);
            if (quartzScheduler.checkExists(key)) {
                quartzScheduler.deleteJob(key);
            }
            JobDetail job = JobBuilder.newJob(jobClass)
                    .withIdentity(key)
                    .usingJobData(data)
                    .storeDurably(false)
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .forJob(job)
                    .startAt(Date.from(startAt))
                    .build();
            quartzScheduler.scheduleJob(job, trigger);
            log.debug("Scheduled job {} at {}", jobName, startAt);
        } catch (SchedulerException ex) {
            log.error("Failed to schedule job {}: {}", jobName, ex.getMessage(), ex);
        }
    }
}
