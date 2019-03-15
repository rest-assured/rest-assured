/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.restassured.config;

import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class HttpClientConfigTest {

    private static final String CUSTOM_MAX_REDIRECTS = "100";

    @Test
    public void cookiePolicyIsSetToIgnoreCookiesByDefault() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig();

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params).containsEntry(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
    }

    @Test
    public void setParamsRespectsOtherConfigurationSettings() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .setParam(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS)
                .reuseHttpClientInstance()
                .setParam(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(
                        entry(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS),
                        entry(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY)
                );
        assertTrue(httpClientConfig.isConfiguredToReuseTheSameHttpClientInstance());

    }

    @Test
    public void setParamsCorrectlyUpdatesPreviousSetting() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .setParam(ClientPNames.MAX_REDIRECTS, "50")
                .setParam(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(entry(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS));
    }

    @Test
    public void addParamsRespectsOtherConfigurationSettings() {
        final Map<String, String> redirectParam = new HashMap<String, String>();
        final Map<String, String> cookieParam = new HashMap<String, String>();

        redirectParam.put(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS);
        cookieParam.put(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .addParams(redirectParam)
                .reuseHttpClientInstance()
                .addParams(cookieParam);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(
                        entry(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS),
                        entry(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY)
                );
        assertTrue(httpClientConfig.isConfiguredToReuseTheSameHttpClientInstance());

    }

    @Test
    public void addParamCorrectlyUpdatesPreviousSetting() {
        final Map<String, String> redirectParam = new HashMap<String, String>();
        final Map<String, String> cookieParam = new HashMap<String, String>();

        redirectParam.put(ClientPNames.MAX_REDIRECTS, "50");
        cookieParam.put(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS);

        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .addParams(redirectParam)
                .reuseHttpClientInstance()
                .addParams(cookieParam);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(entry(ClientPNames.MAX_REDIRECTS, CUSTOM_MAX_REDIRECTS));

    }
}