package com.cyecize.gatewayserver.api.connection;

import com.cyecize.gatewayserver.api.options.Options;
import com.cyecize.gatewayserver.api.options.RouteOption;
import com.cyecize.gatewayserver.api.pool.PoolService;
import com.cyecize.gatewayserver.api.pool.Task;
import com.cyecize.gatewayserver.api.server.Connection;
import com.cyecize.gatewayserver.util.ScheduleUtils;
import com.cyecize.ioc.annotations.PostConstruct;
import com.cyecize.ioc.annotations.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static com.cyecize.gatewayserver.util.HttpProtocolUtils.transferHttpRequest;
import static com.cyecize.gatewayserver.util.HttpProtocolUtils.transferHttpResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionHandlerImpl implements ConnectionHandler {

    private final Options options;

    private final PoolService poolService;

    /**
     * Mapping of host to the desired destination server.
     * <p>
     * Eg.
     * abc.com -> localhost:8080
     * www.abc.com -> localhost:8080
     * <p>
     * xyz.com -> localhost:5050
     * abc.xyz.com -> localhost:5060
     * <p>
     * yyy.com -> 192.168.0.1:80
     * 134.134.122.19 -> localhost:8090
     */
    private final Map<String, DestinationDto> domainsMap = new HashMap<>();

    @PostConstruct
    void init() {
        for (RouteOption option : this.options.getRouteOptions()) {
            final DestinationDto dest = new DestinationDto(
                    option.getDestinationPort(),
                    option.getDestinationHost()
            );

            this.domainsMap.put(option.getHost(), dest);
            for (String subdomain : option.getSubdomains()) {
                this.domainsMap.put(subdomain + "." + option.getHost(), dest);
            }
        }
    }

    @Override
    public void process(Connection clientConn) {
        try {
            ScheduleUtils.scheduleConnectionTerminator(clientConn, this.options.getKillConnectionAfterSeconds());

            if (!clientConn.readRequestLines()) {
                return;
            }

            final String host = clientConn.getHost();

            if (!this.domainsMap.containsKey(host)) {
                log.warn("No such host " + host);
                clientConn.close();
                return;
            }

            final DestinationDto server = this.domainsMap.get(host);
            final Exchange exchange;
            try {
                final Socket serverSock = new Socket(server.getHost(), server.getPort());
                final Connection serverConn = new Connection(serverSock, false);

                exchange = new Exchange(clientConn, serverConn, server);
            } catch (IOException ex) {
                log.warn("Could not establish connection to server {}:{}, Host: {}. Message: {}",
                        server.getHost(), server.getPort(), host, ex.getMessage()
                );
                clientConn.close();
                return;
            }

            final String exchangeName = exchange.getName();
            this.poolService.updateCurrentTaskName(String.format("Client Connection Task, %s", exchangeName));

            this.asyncSocketConnection(
                    String.format("Request Transfer Task, %s", exchangeName),
                    () -> transferHttpRequest(exchange),
                    exchange,
                    false
            );

            this.asyncSocketConnection(
                    String.format("Response Transfer Task, %s", exchangeName),
                    () -> transferHttpResponse(exchange),
                    exchange,
                    true
            );

        } catch (IOException ex) {
            log.error("Error while processing client request! {}", clientConn.getHostOrIp(), ex);
            clientConn.close();
        }
    }

    private void asyncSocketConnection(String taskName,
                                       RunnableThrowable runnable,
                                       Exchange exchange,
                                       boolean closeOnFinish) {
        this.poolService.submit(new Task(
                taskName,
                () -> {
                    try {
                        runnable.run();
                    } catch (IOException ignored) {
                        exchange.close();
                        return;
                    }
                    if (closeOnFinish) {
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
