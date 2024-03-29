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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cyecize.gatewayserver.util.HttpProtocolUtils.transferHttpRequest;
import static com.cyecize.gatewayserver.util.HttpProtocolUtils.transferHttpResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionHandlerImpl implements ConnectionHandler {

    private final Options options;

    private final PoolService poolService;

    private final KeepAliveSessionHandler keepAliveSessionHandler;

    /**
     * Mapping of host to the desired destination server / servers.
     * If more than one server are present in a collection, choose the most appropriate one or the first one.
     * <p>
     * Eg.
     * abc.com -> localhost:8080
     * www.abc.com -> localhost:8080
     * <p>
     * xyz.com -> http localhost:5050, https localhost:5051
     * abc.xyz.com -> localhost:5060
     * <p>
     * yyy.com -> 192.168.0.1:80
     * 134.134.122.19 -> localhost:8090
     */
    private final Map<String, List<DestinationDto>> domainsMap = new HashMap<>();

    @PostConstruct
    void init() {
        for (RouteOption option : this.options.getRouteOptions()) {
            final DestinationDto dest = new DestinationDto(
                    option.getDestinationPort(),
                    option.getDestinationHost(),
                    option.getScheme()
            );

            this.domainsMap.putIfAbsent(option.getHost(), new ArrayList<>());
            this.domainsMap.get(option.getHost()).add(dest);

            for (String subdomain : option.getSubdomains()) {
                final String domainKey = subdomain + "." + option.getHost();
                this.domainsMap.putIfAbsent(domainKey, new ArrayList<>());
                this.domainsMap.get(domainKey).add(dest);
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

            final List<DestinationDto> servers = this.domainsMap.getOrDefault(host, new ArrayList<>());
            final DestinationDto server = servers.stream()
                    .filter(s -> s.getScheme().isCompatibleWith(clientConn))
                    .findFirst().orElse(null);

            if (server == null) {
                log.warn("No such host " + host);
                clientConn.close();
                return;
            }

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
                    exchange
            );

            this.asyncSocketConnection(
                    String.format("Response Transfer Task, %s", exchangeName),
                    () -> this.transferServerResponse(exchange),
                    exchange
            );

        } catch (IOException ex) {
            log.error("Error while processing client request! {}", clientConn.getHostOrIp(), ex);
            clientConn.close();
        }
    }

    private void transferServerResponse(Exchange exchange) throws IOException {
        transferHttpResponse(exchange);

        final String connection = exchange.getServerConnection().getConnection();
        if (connection == null || !connection.contains("keep-alive")) {
            exchange.close();
            return;
        }

        this.keepAliveSessionHandler.runKeepAliveConnection(exchange);
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
                    }
                }
        ));
    }

    @FunctionalInterface
    interface RunnableThrowable {
        void run() throws IOException;
    }
}
