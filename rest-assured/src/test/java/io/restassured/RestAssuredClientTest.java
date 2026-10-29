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

package io.restassured;

import io.restassured.authentication.NoAuthScheme;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.common.spi.http.ClientSettings;
import io.restassured.common.spi.http.HttpHeader;
import io.restassured.common.spi.http.HttpRequest;
import io.restassured.common.spi.http.HttpResponse;
import io.restassured.common.spi.http.ProxySettings;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.internal.http.client.FakeHttpClients;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.ProxySpecification;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import io.restassured.specification.SpecificationQuerier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.LogConfig.logConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

/**
 * What each setting of {@link RestAssuredClient.Builder} does to the requests of the client, sent with a fake HTTP client.
 */
class RestAssuredClientTest {
    private static final RestAssuredConfig FAKE = config().httpClient(httpClientConfig().client(FakeHttpClients.Other.class));

    private final List<String> filtersRun = new CopyOnWriteArrayList<>();

    @AfterEach
    void reset() {
        FakeHttpClients.reset();
        RestAssured.reset();
    }

    // The defaults

    @Test
    void the_builder_starts_from_the_values_that_reset_sets() {
        RestAssured.reset();
        RestAssuredClient client = RestAssuredClient.builder().build();
        QueryableRequestSpecification fromClient = SpecificationQuerier.query(client.given());
        QueryableRequestSpecification fromStatics = SpecificationQuerier.query(RestAssured.given());

        assertThat(fromClient.getBaseUri()).isEqualTo(fromStatics.getBaseUri()).isEqualTo("http://localhost");
        assertThat(fromClient.getPort()).isEqualTo(fromStatics.getPort());
        assertThat(fromClient.getBasePath()).isEqualTo(fromStatics.getBasePath()).isEmpty();
        assertThat(fromClient.getAuthenticationScheme()).isInstanceOf(NoAuthScheme.class);
        assertThat(fromStatics.getAuthenticationScheme()).isInstanceOf(NoAuthScheme.class);
        assertThat(fromClient.getProxySpecification()).isNull();
        assertThat(fromClient.getDefinedFilters()).isEmpty();
        assertThat(fromClient.getHeaders().exist()).isFalse();
        assertThat(fromClient.getCookies().exist()).isFalse();
        assertThat(fromClient.getConfig().isUserConfigured()).isFalse();
        assertThat(client.config().isUserConfigured()).isFalse();
        assertThat(client.filters()).isEmpty();
    }

    @Test
    void a_request_without_settings_goes_to_localhost_on_port_8080() {
        send(UnaryOperator.identity(), client -> client.get("/path"));

        assertThat(sent().uri()).isEqualTo(URI.create("http://localhost:8080/path"));
    }

    // Each setting reaches the request

    @Test
    void base_uri_port_and_base_path() {
        send(builder -> builder.baseUri("http://example.org").port(1234).basePath("/api"), client -> client.get("/path"));

        assertThat(sent().uri()).isEqualTo(URI.create("http://example.org:1234/api/path"));
    }

    @Test
    void url_encoding() {
        send(builder -> builder.urlEncodingEnabled(false), client -> client.given().queryParam("q", "a%20b").get("http://localhost/path"));

        assertThat(sent().uri().getRawQuery()).isEqualTo("q=a%20b");
    }

    @Test
    void authentication() {
        send(builder -> builder.authentication(RestAssured.preemptive().basic("user", "password")), client -> client.get("http://localhost/path"));

        assertThat(header(sent(), "Authorization")).isEqualTo("Basic dXNlcjpwYXNzd29yZA==");
    }

