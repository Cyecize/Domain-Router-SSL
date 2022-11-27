package com.cyecize.gatewayserver.api.pool;

import lombok.Data;

@Data
public class Task {
    private final String taskName;
    private final Runnable runnable;
}
