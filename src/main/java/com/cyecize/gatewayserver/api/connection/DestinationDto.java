package com.cyecize.gatewayserver.api.connection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DestinationDto {
    private int port;
    private String host;
}
