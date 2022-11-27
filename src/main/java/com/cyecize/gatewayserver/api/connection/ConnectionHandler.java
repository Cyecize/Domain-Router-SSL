package com.cyecize.gatewayserver.api.connection;

import com.cyecize.gatewayserver.api.server.Connection;

public interface ConnectionHandler {
    void process(Connection connection);
}
