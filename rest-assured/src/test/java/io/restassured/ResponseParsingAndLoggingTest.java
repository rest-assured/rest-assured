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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.restassured.config.JsonConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.XmlConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Characterizes response body parsing for body assertions, and pretty printing in request and response logging,
 * end to end against a local HTTP server.
 */
class ResponseParsingAndLoggingTest {

    private static final String NL = System.lineSeparator();
    private static final String NAMESPACE_XML = "<foo xmlns:ns=\"http://localhost/\"><bar>sudo </bar><ns:bar>make me a sandwich!</ns:bar></foo>";
    private static final String DTD_XML = "<?xml version=\"1.0\"?><!DOCTYPE greeting [<!ELEMENT greeting (#PCDATA)>]><greeting>Hello</greeting>";
    private static final String HTML = "<html><head><title>T</title></head><body><p>hello</p></body></html>";

    private HttpServer server;
    private final ByteArrayOutputStream log = new ByteArrayOutputStream();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/json", exchange -> send(exchange, "application/json", "{\"n\":1.5,\"s\":\"x\"}"));
        server.createContext("/colon-json", exchange -> send(exchange, "application/json", "{\"message\":{\"ErrorCode:\":0,\"ErrorMsg:\":\"Success\"}}"));
        server.createContext("/xml", exchange -> send(exchange, "application/xml", NAMESPACE_XML));
        server.createContext("/dtd", exchange -> send(exchange, "application/xml", DTD_XML));
        server.createContext("/html", exchange -> send(exchange, "text/html", HTML));
        server.createContext("/empty", exchange -> send(exchange, "application/json", ""));
        server.createContext("/echo", exchange -> send(exchange, "text/plain", "ok"));
        server.start();
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        RestAssured.reset();
    }

    // Body assertions

    @Test
    void json_body_assertions_after_the_request() {
        given().get("/json").then().body("n", equalTo(1.5f)).body("s", equalTo("x"));
    }

    @Test
    void json_body_assertions_before_the_request() {
        given().expect().body("n", equalTo(1.5f)).body("s", equalTo("x")).when().get("/json");
    }

    @Test
    void json_number_return_type_is_configurable() {
        RestAssuredConfig config = RestAssuredConfig.config().jsonConfig(JsonConfig.jsonConfig().numberReturnType(NumberReturnType.BIG_DECIMAL));

        given().config(config).get("/json").then().body("n", equalTo(new BigDecimal("1.5")));
        given().config(config).expect().body("n", equalTo(new BigDecimal("1.5"))).when().get("/json");
    }

    @Test
    void json_body_assertions_on_keys_that_contain_a_colon() {
        given().get("/colon-json").then().body("message.ErrorCode:", equalTo(0)).body("message.ErrorMsg:", equalTo("Success"));
        given().expect().rootPath("message").body("ErrorCode:", equalTo(0)).when().get("/colon-json");
        assertThat(given().get("/colon-json").jsonPath().getInt("message.ErrorCode:")).isZero();
    }

    @Test
    void xml_body_assertions_with_declared_namespace() {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig().declareNamespace("ns", "http://localhost/"));

        given().config(config).get("/xml").then().body("foo.ns:bar.text()", equalTo("make me a sandwich!"));
        given().config(config).expect().body("foo.ns:bar.text()", equalTo("make me a sandwich!")).when().get("/xml");
    }

    @Test
    void xml_body_assertions_without_declared_namespace() {
        given().get("/xml").then().body("foo.bar.text()", equalTo("sudo make me a sandwich!")).body("foo.ns:bar.text()", equalTo(""));
    }

    @Test
    void doctype_is_rejected_with_the_original_sax_exception_after_the_request() {
        Response response = given().get("/dtd");

        assertThatThrownBy(() -> response.then().body("greeting", equalTo("Hello")))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining("DOCTYPE");
    }

    @Test
    void doctype_is_rejected_with_the_original_sax_exception_before_the_request() {
        assertThatThrownBy(() -> given().expect().body("greeting", equalTo("Hello")).when().get("/dtd"))
                .isInstanceOf(SAXParseException.class)
                .hasMessageContaining("DOCTYPE");
    }

    @Test
    void doctype_is_accepted_when_allowed() {
        RestAssuredConfig config = RestAssuredConfig.config().xmlConfig(XmlConfig.xmlConfig().allowDocTypeDeclaration(true).validating(true));

        given().config(config).get("/dtd").then().body("greeting", equalTo("Hello"));
        given().config(config).expect().body("greeting", equalTo("Hello")).when().get("/dtd");
    }

    @Test
    void html_body_assertions() {
        given().get("/html").then().body("html.head.title", equalTo("T")).body("html.body.p", equalTo("hello"));
        given().expect().body("html.head.title", equalTo("T")).when().get("/html");
    }

    // The response content handed to the response object is always an InputStream (non-empty entity) or "" (empty entity).

    @Test
    void empty_response_body_with_body_expectations() {
        Response response = given().expect().body(emptyString()).when().get("/empty");

        assertThat(response.asString()).isEmpty();
        assertThat(response.asByteArray()).isEmpty();
    }

    @Test
    void non_empty_response_body_with_body_expectations_can_be_read_again() {
        Response response = given().expect().body("s", equalTo("x")).when().get("/json");

        assertThat(response.asString()).isEqualTo("{\"n\":1.5,\"s\":\"x\"}");
        assertThat(response.jsonPath().getString("s")).isEqualTo("x");
    }

    @Test
    void head_request_with_body_expectations() {
        Response response = given().expect().body(emptyString()).when().head("/json");

        assertThat(response.asString()).isEmpty();
    }

    // Request logging

    @Test
    void request_log_renders_byte_array_body_like_groovy() {
        given().config(logTo()).log().body().body(new byte[]{65, 66}).post("/echo");

        assertThat(log()).isEqualTo("Body:" + NL + "[65, 66]" + NL);
    }

    @Test
    void request_log_without_pretty_printing_renders_byte_array_body_with_java_to_string() {
        given().config(logTo()).log().body(false).body(new byte[]{65, 66}).post("/echo");

        assertThat(log()).startsWith("Body:" + NL + "[B@");
    }

    @Test
    void request_log_renders_map_body_from_custom_object_mapper_like_groovy() {
        Map<String, Object> mapped = new LinkedHashMap<>();
        mapped.put("a", 1);
        mapped.put("b", new byte[]{65});

        given().config(logTo()).log().body().contentType(ContentType.JSON).body(new Object(), mapperReturning(mapped)).post("/echo");

        assertThat(log()).isEqualTo("Body:" + NL + "[a:1, b:[65]]" + NL);
    }

    @Test
    void request_log_pretty_prints_json_body() {
        given().config(logTo()).log().body().contentType(ContentType.JSON).body("{\"a\":1}").post("/echo");

        assertThat(log()).isEqualTo("Body:" + NL + "{" + NL + "    \"a\": 1" + NL + "}" + NL);
    }

    @Test
    void request_log_pretty_prints_xml_body() {
        given().config(logTo()).log().body().contentType(ContentType.XML).body("<a><b>x</b></a>").post("/echo");

        assertThat(log()).isEqualTo("Body:" + NL + "<a>" + NL + "  <b>x</b>" + NL + "</a>" + NL);
    }

    @Test
    void request_log_pretty_prints_html_body() {
        given().config(logTo()).log().body().contentType(ContentType.HTML).body("<html><body><p>x</p></body></html>").post("/echo");

        assertThat(log()).isEqualTo("Body:" + NL + "<html>" + NL + "  <body>" + NL + "    <p>x</p>" + NL + "  </body>" + NL + "</html>" + NL);
    }

    @Test
    void request_log_renders_byte_array_multipart_content_like_groovy() {
        given().config(logTo()).log().all().multiPart("file", "file.bin", new byte[]{65, 66}).post("/echo");

        assertThat(log()).contains("Content-Type: application/octet-stream" + NL + NL + "\t\t\t\t[65, 66]" + NL);
    }

    @Test
    void request_log_pretty_prints_byte_array_multipart_content_with_json_mime_type() {
        given().config(logTo()).log().all().multiPart("file", "file.json", new byte[]{65, 66}, "application/json").post("/echo");

        assertThat(log()).contains("Content-Type: application/json" + NL + NL
                + "\t\t\t\t[" + NL + "\t\t\t\t    65," + NL + "\t\t\t\t    66" + NL + "\t\t\t\t]" + NL);
    }

    // Response logging and pretty printing

    @Test
    void response_log_pretty_prints_json() {
        given().config(logTo()).get("/json").then().log().body().statusCode(200);

        assertThat(log()).isEqualTo("{" + NL + "    \"n\": 1.5," + NL + "    \"s\": \"x\"" + NL + "}" + NL);
    }

    @Test
    void response_pretty_string_for_each_content_type() {
        assertThat(given().get("/json").asPrettyString()).isEqualTo("{" + NL + "    \"n\": 1.5," + NL + "    \"s\": \"x\"" + NL + "}");
        assertThat(given().get("/xml").asPrettyString()).isEqualTo(
                "<foo xmlns:ns=\"http://localhost/\">" + NL + "  <bar>sudo </bar>" + NL + "  <ns:bar>make me a sandwich!</ns:bar>" + NL + "</foo>");
        assertThat(given().get("/html").asPrettyString()).isEqualTo(
                "<html>" + NL + "  <head>" + NL + "    <title>T</title>" + NL + "  </head>" + NL + "  <body>" + NL + "    <p>hello</p>" + NL + "  </body>" + NL + "</html>");
        assertThat(given().get("/dtd").asPrettyString()).isEqualTo(DTD_XML);
        assertThat(given().get("/echo").asPrettyString()).isEqualTo("ok");
    }

    private RestAssuredConfig logTo() {
        return RestAssuredConfig.config().logConfig(LogConfig.logConfig().defaultStream(new PrintStream(log, true)));
    }

    private String log() {
        return new String(log.toByteArray(), StandardCharsets.UTF_8);
    }

    private static ObjectMapper mapperReturning(Object serialized) {
        return new ObjectMapper() {
            @Override
            public Object deserialize(ObjectMapperDeserializationContext context) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object serialize(ObjectMapperSerializationContext context) {
                return serialized;
            }
        };
    }

    private static void send(HttpExchange exchange, String contentType, String body) throws IOException {
        exchange.getRequestBody().readAllBytes();
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if ("HEAD".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(200, -1);
        } else {
            exchange.sendResponseHeaders(200, bytes.length == 0 ? -1 : bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
        exchange.close();
    }
}
