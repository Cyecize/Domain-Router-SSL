package com.cyecize.gatewayserver.api.options;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Options {
    private Integer httpPort;
    private Integer httpsPort;
    private boolean startHttps;

    private String keystoreFileName;
    private String keystoreFileDir;
    private String keystorePassword;
    private String defaultCertificateAlias;
    private Integer threadPoolSize;
    private Integer minThreadPoolSize;
    private Integer killConnectionAfterSeconds;
    private Integer clientSoTimeoutMillis;

    private List<RouteOption> routeOptions;
    private DebuggingOptions debuggingOptions;

    static Options empty() {
        final Options options = new Options();
        options.setRouteOptions(new ArrayList<>());
        return options;
    }
}
