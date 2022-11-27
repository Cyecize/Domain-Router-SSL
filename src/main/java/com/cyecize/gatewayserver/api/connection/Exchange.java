package com.cyecize.gatewayserver.api.connection;

import com.cyecize.gatewayserver.api.server.Connection;
import lombok.Getter;

public class Exchange {
    @Getter
    private final Connection clientConnection;

    @Getter
    private final Connection serverConnection;
    private final DestinationDto destination;

    public Exchange(Connection clientConnection, Connection serverConnection, DestinationDto destination) {
        this.clientConnection = clientConnection;
        this.serverConnection = serverConnection;
        this.destination = destination;
    }

    public String getName() {
        return String.format("From client '%s' to server '%s:%d'",
                this.clientConnection.getHostOrIp(),
                this.destination.getHost(),
                this.destination.getPort()
        );
    }

    public void close() {
        try {
            this.clientConnection.close();
        } finally {
            this.serverConnection.close();
        }
    }
}
