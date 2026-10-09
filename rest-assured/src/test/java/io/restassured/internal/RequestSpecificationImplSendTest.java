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

package io.restassured.internal;

import io.restassured.RestAssured;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.SessionConfig;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.internal.filter.RecordingServer;
import io.restassured.internal.filter.RecordingServer.Reply;
import io.restassured.internal.filter.RecordingServer.Request;
import io.restassured.response.Response;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.HttpClientConfig.httpClientConfig;
import static io.restassured.config.RestAssuredConfig.config;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Characterizes what {@link RequestSpecificationImpl} sends: the request line, query, headers, cookies and body for every
 * kind of parameter and body, multipart requests, redirects, proxies, authentication and how errors propagate.
 */
class RequestSpecificationImplSendTest {

    private RecordingServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new RecordingServer();
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = server.port();
        for (String method : List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "PURGE")) {
            server.route(method + " /x", r -> Reply.text("ok"));
            server.route(method + " /x/v 1", r -> Reply.text("ok"));
        }
    }

    @AfterEach
    void stopServer() {
        server.close();
        RestAssured.reset();
    }

    private Request last() {
        return server.requests.get(server.requests.size() - 1);
    }

    @Test
    void get_sends_path_params_query_params_headers_and_cookies() {
        given().header("x", "1").header("x", "2").cookie("a", "1").cookie("b").queryParam("q", "a b").param("p", 1).formParam("f", "ä")
                .get("/x/{a}?z=1", "v 1").then().statusCode(200);

        Request request = last();
        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.rawPath()).isEqualTo("/x/v%201");
        assertThat(request.query()).isEqualTo("p=1&q=a%20b&z=1&f=%C3%A4");
        assertThat(request.header("x")).isEqualTo("1,2");
        assertThat(request.cookies()).isEqualTo("a=1; b=null");
        assertThat(request.header("Accept")).isEqualTo("*/*");
        assertThat(request.header("Content-Type")).isEqualTo("application/x-www-form-urlencoded; charset=ISO-8859-1");
        assertThat(request.body()).isEmpty();
    }

    @Test
    void cookies_overwrite_a_cookie_header() {
        given().header("Cookie", "x=1").cookie("a", "1").get("/x");

        assertThat(last().cookies()).isEqualTo("a=1");
    }

    @Test
    void post_sends_request_params_then_form_params_in_the_body_and_query_params_in_the_uri() {
        given().param("p", "1").formParam("f", "x y").formParam("f", "2").formParam("n").queryParam("q", "1").post("/x").then().statusCode(200);

        Request request = last();
        assertThat(request.query()).isEqualTo("q=1");
        assertThat(request.body()).isEqualTo("p=1&f=x%20y&f=2&n");
        assertThat(request.header("Content-Type")).isEqualTo("application/x-www-form-urlencoded; charset=ISO-8859-1");
    }

    @Test
    void put_sends_form_params_in_the_body_and_request_params_in_the_uri() {
        given().param("p", "1").formParam("f", "2").put("/x").then().statusCode(200);

        Request request = last();
        assertThat(request.query()).isEqualTo("p=1");
        assertThat(request.body()).isEqualTo("f=2");
    }

    @Test
    void form_params_are_encoded_with_the_charset_of_the_content_type() {
        given().formParam("f", "ä").post("/x");
        assertThat(last().body()).isEqualTo("f=%E4");

        given().contentType("application/x-www-form-urlencoded; charset=UTF-8").formParam("f", "ä").post("/x");
        assertThat(last().body()).isEqualTo("f=%C3%A4");

        given().config(config().encoderConfig(encoderConfig().defaultCharsetForContentType("UTF-16BE", ContentType.URLENC))).formParam("f", "a").post("/x");
        assertThat(last().header("Content-Type")).isEqualTo("application/x-www-form-urlencoded; charset=UTF-16BE");
        assertThat(last().body()).isEqualTo("\u0000f\u0000=\u0000a");

        given().urlEncodingEnabled(false).formParam("f", "x y").formParam("g", "1", "2").post("/x");
        assertThat(last().body()).isEqualTo("f=x y&g=1&g=2");
    }

    @Test
    void bodies_with_their_content_types() {
        given().body("text").put("/x");
        assertThat(last().body()).isEqualTo("text");
        assertThat(last().header("Content-Type")).isEqualTo("text/plain; charset=ISO-8859-1");

        given().contentType(ContentType.JSON).body(Map.of("a", 1)).post("/x");
        assertThat(last().body()).isEqualTo("{\"a\":1}");
        assertThat(last().header("Content-Type")).isEqualTo("application/json");

        given().body("abc".getBytes(StandardCharsets.UTF_8)).patch("/x");
        assertThat(last().method()).isEqualTo("PATCH");
        assertThat(last().body()).isEqualTo("abc");
        assertThat(last().header("Content-Type")).isEqualTo("application/octet-stream; charset=ISO-8859-1");

        given().body("d").delete("/x");
        assertThat(last().method()).isEqualTo("DELETE");
        assertThat(last().body()).isEqualTo("d");

        given().body("p").request("purge", "/x");
        assertThat(last().method()).isEqualTo("PURGE");
        assertThat(last().body()).isEqualTo("p");

        assertThatThrownBy(() -> given().contentType(ContentType.JSON).noContentType().body("n").put("/x"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Don't know how to encode n as a byte stream.");
        assertThatThrownBy(() -> given().noContentType().body("n").post("/x"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Don't know how to encode n as a byte stream.");
    }

    @Test
    void patch_with_form_params() {
        given().formParam("a", "1").patch("/x").then().statusCode(200);

        assertThat(last().body()).isEqualTo("a=1");
        assertThat(last().header("Content-Type")).isEqualTo("application/x-www-form-urlencoded; charset=ISO-8859-1");
    }

    @Test
    void head_and_options_have_no_body() {
        Response head = given().head("/x");
        given().options("/x");

        assertThat(head.statusCode()).isEqualTo(200);
        assertThat(server.requestLines()).containsExactly("HEAD /x", "OPTIONS /x");
    }

    @Test
    void multipart_request_with_form_params_converted_to_parts() {
        given().formParam("f", "1", "2").param("p", "3").formParam("n").multiPart("file", "name.txt", "content".getBytes(StandardCharsets.UTF_8), "text/plain")
                .multiPart(new MultiPartSpecBuilder("x").controlName("c").header("X-Part", "y").build())
                .post("/x").then().statusCode(200);

        Request request = last();
        String contentType = request.header("Content-Type");
        assertThat(contentType).startsWith("multipart/form-data; boundary=");
        String boundary = contentType.substring("multipart/form-data; boundary=".length());
        assertThat(boundary).matches("[a-zA-Z0-9_-]{30,40}");
        String part = "--" + boundary + "\n";
        assertThat(request.body().replace("\r\n", "\n")).isEqualTo(
                part + "Content-Disposition: form-data; name=\"file\"; filename=\"name.txt\"\nContent-Type: text/plain\n\ncontent\n" +
                        part + "X-Part: y\nContent-Disposition: form-data; name=\"c\"\nContent-Type: text/plain\n\nx\n" +
                        part + "Content-Disposition: form-data; name=\"p\"\nContent-Type: text/plain\n\n3\n" +
                        part + "Content-Disposition: form-data; name=\"f\"\nContent-Type: text/plain\n\n1\n" +
                        part + "Content-Disposition: form-data; name=\"f\"\nContent-Type: text/plain\n\n2\n" +
                        part + "Content-Disposition: form-data; name=\"n\"\nContent-Type: text/plain\n\n\n" +
                        "--" + boundary + "--\n");
    }

    @Test
    void multipart_request_keeps_the_boundary_and_charset_of_the_content_type() {
        given().contentType("multipart/mixed; charset=UTF-8; boundary=abc").multiPart("a", "b").put("/x").then().statusCode(200);

        Request request = last();
        assertThat(request.header("Content-Type")).isEqualTo("multipart/mixed; boundary=abc; charset=UTF-8");
        assertThat(request.body().replace("\r\n", "\n")).isEqualTo(
                "--abc\nContent-Disposition: form-data; name=\"a\"\nContent-Type: text/plain\n\nb\n--abc--\n");
    }

    @Test
    void multipart_request_with_the_default_boundary_and_a_plus_subtype() {
        given().config(config().multiPartConfig(io.restassured.config.MultiPartConfig.multiPartConfig().defaultBoundary("def")))
                .contentType("application/vnd.x+multipart+related").multiPart("a", "b").put("/x").then().statusCode(200);

        assertThat(last().header("Content-Type")).isEqualTo("application/vnd.x+multipart+related; boundary=\"def\"");
        assertThat(last().body()).startsWith("--def");
    }

    @Test
    void multipart_request_with_an_empty_boundary_gets_a_generated_one() {
        given().contentType("multipart/form-data; boundary=\"\"").multiPart("a", "b").put("/x").then().statusCode(200);

        // The Content-Type header of a multipart request is the one of the multipart entity
        assertThat(last().header("Content-Type")).matches("multipart/form-data; boundary=[a-zA-Z0-9_-]{30,40}");
    }

    @Test
    void multipart_request_with_a_custom_specification_without_headers() {
        io.restassured.specification.MultiPartSpecification custom = new io.restassured.specification.MultiPartSpecification() {
            public Object getContent() {
                return "c";
            }

            public String getControlName() {
                return "cn";
            }

            public String getMimeType() {
                return "text/x";
            }

            public String getCharset() {
                return null;
            }

            public String getFileName() {
                return null;
            }

            public Map<String, String> getHeaders() {
                return null;
            }

            public boolean hasFileName() {
                return false;
            }
        };

        given().multiPart(custom).post("/x").then().statusCode(200);

        assertThat(last().body().replace("\r\n", "\n")).contains("Content-Disposition: form-data; name=\"cn\"\nContent-Type: text/x\n\nc\n");
    }

    @Test
    void multipart_request_with_a_non_multipart_content_type_fails() {
        assertThatThrownBy(() -> given().contentType(ContentType.JSON).multiPart("a", "b").post("/x"))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Content-Type application/json is not valid when using multiparts, it must start with \"multipart/\" or contain \"multipart+\".");
    }

    @Test
    void session_id_from_the_config_is_sent_as_cookie() {
        given().config(config().sessionConfig(new SessionConfig().sessionIdValue("v"))).cookie("a", "1").get("/x");

        assertThat(last().cookies()).isEqualTo("a=1; JSESSIONID=v");
    }

    @Test
    void redirects_are_followed_unless_disabled() {
        server.route("GET /r", r -> Reply.redirect("/x"));

        assertThat(given().get("/r").statusCode()).isEqualTo(200);
        assertThat(server.requestLines()).containsExactly("GET /r", "GET /x");

        assertThat(given().redirects().follow(false).get("/r").statusCode()).isEqualTo(302);
        assertThat(given().config(config().redirect(io.restassured.config.RedirectConfig.redirectConfig().followRedirects(false))).get("/r").statusCode()).isEqualTo(302);
    }

    @Test
    void preemptive_basic_auth_sends_the_authorization_header() {
        given().auth().preemptive().basic("u", "p").get("/x");

        assertThat(last().header("Authorization")).isEqualTo("Basic dTpw");
    }

    @Test
    void requests_go_through_the_proxy() {
        given().proxy("127.0.0.1", server.port()).get("http://example.com/x").then().statusCode(200);

        // The port of RestAssured is used for a fully qualified URL without port
        assertThat(last().header("Host")).isEqualTo("example.com:" + server.port());
        assertThat(last().path()).isEqualTo("/x");
    }

    @Test
    void connection_errors_propagate_as_the_checked_exception() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        assertThatThrownBy(() -> given().port(closedPort).get("/x")).isInstanceOf(ConnectException.class);
    }

    @Test
    void assertion_errors_propagate_unwrapped() {
        assertThatThrownBy(() -> given().expect().statusCode(500).when().get("/x"))
                .isExactlyInstanceOf(AssertionError.class)
                .hasMessage("1 expectation failed.\nExpected status code <500> but was <200>.\n");
        assertThatThrownBy(() -> given().get("/x").then().statusCode(500))
                .isExactlyInstanceOf(AssertionError.class);
    }

    @Test
    void request_and_response_are_logged_if_validation_fails() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream stream = new PrintStream(out, true, StandardCharsets.UTF_8);

        assertThatThrownBy(() -> given().config(config().logConfig(new LogConfig(stream, true))).log().ifValidationFails().queryParam("q", "1")
                .get("/x").then().log().ifValidationFails().statusCode(500)).isInstanceOf(AssertionError.class);

        String log = out.toString(StandardCharsets.UTF_8);
        assertThat(log).startsWith("Request method:\tGET\nRequest URI:\thttp://127.0.0.1:" + server.port() + "/x?q=1\n");
        assertThat(log).contains("HTTP/1.1 200 OK");
    }

    @Test
    void the_timing_filter_sets_the_response_time() {
        Response response = given().get("/x");

        assertThat(response.time()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void a_http_client_that_is_not_an_abstract_http_client_fails_with_a_class_cast_exception() {
        // Quirk: the "!client instanceof AbstractHttpClient" guard never fires, the client is cast instead
        AtomicBoolean filterCalled = new AtomicBoolean();
        Filter filter = (req, res, ctx) -> {
            filterCalled.set(true);
            return ctx.next(req, res);
        };

        assertThatThrownBy(() -> given().config(config().httpClient(httpClientConfig().httpClientFactory(() -> HttpClientBuilder.create().build())))
                .filter(filter).get("/x")).isInstanceOf(ClassCastException.class);
        assertThat(filterCalled).isFalse();
    }

    @Test
    void http_client_params_from_the_config_are_used_but_the_redirect_config_has_precedence() {
        server.route("GET /r", r -> Reply.redirect("/x"));
        HttpClientConfig httpClientConfig = httpClientConfig().setParam(org.apache.http.client.params.ClientPNames.HANDLE_REDIRECTS, false)
                .setParam(org.apache.http.params.CoreProtocolPNames.USER_AGENT, "ua");

        assertThat(given().config(config().httpClient(httpClientConfig)).get("/r").statusCode()).isEqualTo(200);
        assertThat(last().header("User-Agent")).isEqualTo("ua");
    }
}
