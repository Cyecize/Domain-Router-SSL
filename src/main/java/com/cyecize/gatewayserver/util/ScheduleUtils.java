package com.cyecize.gatewayserver.util;

import com.cyecize.gatewayserver.api.server.Connection;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ScheduleUtils {
    public static void scheduleConnectionTerminator(Connection connection, int timeout) {
        if (timeout < 1) {
            return;
        }

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            log.warn("Connection {} is running for more than {} seconds, closing.", connection.getHostOrIp(), timeout);
            connection.close();

            scheduler.shutdownNow();
        }, timeout, TimeUnit.SECONDS);

        connection.addCloseable(() -> scheduler.shutdownNow());
    }
}
