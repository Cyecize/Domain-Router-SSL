package com.cyecize.gatewayserver.api.pool;

import java.util.concurrent.Future;

public interface PoolService {
    Future<?> submit(Task task);

    void updateCurrentTaskName(String name);
}
