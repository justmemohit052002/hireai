package com.vionsys.hireai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Dedicated task scheduler configuration for scheduled background jobs.
 * Prevents scheduled jobs (such as the admin health poller and rate limiter cleanup)
 * from hijacking the WebSocket message broker task scheduler (MessageBroker-N threads).
 */
@Configuration
public class SchedulingConfig implements SchedulingConfigurer {

    @Bean(name = "adminHealthTaskScheduler")
    public ThreadPoolTaskScheduler adminHealthTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("admin-health-poller-");
        scheduler.setAwaitTerminationSeconds(10);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(adminHealthTaskScheduler());
    }
}
