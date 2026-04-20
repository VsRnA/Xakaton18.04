package com.vsrna.game.infrastructure.scheduler;

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

    public Instant scheduleWaitTimerExpiry(UUID roomId) {
        Instant expiresAt = Instant.now().plusSeconds(waitTimerSeconds);
        schedule(FillWithBotsJob.class, "fill-bots-" + roomId,
                new JobDataMap() {{ put("roomId", roomId.toString()); }},
                waitTimerSeconds);
        return expiresAt;
    }

    public void scheduleRoundEnd(UUID roomId, int roundNumber) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        data.put("roundNumber", roundNumber);
        schedule(ResolveRoundJob.class, "resolve-round-" + roomId + "-" + roundNumber, data, roundDurationSeconds);
    }

    public void scheduleBoostDecisionEnd(UUID roomId, int roundNumber) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        data.put("roundNumber", roundNumber);
        schedule(BoostWindowStartJob.class, "boost-decision-end-" + roomId + "-" + roundNumber, data, boostDecisionSeconds);
    }

    public void scheduleBoostWindowEnd(UUID roomId, int roundNumber) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        data.put("roundNumber", roundNumber);
        schedule(FinalizeRoundJob.class, "finalize-round-" + roomId + "-" + roundNumber, data, boostWindowSeconds);
    }

    public void scheduleRoomOpen(UUID roomId, Instant startAt) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        scheduleAt(OpenScheduledRoomJob.class, "open-room-" + roomId, data, startAt);
    }

    public void cancel(UUID roomId, String phase) {
        JobKey key = JobKey.jobKey(phase + "-" + roomId);
        try {
            if (quartzScheduler.checkExists(key)) {
                quartzScheduler.deleteJob(key);
                log.debug("Cancelled scheduler job: {}", key);
            }
        } catch (SchedulerException e) {
            log.warn("Failed to cancel job {}: {}", key, e.getMessage());
        }
    }

    private void schedule(Class<? extends Job> jobClass, String jobName,
                          JobDataMap data, int delaySeconds) {
        scheduleAt(jobClass, jobName, data, Instant.now().plusSeconds(delaySeconds));
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
        } catch (SchedulerException e) {
            log.error("Failed to schedule job {}: {}", jobName, e.getMessage(), e);
        }
    }
}
