package com.cyecize.gatewayserver.api.server;

import com.cyecize.ioc.annotations.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HttpsServer implements Server {
    @Override
    public void start() {
        System.out.println("Starting HTTPS");
    }
}