    @Test
    void the_config() {
        RestAssuredConfig config = config().httpClient(httpClientConfig().client(FakeHttpClients.Other.class).readTimeout(Duration.ofSeconds(3)));

        RestAssuredClient client = RestAssuredClient.builder().config(config).build();
        client.get("http://localhost/path");

        assertThat(sent().options().readTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(client.config()).isSameAs(config);
    }

    @Test
    void root_path() {
        FakeHttpClients.response = request -> json("{\"a\": {\"b\": 1}}");
        RestAssuredClient client = RestAssuredClient.builder().config(FAKE).rootPath("a").build();

        client.get("http://localhost/path").then().body("b", equalTo(1));
        client.expect().body("b", equalTo(1)).when().get("http://localhost/path");
    }

    @Test
    void request_specification() {
        RequestSpecification specification = new RequestSpecBuilder().addHeader("X-Default", "1").addQueryParam("q", "a").build();

        send(builder -> builder.requestSpecification(specification), client -> client.get("http://localhost/path"));

        assertThat(header(sent(), "X-Default")).isEqualTo("1");
        assertThat(sent().uri().getQuery()).isEqualTo("q=a");
    }

    @Test
    void response_specification() {
        RestAssuredClient client = RestAssuredClient.builder().config(FAKE).responseSpecification(new ResponseSpecBuilder().expectStatusCode(201).build()).build();

        assertThatThrownBy(() -> client.get("http://localhost/path")).isInstanceOf(AssertionError.class).hasMessageContaining("Expected status code <201> but was <200>");
    }

    @Test
    void default_parser() {
        FakeHttpClients.response = request -> response("application/custom", "{\"a\": 1}");
        RestAssuredClient client = RestAssuredClient.builder().config(FAKE).defaultParser(Parser.JSON).build();

        client.get("http://localhost/path").then().body("a", equalTo(1));
    }

    @Test
    void registered_and_unregistered_parsers() {
        FakeHttpClients.response = request -> response("application/custom", "{\"a\": 1}");
        RestAssuredClient registered = RestAssuredClient.builder().config(FAKE).registerParser("application/custom", Parser.JSON).build();
        RestAssuredClient unregistered = registered.toBuilder().unregisterParser("application/custom").build();

        registered.get("http://localhost/path").then().body("a", equalTo(1));
        assertThatThrownBy(() -> unregistered.get("http://localhost/path").then().body("a", equalTo(1)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("content-type 'application/custom' is not supported out of the box");
    }

    @Test
    void session_id() {
        RestAssuredClient client = RestAssuredClient.builder().config(FAKE).sessionId("1234").build();
        client.get("http://localhost/path");

        assertThat(header(sent(), "Cookie")).isEqualTo("JSESSIONID=1234");
        assertThat(client.config().getSessionConfig().sessionIdValue()).isEqualTo("1234");
    }

    @Test
    void proxy() {
        assertThat(proxy(builder -> builder.proxy("proxy.example", 1234))).isEqualTo(new ProxySettings("http", "proxy.example", 1234, null, null));
        assertThat(proxy(builder -> builder.proxy("proxy.example"))).isEqualTo(new ProxySettings("http", "proxy.example", 8888, null, null));
        assertThat(proxy(builder -> builder.proxy("https://proxy.example:1235"))).isEqualTo(new ProxySettings("https", "proxy.example", 1235, null, null));
        assertThat(proxy(builder -> builder.proxy(1236))).isEqualTo(new ProxySettings("http", "127.0.0.1", 1236, null, null));
        assertThat(proxy(builder -> builder.proxy("proxy.example", 1237, "https"))).isEqualTo(new ProxySettings("https", "proxy.example", 1237, null, null));
        assertThat(proxy(builder -> builder.proxy(URI.create("http://proxy.example:1238")))).isEqualTo(new ProxySettings("http", "proxy.example", 1238, null, null));
        assertThat(proxy(builder -> builder.proxy(ProxySpecification.host("proxy.example").withPort(1239).withAuth("user", "password"))))
                .isEqualTo(new ProxySettings("http", "proxy.example", 1239, "user", "password"));
    }

    @Test
    void filters_run_in_the_order_they_were_added_and_replacing_them_drops_the_others() {
        send(builder -> builder.filters(recording("1")).filters(recording("2"), recording("3")).filters(List.of(recording("4"))),
                client -> client.get("http://localhost/path"));
        send(builder -> builder.filters(recording("5")).replaceFiltersWith(recording("6"), recording("7")),
                client -> client.get("http://localhost/path"));
        send(builder -> builder.filters(recording("8")).replaceFiltersWith(List.of(recording("9"))),
                client -> client.get("http://localhost/path"));

        assertThat(filtersRun).containsExactly("1", "2", "3", "4", "6", "7", "9");
    }

    @Test
    void relaxed_https_validation() {
        assertThat(clientSettings(builder -> builder.relaxedHTTPSValidation()).tls().relaxedProtocol()).isEqualTo("SSL");
        assertThat(clientSettings(builder -> builder.relaxedHTTPSValidation("TLSv1.2")).tls().relaxedProtocol()).isEqualTo("TLSv1.2");
    }

    @Test
    void key_and_trust_stores() throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        File file = new File("store.p12");

        assertThat(clientSettings(builder -> builder.keyStore("keys.p12", "secret")).tls().keyStore().path()).isEqualTo("keys.p12");
        assertThat(clientSettings(builder -> builder.keyStore(file, "secret")).tls().keyStore().path()).isEqualTo(file);
        assertThat(clientSettings(builder -> builder.keyStore("secret")).tls().keyStore().password()).isEqualTo("secret");
        assertThat(clientSettings(builder -> builder.trustStore("trust.p12", "secret")).tls().trustStore().path()).isEqualTo("trust.p12");
        assertThat(clientSettings(builder -> builder.trustStore(file, "secret")).tls().trustStore().path()).isEqualTo(file);
        assertThat(clientSettings(builder -> builder.trustStore(trustStore)).tls().trustStore().keyStore()).isSameAs(trustStore);
    }

    @Test
    void the_last_setting_wins_as_with_the_static_setters() {
        RestAssuredClient client = RestAssuredClient.builder().relaxedHTTPSValidation().config(FAKE).build();

        assertThat(client.config().getSSLConfig().isUserConfigured()).isFalse();
    }

    @Test
    void logging_if_validation_fails_is_enabled_in_the_config_and_in_the_request_specification_set_before() {
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        RequestSpecification specification = new RequestSpecBuilder().setConfig(FAKE.logConfig(logConfig().defaultStream(new PrintStream(log, true)))).build();
        RestAssuredClient client = RestAssuredClient.builder().config(FAKE).requestSpecification(specification)
                .enableLoggingOfRequestAndResponseIfValidationFails().build();

        assertThatThrownBy(() -> client.get("http://localhost/path").then().statusCode(500)).isInstanceOf(AssertionError.class);

        assertThat(log.toString()).contains("Request method:\tGET");
        assertThat(client.config().getLogConfig().logDetailOfRequestAndResponseIfValidationFails()).isEqualTo(LogDetail.ALL);
        // The builder changed its own copy
        assertThat(SpecificationQuerier.query(specification).getConfig().getLogConfig().isLoggingOfRequestAndResponseIfValidationFailsEnabled()).isFalse();
    }

    // Changes that don't reach a client

    @Test
    void the_specifications_given_to_the_builder_are_copied() {
        RequestSpecification requestSpecification = new RequestSpecBuilder().addHeader("X-Before", "1").build();
        ResponseSpecification responseSpecification = new ResponseSpecBuilder().expectStatusCode(200).build();
        RestAssuredClient client = RestAssuredClient.builder().config(FAKE)
                .requestSpecification(requestSpecification).responseSpecification(responseSpecification).build();

        requestSpecification.header("X-After", "1");
        responseSpecification.statusCode(500);
        client.get("http://localhost/path");

        assertThat(header(sent(), "X-Before")).isEqualTo("1");
        assertThat(header(sent(), "X-After")).isNull();
    }

    @Test
    void a_client_from_the_static_fields_takes_their_values_once() {
        RestAssured.baseURI = "http://example.org";
        RestAssured.port = 1234;
        RestAssured.basePath = "/api";
        RestAssured.config = FAKE;
        RestAssured.filters(recording("static"));
        RestAssured.requestSpecification = new RequestSpecBuilder().addHeader("X-Static", "1").build();
        RestAssured.sessionId = "1234";
        RestAssured.registerParser("application/custom", Parser.JSON);
        RestAssuredClient client = RestAssured.client();

        RestAssured.requestSpecification.header("X-Later", "1");
        RestAssured.filters(recording("later"));
        RestAssured.reset();
        RestAssured.baseURI = "http://other.example.org";
        FakeHttpClients.response = request -> response("application/custom", "{\"a\": 1}");
        client.get("/path").then().body("a", equalTo(1));

        assertThat(sent().uri()).isEqualTo(URI.create("http://example.org:1234/api/path"));
        assertThat(header(sent(), "X-Static")).isEqualTo("1");
        assertThat(header(sent(), "X-Later")).isNull();
        assertThat(header(sent(), "Cookie")).isEqualTo("JSESSIONID=1234");
        assertThat(filtersRun).containsExactly("static");
        assertThat(client.filters()).hasSize(1);
    }

    @Test
    void a_client_from_to_builder_is_independent() {
        RequestSpecification specification = new RequestSpecBuilder().addHeader("X-Default", "1").build();
        RestAssuredClient first = RestAssuredClient.builder().config(FAKE).baseUri("http://first.example.org").requestSpecification(specification).build();
        RestAssuredClient.Builder builder = first.toBuilder();

        RestAssuredClient second = builder.baseUri("http://second.example.org").filters(recording("second"))
                .enableLoggingOfRequestAndResponseIfValidationFails().build();
        builder.baseUri("http://third.example.org");
        first.get("/path");
        HttpRequest fromFirst = sent();
        second.get("/path");

        assertThat(fromFirst.uri().getHost()).isEqualTo("first.example.org");
        assertThat(header(fromFirst, "X-Default")).isEqualTo("1");
        assertThat(sent().uri().getHost()).isEqualTo("second.example.org");
        assertThat(header(sent(), "X-Default")).isEqualTo("1");
        assertThat(filtersRun).containsExactly("second");
        assertThat(first.filters()).isEmpty();
        assertThat(first.config().getLogConfig().isLoggingOfRequestAndResponseIfValidationFailsEnabled()).isFalse();
    }

    // then() uses the client's defaults, not the static fields

    @Test
    void then_validates_with_the_root_path_of_the_client_and_without_the_static_response_specification() {
        FakeHttpClients.response = request -> json("{\"a\": {\"b\": 1}, \"c\": {\"b\": 2}}");
        RestAssured.rootPath = "c";
        RestAssured.responseSpecification = new ResponseSpecBuilder().expectStatusCode(500).build();
        RestAssuredClient client = RestAssuredClient.builder().config(FAKE).rootPath("a").build();

        client.get("http://localhost/path").then().body("b", equalTo(1));
    }

    @Test
    void then_validates_with_the_response_specification_of_the_client() {
        RestAssuredClient client = RestAssuredClient.builder().config(FAKE).responseSpecification(new ResponseSpecBuilder().expectStatusCode(200).build()).build();
        Response response = client.get("http://localhost/path");

        // A copy keeps the defaults of the client that sent the request
        Response created = new ResponseBuilder().clone(response).setStatusCode(201).build();

        assertThatThrownBy(() -> created.then().body(equalTo("ok")))
                .isInstanceOf(AssertionError.class).hasMessageContaining("Expected status code <200> but was <201>");
    }

    @Test
    void then_validates_a_response_that_a_filter_built_with_the_defaults_of_the_client() {
        RestAssured.rootPath = "c";
        RestAssuredClient client = RestAssuredClient.builder().config(FAKE).rootPath("a").build();

        client.given().filter((request, response, context) -> new ResponseBuilder().setStatusCode(200).setContentType(ContentType.JSON)
                .setBody("{\"a\": {\"b\": 1}, \"c\": {\"b\": 2}}").build()).get("http://localhost/path").then().body("b", equalTo(1));
    }

    // Getters and checks

    @Test
    void the_filters_of_a_client_cannot_be_changed() {
        Filter filter = recording("1");
        RestAssuredClient client = RestAssuredClient.builder().filters(filter).build();

        assertThat(client.filters()).containsExactly(filter);
        assertThatThrownBy(() -> client.filters().add(filter)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void specifications_must_be_rest_assureds_own() {
        assertThatThrownBy(() -> RestAssuredClient.builder().requestSpecification(mock(RequestSpecification.class)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("Cannot use a request specification of type")
                .hasMessageEndingWith("it must be of type io.restassured.internal.RequestSpecificationImpl.");
        assertThatThrownBy(() -> RestAssuredClient.builder().responseSpecification(mock(ResponseSpecification.class)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("Cannot use a response specification of type")
                .hasMessageEndingWith("it must be of type io.restassured.internal.ResponseSpecificationImpl.");
    }

    private static void send(UnaryOperator<RestAssuredClient.Builder> settings, Consumer<RestAssuredClient> request) {
        request.accept(settings.apply(RestAssuredClient.builder().config(FAKE)).build());
    }

    private ProxySettings proxy(UnaryOperator<RestAssuredClient.Builder> settings) {
        return clientSettings(settings).proxy();
    }

    // The settings of the HTTP client that sent the request
    private ClientSettings clientSettings(UnaryOperator<RestAssuredClient.Builder> settings) {
        send(settings, client -> client.get("http://localhost/path"));
        return FakeHttpClients.created.get(FakeHttpClients.created.size() - 1).settings;
    }

    private Filter recording(String name) {
        return (request, response, context) -> {
            filtersRun.add(name);
            return context.next(request, response);
        };
    }

    private static HttpRequest sent() {
        return FakeHttpClients.sent.get(FakeHttpClients.sent.size() - 1);
    }

    private static String header(HttpRequest request, String name) {
        return request.headers().stream().filter(header -> header.name().equalsIgnoreCase(name)).map(HttpHeader::value).findFirst().orElse(null);
    }

    private static HttpResponse json(String body) {
        return response("application/json", body);
    }

    private static HttpResponse response(String contentType, String body) {
        byte[] bytes = body.getBytes(UTF_8);
        return new HttpResponse("HTTP/1.1", 200, "OK", List.of(new HttpHeader("Content-Type", contentType)), bytes.length,
                new ByteArrayInputStream(bytes), null);
    }
}
