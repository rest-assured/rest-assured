package com.jayway.restassured.config;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

public class HttpClientConfigTest {

    @Test
    public void cookiePolicyIsSetToIgnoreCookiesByDefault() throws Exception {
        final HttpClientConfig httpClientConfig = new HttpClientConfig();

        assertThat((Map<String,String>) httpClientConfig.params(), hasEntry(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES));
    }
}
