package com.cyecize.gatewayserver.api.options;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Options {
    private Integer httpPort;
    private Integer httpsPort;

    private String workingDir;
    private String keystoreFileName;
    private Integer threadPoolSize;

    private List<RouteOption> routeOptions;
    private DebuggingOptions debuggingOptions;

    static Options empty() {
        final Options options = new Options();
        options.setRouteOptions(new ArrayList<>());
        return options;
    }
}
