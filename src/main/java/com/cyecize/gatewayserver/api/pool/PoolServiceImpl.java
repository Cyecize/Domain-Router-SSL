package com.cyecize.gatewayserver.api.pool;

import com.cyecize.gatewayserver.api.options.Options;
import com.cyecize.gatewayserver.constants.General;
import com.cyecize.ioc.annotations.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


@Slf4j
@Service
public class PoolServiceImpl implements PoolService {

    private final Options options;

    private final ExecutorService pool;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final AtomicInteger runningTasks = new AtomicInteger();

    private final ThreadLocal<Task> currentTask = new ThreadLocal<>();

    public PoolServiceImpl(Options options) {
        this.options = options;
        this.pool = this.getPool();
        this.initScheduledTasks();
    }

    @Override
    public Future<?> submit(Task task) {
        return this.pool.submit(() -> {
            this.currentTask.set(task);
            Thread.currentThread().setName(task.getTaskName());
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

    @Override
    public void updateCurrentTaskName(String name) {
        this.currentTask.get().setTaskName(name);
        Thread.currentThread().setName(name);
    }

    private ExecutorService getPool() {
        final int minPoolSize = Math.max(
                this.options.getMinThreadPoolSize(),
                General.MIN_THREAD_POOL_SIZE
        );

        final int poolSize = Math.max(
                this.options.getThreadPoolSize(),
                minPoolSize
        );

        log.info("Initializing thread pool of size: {} - {}.", minPoolSize, poolSize);

        return new ScalingThreadPool(
                minPoolSize,
                poolSize,
                1L,
                TimeUnit.MINUTES
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
