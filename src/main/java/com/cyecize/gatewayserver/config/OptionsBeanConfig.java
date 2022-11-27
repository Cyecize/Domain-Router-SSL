package com.cyecize.gatewayserver.config;

import com.cyecize.gatewayserver.api.options.Options;
import com.cyecize.gatewayserver.api.options.OptionsService;
import com.cyecize.ioc.annotations.Bean;
import com.cyecize.ioc.annotations.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OptionsBeanConfig {

    private final OptionsService optionsService;

    @Bean
    public Options options() {
        return this.optionsService.getOptions();
    }
}
