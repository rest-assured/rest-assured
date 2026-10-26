/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.internal.http.client;

import io.restassured.common.spi.http.ClientCustomizer;
import io.restassured.common.spi.http.UnsupportedFeatureException;
import io.restassured.internal.http.client.HttpClientProviders.How;
import io.restassured.internal.http.client.HttpClientProviders.Selection;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnsupportedFeaturesTest {
    private static final UnsupportedFeatureException HOST_HEADER = new UnsupportedFeatureException("send a Host header",
            "start the JVM with -Djdk.httpclient.allowRestrictedHeaders=host");

    @Test
    void the_default_client_suggests_http_client_apache5() {
        assertThat(UnsupportedFeatures.translate(new Selection(new FakeHttpClients.Jdk(), How.DEFAULT, null), HOST_HEADER))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasCause(HOST_HEADER)
                .hasMessage("The JDK HTTP client (the default module, http-client-jdk) can't send a Host header. Add io.rest-assured:http-client-apache5 "
                        + "to your test dependencies and REST Assured uses it, or start the JVM with -Djdk.httpclient.allowRestrictedHeaders=host.");
    }

    @Test
    void a_client_found_on_the_classpath_suggests_choosing_another() {
        assertThat(UnsupportedFeatures.translate(new Selection(new FakeHttpClients.Apache5(), How.CLASSPATH, null), new UnsupportedFeatureException("do this", null)))
                .hasMessage("Apache HttpClient 5 (found on the classpath, http-client-apache5) can't do this. Choose another client with httpClientConfig().client(..).");
    }

    @Test
    void a_chosen_client_says_how_it_was_chosen() {
        assertThat(UnsupportedFeatures.translate(new Selection(new FakeHttpClients.Jdk(), How.SYSTEM_PROPERTY, "the system property restassured.http.client=jdk"), HOST_HEADER))
                .hasMessage("The JDK HTTP client (chosen with the system property restassured.http.client=jdk) can't send a Host header. Choose another client, "
                        + "or start the JVM with -Djdk.httpclient.allowRestrictedHeaders=host.");
    }

    @Test
    void what_only_http_client_4_does_says_how_to_use_it() {
        Selection byConfig = new Selection(new FakeHttpClients.Apache5(), How.CONFIG, "httpClientConfig().client(APACHE5)");
        Selection byProperty = new Selection(new FakeHttpClients.Apache5(), How.SYSTEM_PROPERTY, "the system property restassured.http.client=apache5");
        Selection onTheClasspath = new Selection(new FakeHttpClients.Apache5(), How.CLASSPATH, null);

        assertThat(UnsupportedFeatures.httpClient4Only(byConfig, "use the HttpClient 4 SSLSocketFactory of certificate authentication",
                "give certificate authentication an SSLContext"))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Apache HttpClient 5 (chosen with httpClientConfig().client(APACHE5)) can't use the HttpClient 4 SSLSocketFactory of "
                        + "certificate authentication. Remove httpClientConfig().client(..) to use Apache HttpClient 4, or give certificate "
                        + "authentication an SSLContext.");
        assertThat(UnsupportedFeatures.httpClient4Only(byProperty, "do this", "do that"))
                .hasMessage("Apache HttpClient 5 (chosen with the system property restassured.http.client=apache5) can't do this. "
                        + "Remove the system property restassured.http.client to use Apache HttpClient 4, or do that.");
        assertThat(UnsupportedFeatures.httpClient4Only(onTheClasspath, "do this", "do that"))
                .hasMessage("Apache HttpClient 5 (found on the classpath, http-client-apache5) can't do this. "
                        + "Remove io.rest-assured:http-client-apache5 from the classpath to use Apache HttpClient 4, or do that.");
    }

    @Test
    void a_customizer_for_the_builder_of_the_chosen_client_or_a_supertype_is_accepted() {
        Selection jdk = new Selection(new FakeHttpClients.Jdk(), How.DEFAULT, null);

        assertThatNoException().isThrownBy(() -> UnsupportedFeatures.verifyCustomizers(jdk, List.of(
                new ClientCustomizer(HttpClient.Builder.class, builder -> { }), new ClientCustomizer(Object.class, builder -> { }))));
    }

    @Test
    void a_customizer_for_another_client_fails() {
        Selection jdk = new Selection(new FakeHttpClients.Jdk(), How.DEFAULT, null);

        assertThatThrownBy(() -> UnsupportedFeatures.verifyCustomizers(jdk, List.of(new ClientCustomizer(FakeHttpClients.Apache5Builder.class, builder -> { }))))
                .isExactlyInstanceOf(UnsupportedOperationException.class)
                .hasMessage("A customizer for " + FakeHttpClients.Apache5Builder.class.getName() + " is configured, but REST Assured uses the JDK HTTP client.");
    }

    @Test
    void a_customizer_fails_for_a_client_that_cant_be_customized() {
        Selection okHttp = new Selection(new FakeHttpClients.OkHttp(), How.CONFIG, "httpClientConfig().client(\"okhttp\")");

        assertThatThrownBy(() -> UnsupportedFeatures.verifyCustomizers(okHttp, List.of(new ClientCustomizer(Object.class, builder -> { }))))
                .hasMessage("A customizer for java.lang.Object is configured, but REST Assured uses OkHttp, which can't be customized.");
    }

    @Test
    void a_customizer_fails_for_http_client_4() {
        assertThatThrownBy(() -> UnsupportedFeatures.verifyCustomizers(new Selection(null, How.DEFAULT, null),
                List.of(new ClientCustomizer(HttpClient.Builder.class, builder -> { }))))
                .hasMessage("A customizer for java.net.http.HttpClient$Builder is configured, but REST Assured uses Apache HttpClient 4, "
                        + "which you customize with httpClientConfig().httpClientFactory(..).");
    }
}
