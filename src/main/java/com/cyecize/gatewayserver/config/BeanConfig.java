package com.cyecize.gatewayserver.config;

import com.cyecize.gatewayserver.api.options.Options;
import com.cyecize.gatewayserver.api.options.OptionsService;
import com.cyecize.ioc.annotations.Bean;
import com.cyecize.ioc.annotations.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BeanConfig {

    private final OptionsService optionsService;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public Options options() {
        return this.optionsService.getOptions();
    }
}
