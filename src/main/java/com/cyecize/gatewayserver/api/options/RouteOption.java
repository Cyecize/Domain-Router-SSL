package com.cyecize.gatewayserver.api.options;

import com.cyecize.gatewayserver.api.server.Scheme;
import lombok.Data;

import java.util.List;

@Data
public class RouteOption {
    private String host;
    private List<String> subdomains;
    private String destinationHost;
    private int destinationPort;
    private String certificateAlias;
    private Scheme scheme;
}
