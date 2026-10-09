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
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.QueryableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.restassured.RestAssured.given;
import static io.restassured.config.MultiPartConfig.multiPartConfig;
import static io.restassured.internal.http.EncoderCharacterization.*;

/**
 * Characterizes, through the public API, every way to add a multipart: which multipart specifications a filter sees,
 * what the specification looks like after the request, and the multipart request that reaches the server.
 */
class MultiPartRequestCharacterizationTest {

    private static final byte[] BYTES = TEXT.getBytes(StandardCharsets.UTF_8);

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
            received = contentType + " | " + escape(body);
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

    private Map<String, Function<RequestSpecification, RequestSpecification>> multiParts() throws IOException {
        File file = tmpDir.resolve("file.txt").toFile();
        Files.write(file.toPath(), BYTES);

        Map<String, Function<RequestSpecification, RequestSpecification>> cases = new LinkedHashMap<>();
        // RequestSpecificationImpl.multiPart(..) overloads, one per MultiPartInternal named-argument site
        cases.put("multiPart(File)", spec -> spec.multiPart(file));
        cases.put("multiPart(String, File)", spec -> spec.multiPart("ctrl", file));
        cases.put("multiPart(String, File, String)", spec -> spec.multiPart("ctrl", file, "application/xml"));
        cases.put("multiPart(String, Object)", spec -> spec.multiPart("ctrl", new RequestBodyEncodingTest.Greeting(TEXT)));
        cases.put("multiPart(String, Object) with a String, multipart/mixed", spec -> spec.contentType("multipart/mixed").multiPart("ctrl", (Object) TEXT));
        cases.put("multiPart(String, Object, String)", spec -> spec.multiPart("ctrl", new RequestBodyEncodingTest.Greeting(TEXT), "application/json"));
        cases.put("multiPart(String, String, Object, String)", spec -> spec.multiPart("ctrl", "greeting.json", new RequestBodyEncodingTest.Greeting(TEXT), "application/json"));
        cases.put("multiPart(String, String, byte[])", spec -> spec.multiPart("ctrl", "file.bin", BYTES));
        cases.put("multiPart(String, String, byte[], String)", spec -> spec.multiPart("ctrl", "file.pdf", BYTES, "application/pdf"));
        cases.put("multiPart(String, String, InputStream)", spec -> spec.multiPart("ctrl", "file.bin", stream()));
        cases.put("multiPart(String, String, InputStream, String)", spec -> spec.multiPart("ctrl", "file.csv", stream(), "text/csv"));
        cases.put("multiPart(String, String)", spec -> spec.multiPart("ctrl", TEXT));
        cases.put("multiPart(String, String, String)", spec -> spec.multiPart("ctrl", "<a/>", "text/xml"));
        cases.put("form param without value (NoParameterValue)", spec -> spec.formParam("novalue").multiPart("ctrl", TEXT));
        cases.put("form params and params", spec -> spec.param("p", "1").formParam("f", "2", "3").multiPart("ctrl", TEXT));
        // RequestSpecificationImpl.multiPart(MultiPartSpecification)
        cases.put("spec String content", spec -> spec.multiPart(new MultiPartSpecBuilder(TEXT).build()));
        cases.put("spec byte[] content, all set", spec -> spec.multiPart(new MultiPartSpecBuilder(BYTES).controlName("ctrl").fileName("file.bin")
                .mimeType("application/pdf").header("X-B", "b").header("X-A", "a").build()));
        cases.put("spec String content, all set", spec -> spec.multiPart(new MultiPartSpecBuilder(TEXT).controlName("ctrl").fileName("file.txt")
                .mimeType("text/plain").charset(StandardCharsets.UTF_8).headers(Collections.singletonMap("X-A", "a")).build()));
        cases.put("spec InputStream content", spec -> spec.multiPart(new MultiPartSpecBuilder(stream()).fileName("in.bin").build()));
        cases.put("spec File content", spec -> spec.multiPart(new MultiPartSpecBuilder(file).controlName("ctrl").build()));
        cases.put("spec File content with charset", spec -> spec.multiPart(new MultiPartSpecBuilder(file).mimeType("text/plain").charset("ISO-8859-1").build()));
        cases.put("spec Object content", spec -> spec.multiPart(new MultiPartSpecBuilder(new RequestBodyEncodingTest.Greeting(TEXT)).build()));
        cases.put("spec Object content, object mapper type", spec -> spec.multiPart(new MultiPartSpecBuilder(new RequestBodyEncodingTest.Greeting(TEXT), ObjectMapperType.GSON).build()));
        cases.put("spec Object content, object mapper", spec -> spec.multiPart(new MultiPartSpecBuilder(new RequestBodyEncodingTest.Greeting(TEXT), mapperReturning("mapped")).build()));
        cases.put("spec mime-type with parameters and charset", spec -> spec.multiPart(new MultiPartSpecBuilder("<a/>").mimeType("application/xml; version=2").charset("UTF-16").build()));
        cases.put("spec empty file name", spec -> spec.multiPart(new MultiPartSpecBuilder(TEXT).emptyFileName().build()));
        cases.put("spec blank file name", spec -> spec.multiPart(new MultiPartSpecBuilder(TEXT).fileName("  ").build()));
        cases.put("custom spec with empty mime-type and charset", spec -> spec.multiPart(customSpec(TEXT, "", "")));
        cases.put("custom spec with null mime-type, charset and file name", spec -> spec.multiPart(customSpec(BYTES, null, null)));
        // Content-types
        cases.put("multipart/mixed with charset", spec -> spec.contentType("multipart/mixed; charset=UTF-16").multiPart("ctrl", TEXT));
        cases.put("multipart with boundary", spec -> spec.contentType("multipart/form-data; boundary=custom").multiPart("ctrl", TEXT));
        cases.put("vendor multipart content-type", spec -> spec.contentType("application/vnd.x+multipart+form-data").multiPart("ctrl", TEXT));
        cases.put("non-multipart content-type", spec -> spec.contentType("application/json").multiPart("ctrl", TEXT));
        cases.put("strict mode, two parts", spec -> spec.config(RestAssured.config().httpClient(HttpClientConfig.httpClientConfig().httpMultipartMode(HttpMultipartMode.STRICT)))
                .multiPart("one", TEXT).multiPart("two", "file.bin", BYTES));
        cases.put("browser compatible mode", spec -> spec.config(RestAssured.config().httpClient(HttpClientConfig.httpClientConfig().httpMultipartMode(HttpMultipartMode.BROWSER_COMPATIBLE)))
                .multiPart("one", TEXT, "text/plain; charset=UTF-8"));
        return cases;
    }

