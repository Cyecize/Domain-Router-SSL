package com.cyecize.gatewayserver;

import com.cyecize.gatewayserver.api.server.Server;
import com.cyecize.ioc.MagicInjector;
import com.cyecize.ioc.annotations.Service;
import com.cyecize.ioc.annotations.StartUp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppStartUp {

    private final List<Server> servers;

    public static void main(String[] args) {
        MagicInjector.run(AppStartUp.class);
    }

    @StartUp
    public void startUp() {
        for (Server server : this.servers) {
            server.start();
        }
    }
}
