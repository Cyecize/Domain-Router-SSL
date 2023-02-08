package com.cyecize.gatewayserver.api.connection;

import com.cyecize.gatewayserver.api.server.Connection;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketException;

@Slf4j
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

    public static Exchange createFrom(Exchange exchange) {
        final Connection client = new Connection(
                exchange.getClientConnection().getSocket(),
                exchange.getClientConnection().isSsl()
        );

        final Connection server = new Connection(
                exchange.getServerConnection().getSocket(),
                exchange.getServerConnection().isSsl()
        );

        return new Exchange(client, server, exchange.destination);
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

    public void closeCloseables() {
        this.clientConnection.closeCloseables();
        this.serverConnection.closeCloseables();
    }

    public void removeSocketTimeouts() {
        try {
            this.clientConnection.getSocket().setSoTimeout(0);
            this.serverConnection.getSocket().setSoTimeout(0);
        } catch (SocketException e) {
            log.warn("Error while removing socket timeouts! Closing exchange {}.", this.getName());
            this.close();
        }
    }
}