    @Test
    void sends_every_kind_of_multipart_like_before() throws Exception {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Function<RequestSpecification, RequestSpecification>> multiPart : multiParts().entrySet()) {
            out.append("== ").append(multiPart.getKey()).append('\n').append(send(multiPart.getValue())).append('\n');
        }
        assertMatchesGoldenFile("multipart-requests.txt", out.toString());
    }

    private String send(Function<RequestSpecification, RequestSpecification> multiPart) {
        received = "<nothing sent>";
        StringBuilder out = new StringBuilder();
        RequestSpecification spec = given().filter((requestSpec, responseSpec, ctx) -> {
            out.append("filter sees:\n").append(render(requestSpec.getMultiPartParams()));
            return ctx.next(requestSpec, responseSpec);
        });
        try {
            multiPart.apply(spec).post("/");
            out.append("sent: ").append(received).append('\n');
        } catch (Throwable t) {
            out.append(EncoderCharacterization.render(t, tmpDir)).append('\n');
        }
        out.append("afterwards:\n").append(render(((QueryableRequestSpecification) spec).getMultiPartParams()));
        return out.toString();
    }

    private String render(List<MultiPartSpecification> multiParts) {
        StringBuilder out = new StringBuilder();
        for (MultiPartSpecification multiPart : multiParts) {
            out.append("  ").append(multiPart.getClass().getSimpleName())
                    .append(" controlName=").append(multiPart.getControlName())
                    .append(" fileName=").append(multiPart.getFileName())
                    .append(" hasFileName=").append(multiPart.hasFileName())
                    .append(" mimeType=").append(multiPart.getMimeType())
                    .append(" charset=").append(multiPart.getCharset())
                    .append(" headers=").append(multiPart.getHeaders())
                    .append(" content=").append(renderContent(multiPart.getContent()))
                    .append('\n')
                    .append("    toString: ").append(sanitize(multiPart.toString(), tmpDir))
                    .append('\n');
        }
        return out.toString();
    }

    private String renderContent(Object content) {
        if (content == null) {
            return "null";
        } else if (content instanceof byte[]) {
            return "byte[] " + escape((byte[]) content);
        } else if (content instanceof File) {
            return "File " + ((File) content).getName();
        } else if (content instanceof InputStream) {
            return content.getClass().getSimpleName();
        }
        return content.getClass().getSimpleName() + " " + sanitize(content.toString(), tmpDir);
    }

    private static InputStream stream() {
        return new ByteArrayInputStream(BYTES);
    }

    private static MultiPartSpecification customSpec(Object content, String mimeType, String charset) {
        return new MultiPartSpecification() {
            public Object getContent() {
                return content;
            }

            public String getControlName() {
                return "custom";
            }

            public String getMimeType() {
                return mimeType;
            }

            public Map<String, String> getHeaders() {
                return Collections.singletonMap("X-Custom", "c");
            }

            public String getCharset() {
                return charset;
            }

            public String getFileName() {
                return null;
            }

            public boolean hasFileName() {
                return false;
            }
        };
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
}
