package com.cyecize.gatewayserver.api.pool;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Task {
    private String taskName;
    private final Runnable runnable;
}
