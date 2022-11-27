package com.cyecize.gatewayserver.config;

import com.cyecize.ioc.annotations.Bean;
import com.cyecize.ioc.annotations.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeanConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
