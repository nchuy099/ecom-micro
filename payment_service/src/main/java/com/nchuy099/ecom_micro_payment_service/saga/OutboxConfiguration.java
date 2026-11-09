package com.nchuy099.ecom_micro_payment_service.saga;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.Map;

@Configuration
public class OutboxConfiguration {
    @Bean("outboxExecutor")
    ThreadPoolTaskExecutor outboxExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(256);
        executor.setThreadNamePrefix("outbox-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    @Bean
    SchedulerFactoryBeanCustomizer outboxSchedulerContext(OutboxPublisher publisher) {
        return factory -> factory.setSchedulerContextAsMap(Map.of("outboxPublisher", publisher));
    }

    @Bean
    JobDetail outboxRetryJob() {
        return JobBuilder.newJob(OutboxRetryJob.class)
                .withIdentity("outbox-retry")
                .storeDurably()
                .requestRecovery()
                .build();
    }

    @Bean
    Trigger outboxRetryTrigger(
            JobDetail outboxRetryJob,
            @Value("${saga.outbox.retry-interval-ms:10000}") long interval) {
        if (interval <= 0)
            throw new IllegalArgumentException("Outbox retry interval must be positive");
        return TriggerBuilder.newTrigger()
                .withIdentity("outbox-retry-trigger")
                .forJob(outboxRetryJob)
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInMilliseconds(interval)
                                .repeatForever()
                                .withMisfireHandlingInstructionNextWithRemainingCount())
                .build();
    }
}
