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

package io.restassured.module.mockmvc;

import io.restassured.filter.log.LogDetail;
import io.restassured.internal.ResponseSpecificationImpl;
import io.restassured.internal.client.ResponseDefaults;
import io.restassured.internal.support.CommonVersionCheck;
import io.restassured.module.mockmvc.config.RestAssuredMockMvcConfig;
import io.restassured.module.mockmvc.internal.MockMvcClientDefaults;
import io.restassured.module.mockmvc.internal.MockMvcFactory;
import io.restassured.module.mockmvc.internal.MockMvcRequestSpecificationImpl;
import io.restassured.module.mockmvc.internal.StandaloneMockMvcFactory;
import io.restassured.module.mockmvc.specification.MockMvcAuthenticationScheme;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Defaults of its own for the requests of {@link RestAssuredMockMvc}: a MockMvc instance, base path, config, result
 * handlers, post processors, specifications and authentication. A client never reads the static fields of
 * <code>RestAssuredMockMvc</code>, so tests that run in parallel can each use their own settings. A client sends nothing
 * by itself. Requests start with {@link RestAssuredMockMvc#given()} as usual, and it picks the client that they use.
 * <h2>Creating a client</h2>
 * <pre>
 * RestAssuredMockMvcClient api = RestAssuredMockMvcClient.builder()
 *         .mockMvc(mockMvc).basePath("/api")
 *         .authentication(principal(user))
 *         .build();
 * </pre>
 * The {@link Builder} starts from the values that {@link RestAssuredMockMvc#reset()} sets. It has a method for each
 * static field and static setter of <code>RestAssuredMockMvc</code> except <code>reset()</code>.
 * {@link RestAssuredMockMvc#client()} creates a client from the current values of the static fields instead.
 * <h2>Using a client</h2>
 * A request uses the first of these that applies:
 * <ol>
 * <li>A client named in {@link RestAssuredMockMvc#given(RestAssuredMockMvcClient)}:
 * <pre>
 * given(api).param("name", "John").when().get("/greeting").then().statusCode(200);
 * </pre></li>
 * <li>The client bound to the current thread with {@link RestAssuredMockMvc#useClient(RestAssuredMockMvcClient)}, for
 * example for tests that run in parallel and each have their own settings. <code>given()</code>, <code>when()</code>,
 * <code>get(..)</code> and the other static methods that start or send a request then use it, until
 * {@link RestAssuredMockMvc#resetClient()} or {@link RestAssuredMockMvc#reset()} unbinds it:
 * <pre>
 * &#64;BeforeEach void useOwnMockMvc() { RestAssuredMockMvc.useClient(RestAssuredMockMvcClient.builder().mockMvc(mockMvc).build()); }
 * &#64;AfterEach void resetRestAssured() { RestAssuredMockMvc.reset(); }
 *
 * &#64;Test void greets() {
 *     get("/greeting").then().body("content", equalTo("Hello, World!"));
 * }
 * </pre></li>
 * <li>The static settings of <code>RestAssuredMockMvc</code>, as without a client.</li>
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
 * RestAssuredMockMvcClient admin = api.toBuilder().authentication(principal(admin)).build();
 * </pre>
 * <h2>Thread safety</h2>
 * <ul>
 * <li>A client can be used from many threads at once. Its requests share its MockMvc instance, which is built once if
 * the client was given a builder or controllers.</li>
 * <li><code>given(client)</code> returns a new specification each time. Don't change one specification from several
 * threads at once.</li>
 * <li>The result handlers and post processors of the client are used by all its requests and must be thread safe.</li>
 * </ul>
 * A specification built with {@link io.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder} contains no
 * static settings of <code>RestAssuredMockMvc</code>, so used with a client, for example with
 * <code>given(client).spec(requestSpec)</code>, it gets the settings of the client.
 */
public final class RestAssuredMockMvcClient {

    static {
        // First, so that an older rest-assured-common gives a message instead of a NoClassDefFoundError
        CommonVersionCheck.verify();
    }

    private final Supplier<MockMvcClientDefaults> defaults;
    private final Supplier<ResponseDefaults> responseDefaults;

    // The default client of the static API reads the static fields at each call
    RestAssuredMockMvcClient(Supplier<MockMvcClientDefaults> defaults, Supplier<ResponseDefaults> responseDefaults) {
        this.defaults = defaults;
        this.responseDefaults = responseDefaults;
    }

    /**
     * @return A builder that starts from the values that {@link RestAssuredMockMvc#reset()} sets
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
    public RestAssuredMockMvcConfig config() {
        return defaults.get().config();
    }

    /**
     * @return The result handlers of this client (read-only)
     */
    public List<ResultHandler> resultHandlers() {
        return defaults.get().resultHandlers();
    }

    /**
     * @return The request post processors of this client (read-only)
     */
    public List<RequestPostProcessor> postProcessors() {
        return defaults.get().requestPostProcessors();
    }

    // Starts a request of RestAssuredMockMvc.given(), given(client) and the static methods that send a request
    MockMvcRequestSpecification given() {
        return defaults.get().given(responseDefaults);
    }

    /**
     * Builds a {@link RestAssuredMockMvcClient}. It starts from the values that {@link RestAssuredMockMvc#reset()} sets,
     * and has a method for each static field and static setter of {@link RestAssuredMockMvc}. As with those, the last
     * value set wins: for example {@link #mockMvc(MockMvc)} followed by {@link #standaloneSetup(Object...)} uses the
     * controllers. A builder isn't thread safe. It can build several clients, which are independent of each other and of
     * the builder.
     */
    public static final class Builder {
        private MockMvcFactory mockMvcFactory;
        private RestAssuredMockMvcConfig config = new RestAssuredMockMvcConfig();
        private final List<ResultHandler> resultHandlers = new ArrayList<>();
        private final List<RequestPostProcessor> postProcessors = new ArrayList<>();
        private String basePath = "/";
        // Copies that nothing changes, so the clients built share them
        private MockMvcRequestSpecificationImpl requestSpecification;
        private ResponseSpecificationImpl responseSpecification;
        private MockMvcAuthenticationScheme authentication;

        private Builder() {
        }

        // Copies the specifications, which may be the static ones that users change
        Builder(MockMvcClientDefaults defaults) {
            mockMvcFactory = defaults.mockMvcFactory();
            if (defaults.config() != null) {
                config = defaults.config();
            }
            resultHandlers.addAll(defaults.resultHandlers());
            postProcessors.addAll(defaults.requestPostProcessors());
            basePath = defaults.basePath();
            requestSpecification(defaults.requestSpecification());
            responseSpecification(defaults.responseSpecification());
            authentication = defaults.authentication();
        }

        /**
         * @param mockMvc The MockMvc instance, like {@link RestAssuredMockMvc#mockMvc(MockMvc)}
         */
        public Builder mockMvc(MockMvc mockMvc) {
            mockMvcFactory = new MockMvcFactory(notNull(mockMvc, MockMvc.class));
            return this;
        }

        /**
         * Uses a MockMvc instance of controllers and {@link MockMvcConfigurer}s, like
         * {@link RestAssuredMockMvc#standaloneSetup(Object...)}. The clients built share the instance, which is built
         * at the first request.
         */
        public Builder standaloneSetup(Object... controllersOrMockMvcConfigurers) {
            mockMvcFactory = StandaloneMockMvcFactory.of(controllersOrMockMvcConfigurers);
            return this;
        }

        /**
         * Uses the MockMvc instance of a builder, like {@link RestAssuredMockMvc#standaloneSetup(MockMvcBuilder)}. The
         * clients built share the instance, which is built at the first request.
         */
        public Builder standaloneSetup(MockMvcBuilder builder) {
            mockMvcFactory = new MockMvcFactory(notNull(builder, MockMvcBuilder.class));
            return this;
        }

        /**
         * Uses a MockMvc instance of a web application context, like
         * {@link RestAssuredMockMvc#webAppContextSetup(WebApplicationContext, MockMvcConfigurer...)}. The clients built
         * share the instance, which is built at the first request.
         */
        public Builder webAppContextSetup(WebApplicationContext context, MockMvcConfigurer... mockMvcConfigurers) {
            mockMvcFactory = MockMvcFactory.webAppContextSetup(context, mockMvcConfigurers);
            return this;
        }

        /**
         * @param basePath The path that the path of each request is appended to, like {@link RestAssuredMockMvc#basePath}
         */
        public Builder basePath(String basePath) {
            this.basePath = notNull(basePath, "Base path");
            return this;
        }

        /**
         * @param config The config, like {@link RestAssuredMockMvc#config}
         */
        public Builder config(RestAssuredMockMvcConfig config) {
            this.config = notNull(config, RestAssuredMockMvcConfig.class);
            return this;
        }

        /**
         * Adds result handlers that run after each request, like
         * {@link RestAssuredMockMvc#resultHandlers(ResultHandler, ResultHandler...)}.
         */
        public Builder resultHandlers(ResultHandler resultHandler, ResultHandler... additionalResultHandlers) {
            resultHandlers.addAll(asList(notNull(resultHandler, ResultHandler.class), additionalResultHandlers));
            return this;
        }

        /**
         * Adds post processors that apply to each request, like
         * {@link RestAssuredMockMvc#postProcessors(RequestPostProcessor, RequestPostProcessor...)}.
         */
        public Builder postProcessors(RequestPostProcessor postProcessor, RequestPostProcessor... additionalPostProcessors) {
            postProcessors.addAll(asList(notNull(postProcessor, RequestPostProcessor.class), additionalPostProcessors));
            return this;
        }

        @SafeVarargs
        private static <T> List<T> asList(T first, T... additional) {
            List<T> list = new ArrayList<>();
            list.add(first);
            if (additional != null) {
                Collections.addAll(list, additional);
            }
            return list;
        }

        /**
         * Sets the request specification that each <code>given()</code> starts from, like
         * {@link RestAssuredMockMvc#requestSpecification}. The builder takes a copy, so changing
         * <code>requestSpecification</code> later doesn't change the client.
         *
         * @param requestSpecification A specification made by
         *                             {@link io.restassured.module.mockmvc.specification.MockMvcRequestSpecBuilder} or
         *                             <code>given()</code>, or <code>null</code> for none
         */
        public Builder requestSpecification(MockMvcRequestSpecification requestSpecification) {
            this.requestSpecification = requestSpecification == null ? null : MockMvcRequestSpecificationImpl.copyOf(requestSpecification);
            return this;
        }

        /**
         * Sets the response specification that each request is validated with, like
         * {@link RestAssuredMockMvc#responseSpecification}. The builder takes a copy, so changing
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
         * @param authentication The authentication scheme, such as {@link RestAssuredMockMvc#principal(Object)}, like
         *                       {@link RestAssuredMockMvc#authentication}, or <code>null</code> for none
         */
        public Builder authentication(MockMvcAuthenticationScheme authentication) {
            this.authentication = authentication;
            return this;
        }

        /**
         * Logs the request and the response when validation fails, with {@link LogDetail#ALL}, like
         * {@link RestAssuredMockMvc#enableLoggingOfRequestAndResponseIfValidationFails()}.
         */
        public Builder enableLoggingOfRequestAndResponseIfValidationFails() {
            return enableLoggingOfRequestAndResponseIfValidationFails(LogDetail.ALL);
        }

        /**
         * Logs the request and the response when validation fails, like
         * {@link RestAssuredMockMvc#enableLoggingOfRequestAndResponseIfValidationFails(LogDetail)}. Replaces the log
         * config, and also applies to the request specification set so far.
         */
        public Builder enableLoggingOfRequestAndResponseIfValidationFails(LogDetail logDetail) {
            config = config.logConfig(logConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail));
            if (requestSpecification != null) {
                // Changes a copy, since the clients built before share the specification
                MockMvcRequestSpecificationImpl specification = MockMvcRequestSpecificationImpl.copyOf(requestSpecification);
                RestAssuredMockMvcConfig specificationConfig = specification.getRestAssuredMockMvcConfig();
                specification.config(specificationConfig.logConfig(
                        specificationConfig.getLogConfig().enableLoggingOfRequestAndResponseIfValidationFails(logDetail)));
                requestSpecification = specification;
            }
            return this;
        }

        /**
         * @return A new client with the values of this builder
         */
        public RestAssuredMockMvcClient build() {
            MockMvcClientDefaults defaults = new MockMvcClientDefaults(mockMvcFactory, config, Collections.unmodifiableList(new ArrayList<>(resultHandlers)),
                    Collections.unmodifiableList(new ArrayList<>(postProcessors)), basePath, requestSpecification, responseSpecification, authentication);
            return new RestAssuredMockMvcClient(() -> defaults, () -> ResponseDefaults.NONE);
        }
    }
}
