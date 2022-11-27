package com.cyecize.gatewayserver.api.options;

import com.cyecize.gatewayserver.constants.General;
import com.cyecize.ioc.annotations.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class OptionsServiceImpl implements OptionsService {

    private final ObjectMapper objectMapper;

    private Options optionsCache;

    @Override
    public Options getOptions() {
        if (this.optionsCache != null) {
            return this.optionsCache;
        }

        final String fileName;
        if (!System.getenv().containsKey(General.ENV_VAR_OPTIONS_FILE_NAME)) {
            log.info("Reading default options from file [{}].", General.DEFAULT_OPTIONS_FILE_NAME);
            fileName = General.WORKING_DIRECTORY + General.DEFAULT_OPTIONS_FILE_NAME;
        } else {
            fileName = System.getenv(General.ENV_VAR_OPTIONS_FILE_NAME);
            log.info("Reading options from user provided file {}.", fileName);
        }

        final Options options = this.readFromFile(fileName);
        this.optionsCache = options;

        return options;
    }

    private Options readFromFile(String filePath) {
        Options options;
        try (InputStream inputStream = new FileInputStream(filePath)) {
            try {
                options = this.objectMapper
                        .readerFor(Options.class)
                        .readValue(inputStream);
            } catch (Exception ex) {
                log.error("Error while parsing options file {}!", filePath, ex);
                options = Options.empty();
            }
        } catch (IOException ex) {
            log.error("Error while reading the options file {}!", filePath, ex);
            options = Options.empty();
        }

        if (options.getHttpPort() == null) {
            log.info("Using default HTTP port {}.", General.DEFAULT_HTTP_PORT);
            options.setHttpPort(General.DEFAULT_HTTP_PORT);
        }

        if (options.getHttpsPort() == null) {
            log.info("Using default HTTPS port {}.", General.DEFAULT_HTTPS_PORT);
            options.setHttpsPort(General.DEFAULT_HTTPS_PORT);
        }

        if (options.getRouteOptions() == null || options.getRouteOptions().isEmpty()) {
            log.warn("There are no routes configured in file [{}].", filePath);
            options.setRouteOptions(new ArrayList<>());
        }

        return options;
    }
}
