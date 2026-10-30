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

package io.restassured.module.webtestclient;

import io.restassured.filter.log.LogDetail;
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.internal.client.ResponseDefaults;
import io.restassured.internal.support.CommonVersionCheck;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import io.restassured.module.webtestclient.internal.BuilderBasedWebTestClientFactory;
import io.restassured.module.webtestclient.internal.StandaloneWebTestClientFactory;
import io.restassured.module.webtestclient.internal.WebTestClientClientDefaults;
import io.restassured.module.webtestclient.internal.WebTestClientFactory;
import io.restassured.module.webtestclient.internal.WebTestClientRequestSpecificationImpl;
import io.restassured.module.webtestclient.internal.WrapperWebTestClientFactory;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.reactive.function.server.RouterFunction;

import java.util.function.Supplier;

import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Defaults of its own for the requests of {@link RestAssuredWebTestClient}: a WebTestClient instance, base path, config
 * and specifications. A client never reads the static fields of <code>RestAssuredWebTestClient</code>, so tests that run
 * in parallel can each use their own settings. A client sends nothing by itself. Requests start with
 * {@link RestAssuredWebTestClient#given()} as usual, and it picks the client that they use.
 * <h2>Creating a client</h2>
 * <pre>
 * RestAssuredWebTestClientClient api = RestAssuredWebTestClientClient.builder()
 *         .webTestClient(webTestClient).basePath("/api")
 *         .build();
 * </pre>
 * The {@link Builder} starts from the values that {@link RestAssuredWebTestClient#reset()} sets. It has a method for
 * each static field and static setter of <code>RestAssuredWebTestClient</code> except <code>reset()</code>.
 * {@link RestAssuredWebTestClient#client()} creates a client from the current values of the static fields instead.
 * <h2>Using a client</h2>
 * A request uses the first of these that applies:
 * <ol>
 * <li>A client named in {@link RestAssuredWebTestClient#given(RestAssuredWebTestClientClient)}:
 * <pre>
 * given(api).queryParam("name", "John").when().get("/greeting").then().statusCode(200);
 * </pre></li>
 * <li>The client bound to the current thread with
 * {@link RestAssuredWebTestClient#useClient(RestAssuredWebTestClientClient)}, for example for tests that run in parallel
 * and each have their own settings. <code>given()</code>, <code>get(..)</code> and the other static methods that start
 * or send a request then use it, until {@link RestAssuredWebTestClient#resetClient()} or
 * {@link RestAssuredWebTestClient#reset()} unbinds it:
 * <pre>
 * &#64;BeforeEach void useOwnWebTestClient() { RestAssuredWebTestClient.useClient(RestAssuredWebTestClientClient.builder().webTestClient(webTestClient).build()); }
 * &#64;AfterEach void unbindClient() { RestAssuredWebTestClient.resetClient(); }
 *
 * &#64;Test void greets() {
 *     get("/greeting").then().body("content", equalTo("Hello, World!"));
 * }
 * </pre></li>
 * <li>The static settings of <code>RestAssuredWebTestClient</code>, as without a client.</li>
 * </ol>
 * The binding is for the current thread only. A request on another thread uses the static settings unless it names the
 * client with <code>given(client)</code>.
 * <p>
 * The responses of a client don't read {@link io.restassured.RestAssured#rootPath} or
 * {@link io.restassured.RestAssured#responseSpecification} either, which those of the static settings do.
 * <h2>Changing a client</h2>
 * A client can't be changed once it's built. {@link #toBuilder()} gives a builder with its values, which builds a new,
 * independent client:
 * <pre>
 * RestAssuredWebTestClientClient other = api.toBuilder().basePath("/other").build();
 * </pre>
 * <h2>Thread safety</h2>
 * <ul>
 * <li>A client can be used from many threads at once.</li>
 * <li><code>given(client)</code> returns a new specification each time. Don't change one specification from several
 * threads at once. Sending one specification, or one built with
 * {@link io.restassured.module.webtestclient.specification.WebTestClientRequestSpecBuilder}, from many threads is
 * safe.</li>
 * </ul>
 * A specification built with
 * {@link io.restassured.module.webtestclient.specification.WebTestClientRequestSpecBuilder} contains no static settings
 * of <code>RestAssuredWebTestClient</code>, so used with a client, for example with
 * <code>given(client).spec(requestSpec)</code>, it gets the settings of the client.
 */
public final class RestAssuredWebTestClientClient {

    static {
        // First, so that an older rest-assured-common gives a message instead of a NoClassDefFoundError
        CommonVersionCheck.verify();
    }

    private final Supplier<WebTestClientClientDefaults> defaults;
    private final Supplier<ResponseDefaults> responseDefaults;

    // The default client of the static API reads the static fields at each call
    RestAssuredWebTestClientClient(Supplier<WebTestClientClientDefaults> defaults, Supplier<ResponseDefaults> responseDefaults) {
        this.defaults = defaults;
        this.responseDefaults = responseDefaults;
    }

    /**
     * @return A builder that starts from the values that {@link RestAssuredWebTestClient#reset()} sets
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return A builder with the values of this client. The client it builds is independent of this one.
     */
    public Builder toBuilder() {
        return new Builder(defaults.get());
    }

    /**
     * @return The config of this client
     */
    public RestAssuredWebTestClientConfig config() {
        return defaults.get().config();
    }

    // Starts a request of RestAssuredWebTestClient.given(), given(client) and the static methods that send a request
    WebTestClientRequestSpecification given() {
        return defaults.get().given(responseDefaults);
    }

    /**
     * Builds a {@link RestAssuredWebTestClientClient}. It starts from the values that
     * {@link RestAssuredWebTestClient#reset()} sets, and has a method for each static field and static setter of
     * {@link RestAssuredWebTestClient}. As with those, the last value set wins: for example
     * {@link #webTestClient(WebTestClient)} followed by {@link #standaloneSetup(Object...)} uses the controllers. A
     * builder isn't thread safe. It can build several clients, which are independent of each other and of the builder.
     */
    public static final class Builder {
        private WebTestClientFactory webTestClientFactory;
        private RestAssuredWebTestClientConfig config = new RestAssuredWebTestClientConfig();
        private String basePath = "/";
        // Copies that nothing changes, so the clients built share them
        private WebTestClientRequestSpecificationImpl requestSpecification;
        private ResponseSpecificationImpl responseSpecification;

        private Builder() {
        }

        // Copies the specifications, which may be the static ones that users change
        Builder(WebTestClientClientDefaults defaults) {
            webTestClientFactory = defaults.webTestClientFactory();
            if (defaults.config() != null) {
                config = defaults.config();
            }
            basePath = defaults.basePath();
            requestSpecification(defaults.requestSpecification());
            responseSpecification(defaults.responseSpecification());
        }

        /**
         * @param webTestClient The WebTestClient instance, like {@link RestAssuredWebTestClient#webTestClient(WebTestClient)}
         */
        public Builder webTestClient(WebTestClient webTestClient) {
            webTestClientFactory = new WrapperWebTestClientFactory(notNull(webTestClient, WebTestClient.class));
            return this;
        }

        /**
         * Uses a WebTestClient of controllers, {@link org.springframework.test.web.reactive.server.WebTestClientConfigurer}s
         * and {@link org.springframework.web.reactive.function.client.ExchangeFilterFunction}s, like
         * {@link RestAssuredWebTestClient#standaloneSetup(Object...)}.
         */
        public Builder standaloneSetup(Object... controllersOrConfigurersOrExchangeFilterFunctions) {
            webTestClientFactory = StandaloneWebTestClientFactory.of(controllersOrConfigurersOrExchangeFilterFunctions);
            return this;
        }

        /**
         * Uses a WebTestClient of a router function, like
         * {@link RestAssuredWebTestClient#standaloneSetup(RouterFunction, Object...)}.
         */
        public Builder standaloneSetup(RouterFunction routerFunction, Object... configurersOrExchangeFilterFunctions) {
            webTestClientFactory = StandaloneWebTestClientFactory.of(routerFunction, configurersOrExchangeFilterFunctions);
            return this;
        }

        /**
         * Uses the WebTestClient of a builder, like {@link RestAssuredWebTestClient#standaloneSetup(WebTestClient.Builder)}.
         */
        public Builder standaloneSetup(WebTestClient.Builder builder) {
            webTestClientFactory = new BuilderBasedWebTestClientFactory(notNull(builder, WebTestClient.Builder.class));
            return this;
        }

        /**
         * Uses a WebTestClient of a web application context, like
         * {@link RestAssuredWebTestClient#webAppContextSetup(WebApplicationContext, Object...)}.
         */
        public Builder webAppContextSetup(WebApplicationContext context, Object... configurersOrExchangeFilterFunctions) {
            webTestClientFactory = StandaloneWebTestClientFactory.of(context, configurersOrExchangeFilterFunctions);
            return this;
        }

        /**
         * Uses a WebTestClient of an application context, like
         * {@link RestAssuredWebTestClient#applicationContextSetup(ApplicationContext, Object...)}.
         */
        public Builder applicationContextSetup(ApplicationContext context, Object... configurersOrExchangeFilterFunctions) {
            webTestClientFactory = StandaloneWebTestClientFactory.of(context, configurersOrExchangeFilterFunctions);
            return this;
        }

        /**
         * @param basePath The path that the path of each request is appended to, like {@link RestAssuredWebTestClient#basePath}
         */
        public Builder basePath(String basePath) {
            this.basePath = notNull(basePath, "Base path");
            return this;
        }

        /**
         * @param config The config, like {@link RestAssuredWebTestClient#config}
         */
        public Builder config(RestAssuredWebTestClientConfig config) {
            this.config = notNull(config, RestAssuredWebTestClientConfig.class);
            return this;
        }

        /**
         * Sets the request specification that each <code>given()</code> starts from, like
         * {@link RestAssuredWebTestClient#requestSpecification}. The builder takes a copy, so changing
         * <code>requestSpecification</code> later doesn't change the client.
         *
         * @param requestSpecification A specification made by
         *                             {@link io.restassured.module.webtestclient.specification.WebTestClientRequestSpecBuilder}
         *                             or <code>given()</code>, or <code>null</code> for none
         */
        public Builder requestSpecification(WebTestClientRequestSpecification requestSpecification) {
            this.requestSpecification = requestSpecification == null ? null : WebTestClientRequestSpecificationImpl.copyOf(requestSpecification);
            return this;
        }

        /**
         * Sets the response specification that each request is validated with, like
         * {@link RestAssuredWebTestClient#responseSpecification}. The builder takes a copy, so changing
         * <code>responseSpecification</code> later doesn't change the client.
         *
         * @param responseSpecification A specification made by {@link io.restassured.builder.ResponseSpecBuilder} or
         *                              <code>expect()</code>, or <code>null</code> for none
         */
        public Builder responseSpecification(ResponseSpecification responseSpecification) {
            this.responseSpecification = responseSpecification == null ? null : ResponseSpecificationImpl.copyOf(responseSpecification);
            return this;
        }

        /**
         * Logs the request and the response when validation fails, with {@link LogDetail#ALL}, like
         * {@link RestAssuredWebTestClient#enableLoggingOfRequestAndResponseIfValidationFails()}.
         */
        public Builder enableLoggingOfRequestAndResponseIfValidationFails() {
            return enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
        }

        /**
         * Logs the request and the response when validation fails, like
         * {@link RestAssuredWebTestClient#enableLoggingOfRequestAndResponseIfValidationFails(LogDetail)}. Replaces the
         * log config, and also applies to the request specification set so far.
         */
        public Builder enableLoggingOfRequestAndResponseIfValidationFails(LogDetail logDetail) {
            config = config.logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail));
            if (requestSpecification != null) {
                // Changes a copy, since the clients built before share the specification
                WebTestClientRequestSpecificationImpl specification = WebTestClientRequestSpecificationImpl.copyOf(requestSpecification);
                RestAssuredWebTestClientConfig specificationConfig = specification.getRestAssuredWebTestClientConfig();
                specification.config(specificationConfig.logConfig(
                        specificationConfig.getLogConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail)));
                requestSpecification = specification;
            }
            return this;
        }

        /**
         * @return A new client with the values of this builder
         */
        public RestAssuredWebTestClientClient build() {
            WebTestClientClientDefaults defaults = new WebTestClientClientDefaults(webTestClientFactory, config, basePath, requestSpecification,
                    responseSpecification);
            return new RestAssuredWebTestClientClient(() -> defaults, () -> ResponseDefaults.NONE);
        }
    }
}
