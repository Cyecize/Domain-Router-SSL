package com.cyecize.gatewayserver.api.pool;

import com.cyecize.gatewayserver.api.options.Options;
import com.cyecize.gatewayserver.constants.General;
import com.cyecize.ioc.annotations.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Service
public class PoolServiceImpl implements PoolService {

    private final Options options;

    private final ThreadPoolExecutor pool;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final AtomicInteger runningTasks = new AtomicInteger();

    public PoolServiceImpl(Options options) {
        this.options = options;
        this.pool = this.getPool();
        this.initScheduledTasks();
    }

    @Override
    public Future<?> submit(Task task) {
        return this.pool.submit(() -> {
            this.runningTasks.incrementAndGet();
            final Optional<ScheduledExecutorService> stuckTaskScheduler = this.createForStuckTask(task);

            try {
                task.getRunnable().run();
            } finally {
                stuckTaskScheduler.ifPresent(ExecutorService::shutdownNow);
                this.runningTasks.decrementAndGet();
            }
        });
    }

    private ThreadPoolExecutor getPool() {
        final int poolSize = Math.max(
                this.options.getThreadPoolSize(),
                General.MIN_THREAD_POOL_SIZE
        );

        log.info("Initializing thread pool of size: {} - {}.", General.MIN_THREAD_POOL_SIZE, poolSize);
        return new ThreadPoolExecutor(
                General.MIN_THREAD_POOL_SIZE,
                poolSize,
                1L,
                TimeUnit.MINUTES,
                new LinkedBlockingQueue<>()
        );
    }

    private void initScheduledTasks() {
        if (this.options.getDebuggingOptions() == null) {
            return;
        }

        final int runningTasksInterval = this.options.getDebuggingOptions().getRunningTasksIntervalSeconds();
        if (runningTasksInterval > 0) {
            this.scheduler.scheduleAtFixedRate(() -> {
                if (this.runningTasks.get() > 0) {
                    log.info("Currently running {} tasks", this.runningTasks);
                }
            }, 0, runningTasksInterval, TimeUnit.SECONDS);
        }
    }

    private Optional<ScheduledExecutorService> createForStuckTask(Task task) {
        if (this.options.getDebuggingOptions() == null) {
            return Optional.empty();
        }

        final int interval = this.options.getDebuggingOptions().getTaskStuckIntervalSeconds();
        if (interval < 1) {
            return Optional.empty();
        }

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                () -> log.error("Stuck on task: {}", task.getTaskName()), interval, interval, TimeUnit.SECONDS
        );

        return Optional.of(scheduler);
    }
}
