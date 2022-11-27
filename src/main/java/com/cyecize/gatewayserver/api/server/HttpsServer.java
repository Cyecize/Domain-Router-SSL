package com.cyecize.gatewayserver.api.server;

import com.cyecize.gatewayserver.api.connection.ConnectionHandler;
import com.cyecize.gatewayserver.api.options.Options;
import com.cyecize.gatewayserver.api.pool.PoolService;
import com.cyecize.gatewayserver.api.pool.Task;
import com.cyecize.ioc.annotations.Nullable;
import com.cyecize.ioc.annotations.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.ServerSocket;

@Service
@Slf4j
@RequiredArgsConstructor
public class HttpsServer implements Server {

    private final Options options;

    @Nullable
    private final SSLContext sslContext;

    private final PoolService poolService;

    private final ConnectionHandler connectionHandler;

    @Override
    public void start() {
        if (!this.options.isStartHttps()) {
            return;
        }

        final Thread thread = new Thread(() -> {
            log.info("Try port {}.", this.options.getHttpsPort());

            final SSLServerSocketFactory factory = this.sslContext.getServerSocketFactory();
            try (ServerSocket listener = factory.createServerSocket(this.options.getHttpsPort())) {
                final SSLServerSocket sslListener = (SSLServerSocket) listener;
                sslListener.setNeedClientAuth(false);

                log.info("Start listening for HTTPS connections!");
                while (true) {
                    final SSLSocket client = (SSLSocket) sslListener.accept();
                    client.setSoTimeout(this.options.getClientSoTimeoutMillis());

                    this.poolService.submit(new Task(
                            "Client Connection (SSL) Task",
                            () -> this.connectionHandler.process(new Connection(client, true))
                    ));
                }

            } catch (IOException ex) {
                log.error("Error while initializing SSL server socket.", ex);
            }
        }, "HTTP Server Thread");
        thread.start();
    }
}
