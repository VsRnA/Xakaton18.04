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

    @Value("${app.game.boost-window-seconds:5}")
    private int boostWindowSeconds;

    public void scheduleWaitTimerExpiry(UUID roomId) {
        schedule(FillWithBotsJob.class, "fill-bots-" + roomId,
                new JobDataMap() {{ put("roomId", roomId.toString()); }},
                waitTimerSeconds);
    }

    public void scheduleRoundEnd(UUID roomId, int roundNumber) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        data.put("roundNumber", roundNumber);
        schedule(ResolveRoundJob.class, "resolve-round-" + roomId + "-" + roundNumber, data, roundDurationSeconds);
    }

    public void scheduleBoostWindowEnd(UUID roomId, int roundNumber) {
        JobDataMap data = new JobDataMap();
        data.put("roomId", roomId.toString());
        data.put("roundNumber", roundNumber);
        schedule(FinalizeRoundJob.class, "finalize-round-" + roomId + "-" + roundNumber, data, boostWindowSeconds);
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
                    .startAt(Date.from(Instant.now().plusSeconds(delaySeconds)))
                    .build();
            quartzScheduler.scheduleJob(job, trigger);
            log.debug("Scheduled job {} in {}s", jobName, delaySeconds);
        } catch (SchedulerException e) {
            log.error("Failed to schedule job {}: {}", jobName, e.getMessage(), e);
        }
    }
}
