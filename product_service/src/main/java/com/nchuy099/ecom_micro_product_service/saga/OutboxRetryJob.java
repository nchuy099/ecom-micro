package com.nchuy099.ecom_micro_product_service.saga;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.quartz.SchedulerException;

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class OutboxRetryJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            var publisher =
                    (OutboxPublisher) context.getScheduler().getContext().get("outboxPublisher");
            var data = context.getJobDetail().getJobDataMap();
            long cursor = data.containsKey("cursor") ? Long.parseLong(data.getString("cursor")) : 0;
            data.put("cursor", Long.toString(publisher.retryBatch(cursor)));
        } catch (SchedulerException | RuntimeException e) {
            throw new JobExecutionException("Outbox retry failed", e);
        }
    }
}
