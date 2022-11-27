package com.cyecize.gatewayserver.api.server;

import com.cyecize.gatewayserver.api.connection.ConnectionHandler;
import com.cyecize.gatewayserver.api.options.Options;
import com.cyecize.gatewayserver.api.pool.PoolService;
import com.cyecize.gatewayserver.api.pool.Task;
import com.cyecize.ioc.annotations.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Service
@Slf4j
@RequiredArgsConstructor
public class HttpServer implements Server {

    private final Options options;

    private final PoolService poolService;

    private final ConnectionHandler connectionHandler;

    @Override
    public void start() {
        final Thread thread = new Thread(() -> {
            log.info("Try port {}.", this.options.getHttpPort());

            try (final ServerSocket server = new ServerSocket(this.options.getHttpPort())) {
                log.info("Start listening for HTTP connections!");

                while (true) {
                    final Socket client = server.accept();
                    client.setSoTimeout(this.options.getClientSoTimeoutMillis());
                    final Connection connection = new Connection(client, false);

                    this.poolService.submit(new Task(
                            String.format("Client Connection Task, addr: %s", connection.getHostOrIp()),
                            () -> this.connectionHandler.process(connection)
                    ));
                }
            } catch (IOException e) {
                log.error("Error while initializing server socket.", e);
            }
        }, "HTTP Server Thread");
        thread.start();
    }
}
