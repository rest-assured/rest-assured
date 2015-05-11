package com.jayway.restassured.module.mockmvc.config;

import com.jayway.restassured.config.Config;

import java.util.concurrent.TimeUnit;

public class MockMvcAsyncConfig implements Config {

    private static final long DEFAULT_TIMEOUT = TimeUnit.SECONDS.toMillis(1);
    private final long timeoutInMs;
    private final boolean userConfigured;

    public MockMvcAsyncConfig() {
        this.timeoutInMs = DEFAULT_TIMEOUT;
        this.userConfigured = false;
    }

    public MockMvcAsyncConfig(long timeoutInMs) {
        this.timeoutInMs = timeoutInMs;
        this.userConfigured = true;
    }

    public long getTimeoutInMs() {
        return timeoutInMs;
    }

    public boolean isUserConfigured() {
        return false;
    }
}
