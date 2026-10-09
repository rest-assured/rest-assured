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

package io.restassured.internal.http;

import com.sun.net.httpserver.HttpServer;
import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.restassured.RestAssured.given;
import static io.restassured.config.EncoderConfig.encoderConfig;
import static io.restassured.config.MultiPartConfig.multiPartConfig;
import static io.restassured.internal.http.EncoderCharacterization.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Characterizes, through the public API, which request body reaches the server for every kind of body and every
 * request content-type.
 */
class RequestBodyEncodingTest {

    private static final String NO_CONTENT_TYPE = "<none>";

    private static final List<String> CONTENT_TYPES = Arrays.asList(
            ContentType.JSON.toString(),
            ContentType.XML.toString(),
            ContentType.TEXT.toString(),
            ContentType.URLENC.toString(),
            ContentType.BINARY.toString(),
            ContentType.HTML.toString(),
            ContentType.ANY.toString(),
            "multipart/form-data",
            NO_CONTENT_TYPE);

    @TempDir
    Path tmpDir;

    private HttpServer server;
    private volatile String received;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            byte[] body = exchange.getRequestBody().readAllBytes();
            received = exchange.getRequestMethod() + " | " + contentType + " | " + escape(body);
            byte[] response = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
        RestAssured.baseURI = "http://127.0.0.1";
        RestAssured.port = server.getAddress().getPort();
        RestAssured.config = RestAssuredConfig.config().multiPartConfig(multiPartConfig().defaultBoundary("BOUNDARY"));
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        RestAssured.reset();
    }

    private Map<String, Function<RequestSpecification, RequestSpecification>> bodies() throws IOException {
        Path file = tmpDir.resolve("body.txt");
        Files.write(file, TEXT.getBytes(StandardCharsets.UTF_8));

        Map<String, Function<RequestSpecification, RequestSpecification>> bodies = new LinkedHashMap<>();
        bodies.put("String", spec -> spec.body(TEXT));
        bodies.put("GString", spec -> spec.body((Object) gString()));
        bodies.put("StringBuilder", spec -> spec.body((Object) new StringBuilder(TEXT)));
        bodies.put("byte[]", spec -> spec.body(TEXT.getBytes(StandardCharsets.UTF_8)));
        bodies.put("InputStream", spec -> spec.body(new ByteArrayInputStream(TEXT.getBytes(StandardCharsets.UTF_8))));
        bodies.put("File", spec -> spec.body(file.toFile()));
        bodies.put("Map", spec -> spec.body(Collections.singletonMap("a", TEXT)));
        bodies.put("Pojo", spec -> spec.body(new Greeting(TEXT)));
        bodies.put("formParams", spec -> spec.formParam("a", "1").formParam("b", TEXT));
        bodies.put("multiPart", spec -> spec.multiPart("file", "file.txt", TEXT.getBytes(StandardCharsets.UTF_8), "text/plain"));
        bodies.put("mapper -> String", spec -> spec.body(new Greeting(TEXT), mapperReturning(() -> TEXT)));
        bodies.put("mapper -> GString", spec -> spec.body(new Greeting(TEXT), mapperReturning(EncoderCharacterization::gString)));
        bodies.put("mapper -> StringBuilder", spec -> spec.body(new Greeting(TEXT), mapperReturning(() -> new StringBuilder(TEXT))));
        bodies.put("mapper -> byte[]", spec -> spec.body(new Greeting(TEXT), mapperReturning(() -> TEXT.getBytes(StandardCharsets.UTF_8))));
        bodies.put("mapper -> Map", spec -> spec.body(new Greeting(TEXT), mapperReturning(() -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("greeting", TEXT);
            map.put("list", Arrays.asList(1, null, true));
            return map;
        })));
        bodies.put("mapper -> List", spec -> spec.body(new Greeting(TEXT), mapperReturning(() -> Arrays.asList(TEXT, 2))));
        bodies.put("mapper -> Closure", spec -> spec.body(new Greeting(TEXT), mapperReturning(EncoderCharacterization::closure)));
        return bodies;
    }

    @Test
    void sends_every_body_type_for_every_content_type_like_before() throws Exception {
        Map<String, Function<RequestSpecification, RequestSpecification>> bodies = bodies();
        StringBuilder out = new StringBuilder();
        for (String contentType : CONTENT_TYPES) {
            for (String method : Arrays.asList("POST", "PUT")) {
                out.append("== ").append(method).append(' ').append(contentType).append('\n');
                for (Map.Entry<String, Function<RequestSpecification, RequestSpecification>> body : bodies.entrySet()) {
                    out.append(body.getKey()).append(" -> ").append(send(method, contentType, body.getValue())).append('\n');
                }
            }
        }
        assertMatchesGoldenFile("request-body-encoding.txt", out.toString());
    }

    @Test
    void custom_content_type_encoded_as_a_predefined_content_type_uses_its_encoder() {
        given().
                config(RestAssured.config().encoderConfig(encoderConfig().encodeContentTypeAs("application/custom", ContentType.JSON))).
                contentType("application/custom").
                body(new Greeting(TEXT), mapperReturning(() -> Collections.singletonMap("a", TEXT))).
        when().
                put("/");

        assertThat(received).isEqualTo("PUT | application/custom; charset=ISO-8859-1 | {\"a\":\"hello \\x5cu00e5\\x5cu00e4\\x5cu00f6\"}");
    }

    @Test
    void url_encoded_body_that_is_not_a_string_throws_illegal_argument_exception() {
        Throwable t = catchThrowable(() -> given().contentType(ContentType.URLENC).body(new byte[]{1}).post("/"));

        assertThat(t).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Don't know how to encode a request body of type byte[] as content-type application/x-www-form-urlencoded; charset=ISO-8859-1. " +
                        "A form url-encoded request body must be a String, use formParam(..) or formParams(..) to send form parameters.");
    }

    @Test
    void closure_returned_by_custom_object_mapper_throws_illegal_argument_exception() {
        Throwable t = catchThrowable(() -> given().contentType(ContentType.JSON).body(new Greeting(TEXT), mapperReturning(EncoderCharacterization::closure)).post("/"));

        assertThat(t).isInstanceOf(IllegalArgumentException.class).hasMessageStartingWith("A Groovy closure").hasMessageContaining("is not supported as request body");
    }

    private String send(String method, String contentType, Function<RequestSpecification, RequestSpecification> body) {
        received = "<nothing sent>";
        try {
            RequestSpecification spec = given();
            if (!NO_CONTENT_TYPE.equals(contentType)) {
                spec.contentType(contentType);
            }
            body.apply(spec).request(method, "/");
            return received;
        } catch (Throwable t) {
            return render(t, tmpDir);
        }
    }

    private static ObjectMapper mapperReturning(Supplier<Object> serialized) {
        return new ObjectMapper() {
            @Override
            public Object deserialize(ObjectMapperDeserializationContext context) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object serialize(ObjectMapperSerializationContext context) {
                return serialized.get();
            }
        };
    }

    public static class Greeting {
        private String greeting;

        public Greeting() {
        }

        Greeting(String greeting) {
            this.greeting = greeting;
        }

        public String getGreeting() {
            return greeting;
        }

        public void setGreeting(String greeting) {
            this.greeting = greeting;
        }
    }
}
