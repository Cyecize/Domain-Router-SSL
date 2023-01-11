package com.cyecize.gatewayserver.api.server;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum Scheme {
    HTTP(c -> !c.isSsl(), false),
    HTTPS(Connection::isSsl, true),
    ALL(c -> true, true);

    private final Function<Connection, Boolean> validator;
    @Getter
    private final boolean isSSLCompatible;

    public boolean isCompatibleWith(Connection connection) {
        return this.validator.apply(connection);
    }
}
