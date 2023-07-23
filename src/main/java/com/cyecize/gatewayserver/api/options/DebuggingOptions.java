package com.cyecize.gatewayserver.api.options;

import lombok.Data;

@Data
public class DebuggingOptions {
    private int runningTasksIntervalSeconds;
    private int taskStuckIntervalSeconds;
    private boolean logSSL;
    private boolean logConnections;
}
