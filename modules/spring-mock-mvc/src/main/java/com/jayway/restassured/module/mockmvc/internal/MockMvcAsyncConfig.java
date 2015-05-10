package com.jayway.restassured.module.mockmvc.internal;

public class MockMvcAsyncConfig {

    private final boolean async;
    private final long timeoutInMs;

    public MockMvcAsyncConfig(boolean async, long timeoutInMs) {
        this.async = async;
        this.timeoutInMs = timeoutInMs;
    }

    public boolean isAsync() {
        return async;
    }

    public long getTimeoutInMs() {
        return timeoutInMs;
    }
}
