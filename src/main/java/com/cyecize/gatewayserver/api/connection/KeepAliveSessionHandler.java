package com.cyecize.gatewayserver.api.connection;

import com.cyecize.gatewayserver.api.pool.PoolService;
import com.cyecize.gatewayserver.api.pool.Task;
import com.cyecize.gatewayserver.api.server.Connection;
import com.cyecize.ioc.annotations.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import static com.cyecize.gatewayserver.util.HttpProtocolUtils.transferHttpRequest;
import static com.cyecize.gatewayserver.util.HttpProtocolUtils.transferHttpResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeepAliveSessionHandler {
    private final PoolService poolService;

    public void runKeepAliveConnection(Exchange exchange) {
        exchange.closeCloseables();
        exchange.removeSocketTimeouts();

        this.asyncSocketConnection(
                String.format("Request transfer Keep-Alive session %s", exchange.getName()),
                () -> this.runRequestTransfer(exchange),
                exchange
        );

        this.asyncSocketConnection(
                String.format("Response transfer Keep-Alive session %s", exchange.getName()),
                () -> this.runResponseTransfer(exchange),
                exchange
        );
    }

    private void runRequestTransfer(Exchange exchange) throws IOException {
        while (true) {
            // Create new instance to reset data from previous request.
            exchange = Exchange.createFrom(exchange);
            final Connection clientConn = exchange.getClientConnection();
            if (!clientConn.readRequestLines()) {
                return;
            }
            transferHttpRequest(exchange);
        }
    }

    private void runResponseTransfer(Exchange exchange) throws IOException {
        while (true) {
            // Create new instance to reset data from previous request.
            exchange = Exchange.createFrom(exchange);
            final Connection serverConn = exchange.getServerConnection();
            if (!serverConn.readRequestLines()) {
                return;
            }
            System.out.println(exchange.getServerConnection().getConnection());
            transferHttpResponse(exchange);
        }
    }

    private void asyncSocketConnection(String taskName,
                                       RunnableThrowable runnable,
                                       Exchange exchange) {
        this.poolService.submit(new Task(
                taskName,
                () -> {
                    try {
                        runnable.run();
                    } catch (IOException ignored) {
                        exchange.close();
                    } finally {
                        //Since sessions are while true loops, if one of the tasks completes it would mean
                        // that the connection is finally ready to close
                        exchange.close();
                    }
                }
        ));
    }

    @FunctionalInterface
    interface RunnableThrowable {
        void run() throws IOException;
    }
}
