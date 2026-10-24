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

import io.restassured.common.spi.http.ClientCustomizer;
import io.restassured.common.spi.http.HttpClientProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

@SuppressWarnings({"unchecked", "deprecation"})
public class HttpClientConfigTest {

    private static final String CUSTOM_MAX_REDIRECTS = "100";

    @Test
    public void cookiePolicyIsSetToIgnoreCookiesByDefault() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig();

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params).containsEntry("http.protocol.cookie-policy", "ignoreCookies");
    }

    @Test
    public void setParamsRespectsOtherConfigurationSettings() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .setParam("http.protocol.max-redirects", CUSTOM_MAX_REDIRECTS)
                .reuseHttpClientInstance()
                .setParam("http.protocol.cookie-policy", "compatibility");

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(
                        entry("http.protocol.max-redirects", CUSTOM_MAX_REDIRECTS),
                        entry("http.protocol.cookie-policy", "compatibility")
                );
        assertThat(httpClientConfig.isConfiguredToReuseTheSameHttpClientInstance()).isTrue();
    }

    @Test
    public void setParamsCorrectlyUpdatesPreviousSetting() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .setParam("http.protocol.max-redirects", "50")
                .setParam("http.protocol.max-redirects", CUSTOM_MAX_REDIRECTS);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(entry("http.protocol.max-redirects", CUSTOM_MAX_REDIRECTS));
    }

    @Test
    public void addParamsRespectsOtherConfigurationSettings() {
        final Map<String, String> redirectParam = new HashMap<String, String>();
        final Map<String, String> cookieParam = new HashMap<String, String>();

        redirectParam.put("http.protocol.max-redirects", CUSTOM_MAX_REDIRECTS);
        cookieParam.put("http.protocol.cookie-policy", "compatibility");

        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .addParams(redirectParam)
                .reuseHttpClientInstance()
                .addParams(cookieParam);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(
                        entry("http.protocol.max-redirects", CUSTOM_MAX_REDIRECTS),
                        entry("http.protocol.cookie-policy", "compatibility")
                );
        assertThat(httpClientConfig.isConfiguredToReuseTheSameHttpClientInstance()).isTrue();
    }

    @Test
    public void addParamCorrectlyUpdatesPreviousSetting() {
        final Map<String, String> redirectParam = new HashMap<String, String>();
        final Map<String, String> cookieParam = new HashMap<String, String>();

        redirectParam.put("http.protocol.max-redirects", "50");
        cookieParam.put("http.protocol.max-redirects", CUSTOM_MAX_REDIRECTS);

        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .addParams(redirectParam)
                .reuseHttpClientInstance()
                .addParams(cookieParam);

        Map<String, Object> params = (Map<String, Object>) httpClientConfig.params();
        assertThat(params)
                .contains(entry("http.protocol.max-redirects", CUSTOM_MAX_REDIRECTS));

    }

    @Test
    public void newSettingsHaveTheDefaultsOfTheParameters() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig();

        assertThat(httpClientConfig.connectTimeout()).isNull();
        assertThat(httpClientConfig.readTimeout()).isNull();
        assertThat(httpClientConfig.connectionRequestTimeout()).isNull();
        assertThat(httpClientConfig.keepsResponseCookies()).isFalse();
        assertThat(httpClientConfig.cookieDatePatterns()).containsExactly("EEE, dd-MMM-yyyy HH:mm:ss z", "EEE, dd MMM yyyy HH:mm:ss z");
        assertThat(httpClientConfig.expectsContinue()).isFalse();
        assertThat(httpClientConfig.multipartMode()).isEqualTo(MultipartMode.STRICT);
    }

    @Test
    public void newSettingsSetTheStringParameters() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .connectTimeout(Duration.ofSeconds(1))
                .readTimeout(Duration.ofSeconds(2))
                .connectionRequestTimeout(Duration.ofSeconds(3))
                .keepResponseCookies(true)
                .cookieDatePatterns("yyyy-MM-dd")
                .expectContinue(true)
                .multipartMode(MultipartMode.RFC6532);

        assertThat((Map<String, Object>) httpClientConfig.params()).contains(
                entry("http.connection.timeout", 1000),
                entry("http.socket.timeout", 2000),
                entry("http.conn-manager.timeout", 3000L),
                entry("http.protocol.cookie-policy", "default"),
                entry("http.protocol.cookie-datepatterns", Collections.singletonList("yyyy-MM-dd")),
                entry("http.protocol.expect-continue", true));
        assertThat(httpClientConfig.multipartMode()).isEqualTo(MultipartMode.RFC6532);
    }

    @Test
    public void newGettersReadTheParametersSetWithTheOldMethods() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig()
                .setParam("http.connection.timeout", 1000)
                .setParam("http.socket.timeout", "2000")
                .setParam("http.conn-manager.timeout", 3000L)
                .setParam("http.protocol.cookie-policy", "compatibility")
                .setParam("http.protocol.cookie-datepatterns", Collections.singletonList("yyyy-MM-dd"))
                .setParam("http.protocol.expect-continue", true);

        assertThat(httpClientConfig.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
        assertThat(httpClientConfig.readTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(httpClientConfig.connectionRequestTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(httpClientConfig.keepsResponseCookies()).isTrue();
        assertThat(httpClientConfig.cookieDatePatterns()).containsExactly("yyyy-MM-dd");
        assertThat(httpClientConfig.expectsContinue()).isTrue();
    }

    @Test
    public void keepResponseCookiesFalseIgnoresCookies() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig().keepResponseCookies(true).keepResponseCookies(false);

        assertThat((Map<String, Object>) httpClientConfig.params()).contains(entry("http.protocol.cookie-policy", "ignoreCookies"));
        assertThat(httpClientConfig.keepsResponseCookies()).isFalse();
    }

    @Test
    public void negativeTimeoutIsRejected() {
        assertThatThrownBy(() -> new HttpClientConfig().readTimeout(Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("readTimeout can't be negative, was PT-0.001S");
    }

    @Test
    public void timeoutUnderOneMillisecondIsRoundedUpSinceZeroMeansNoLimit() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig().readTimeout(Duration.ofNanos(1)).connectionRequestTimeout(Duration.ZERO);

        assertThat(httpClientConfig.readTimeout()).isEqualTo(Duration.ofMillis(1));
        assertThat(httpClientConfig.connectionRequestTimeout()).isEqualTo(Duration.ZERO);
    }

    @Test
    public void everyMultipartModeIsKept() {
        for (MultipartMode multipartMode : MultipartMode.values()) {
            assertThat(new HttpClientConfig().multipartMode(multipartMode).multipartMode()).isEqualTo(multipartMode);
        }
    }

    @Test
    public void noClientIsChosenByDefault() {
        assertThat(new HttpClientConfig().client()).isNull();
        assertThat(new HttpClientConfig().customizers()).isEmpty();
    }

    @Test
    public void clientIsWhatWasGiven() {
        assertThat(new HttpClientConfig().client(HttpClientType.APACHE5).client()).isEqualTo(HttpClientType.APACHE5);
        assertThat(new HttpClientConfig().client(" okhttp ").client()).isEqualTo("okhttp");
        assertThat(new HttpClientConfig().client(HttpClientType.JDK).client(CustomProvider.class).client()).isEqualTo(CustomProvider.class);
    }

    @Test
    public void blankClientNameIsRejected() {
        assertThatThrownBy(() -> new HttpClientConfig().client(" ")).isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("The name of the HTTP client can't be blank");
    }

    @Test
    public void customizersAreKeptInOrder() {
        Consumer<StringBuilder> first = builder -> builder.append("1");
        Consumer<Object> second = builder -> { };

        HttpClientConfig config = new HttpClientConfig().customizeClient(StringBuilder.class, first).customizeClient(StringBuilder.class, second);

        assertThat(config.customizers()).containsExactly(new ClientCustomizer(StringBuilder.class, first),
                new ClientCustomizer(StringBuilder.class, second));
        assertThatThrownBy(() -> config.customizers().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void clientAndCustomizersAreKeptByTheOtherSettings() {
        HttpClientConfig config = new HttpClientConfig().client(HttpClientType.JDK).customizeClient(StringBuilder.class, builder -> { })
                .setParam("a", "b").readTimeout(Duration.ofSeconds(1)).reuseHttpClientInstance().dontReuseHttpClientInstance()
                .withParams(Collections.emptyMap()).addParams(Collections.emptyMap()).multipartMode(MultipartMode.BROWSER_COMPATIBLE);

        assertThat(config.client()).isEqualTo(HttpClientType.JDK);
        assertThat(config.customizers()).hasSize(1);
    }

    @Test
    public void providerNamesOfHttpClientTypes() {
        assertThat(HttpClientType.JDK.providerName()).isEqualTo("jdk");
        assertThat(HttpClientType.APACHE5.providerName()).isEqualTo("apache5");
    }

    @Test
    public void reusedHttpClientInstanceIsKeptByCopiesThatDontChangeHowTheClientIsCreated() {
        final HttpClientConfig httpClientConfig = new HttpClientConfig().reuseHttpClientInstance();
        final Object reuseIdentity = httpClientConfig.reuseIdentity();

        assertThat(reuseIdentity).isNotNull();
        assertThat(httpClientConfig.multipartMode(MultipartMode.BROWSER_COMPATIBLE).reuseIdentity()).isSameAs(reuseIdentity);
        assertThat(httpClientConfig.reuseHttpClientInstance().reuseIdentity()).isSameAs(reuseIdentity);
        assertThat(httpClientConfig.setParam("http.protocol.max-redirects", CUSTOM_MAX_REDIRECTS).reuseIdentity()).isNotNull().isNotSameAs(reuseIdentity);
        assertThat(httpClientConfig.connectTimeout(Duration.ofSeconds(1)).reuseIdentity()).isNotNull().isNotSameAs(reuseIdentity);
        assertThat(new HttpClientConfig().reuseHttpClientInstance().reuseIdentity()).isNotSameAs(reuseIdentity);
        assertThat(httpClientConfig.dontReuseHttpClientInstance().reuseIdentity()).isNull();
        assertThat(new HttpClientConfig().reuseIdentity()).isNull();
    }

    @Test
    public void choosingAClientKeepsTheReusedHttpClientInstance() {
        HttpClientConfig config = new HttpClientConfig().reuseHttpClientInstance();

        assertThat(config.client("jdk").customizeClient(StringBuilder.class, builder -> { }).reuseIdentity()).isSameAs(config.reuseIdentity());
    }

    public abstract static class CustomProvider implements HttpClientProvider {
    }
}
