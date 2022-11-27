package com.cyecize.gatewayserver.constants;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class General {
    private final static String START_UP_PACKAGE_PATH = General.class.getName()
            .replace(General.class.getSimpleName(), "")
            .replaceAll("\\.", "/");

    public static final String WORKING_DIRECTORY = URLDecoder.decode(General.class.getResource("").toString()
            .replace("file:/", "/")
            .replace(START_UP_PACKAGE_PATH, ""), StandardCharsets.UTF_8);

    public static final String DEFAULT_OPTIONS_FILE_NAME = "options.json";

    public static final String ENV_VAR_OPTIONS_FILE_NAME = "optionsFile";

    public static final int DEFAULT_HTTP_PORT = 80;

    public static final int DEFAULT_HTTPS_PORT = 443;

    public static final int DEFAULT_THREAD_POOL_SIZE = 20;

    public static final int MIN_THREAD_POOL_SIZE = 3;
}
