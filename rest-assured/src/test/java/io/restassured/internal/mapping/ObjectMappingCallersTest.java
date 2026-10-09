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

package io.restassured.internal.mapping;

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.builder.ResponseBuilder;
import io.restassured.internal.mapping.MappingTestSupport.Greeting;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the code paths through which the request and response implementations and the multipart builder reach
 * {@link ObjectMapping} and the object mapper contexts.
 */
class ObjectMappingCallersTest {

    private static final String GREETING_JSON = "{\"firstName\":\"John\",\"lastName\":\"Doe\"}";

    private final AtomicReference<ObjectMapperSerializationContext> serializationContext = new AtomicReference<>();
    private final AtomicReference<ObjectMapperDeserializationContext> deserializationContext = new AtomicReference<>();

    private final ObjectMapper recordingMapper = new ObjectMapper() {
        @Override
        public Object deserialize(ObjectMapperDeserializationContext context) {
            deserializationContext.set(context);
            return "deserialized";
        }

        @Override
        public Object serialize(ObjectMapperSerializationContext context) {
            serializationContext.set(context);
            return "serialized";
        }
    };

    @Test
    void request_body_object_is_serialized_with_the_mapper_found_for_the_content_type() {
        FilterableRequestSpecification request = send(given().contentType("application/json").body(new Greeting("John", "Doe")));

        assertThat(request.<Object>getBody()).isEqualTo(GREETING_JSON);
    }

    @Test
    void request_body_object_is_serialized_with_the_given_mapper_type() {
        FilterableRequestSpecification request = send(given().contentType("application/xml").body(new Greeting("John", "Doe"), ObjectMapperType.GSON));

        assertThat(request.<Object>getBody()).isEqualTo(GREETING_JSON);
    }

    @Test
    void request_body_object_is_serialized_with_the_given_mapper() {
        Greeting greeting = new Greeting("John", "Doe");

        FilterableRequestSpecification request = send(given().contentType("application/json; charset=UTF-16").body(greeting, recordingMapper));

        assertThat(request.<Object>getBody()).isEqualTo("serialized");
        assertThat(serializationContext.get().getObjectToSerialize()).isSameAs(greeting);
        assertThat(serializationContext.get().getContentType()).isEqualTo("application/json; charset=UTF-16");
        assertThat(serializationContext.get().getCharset()).isEqualTo("UTF-16");
    }

    @Test
    void response_is_deserialized_with_the_mapper_found_for_the_content_type() {
        Greeting greeting = jsonResponse().as(Greeting.class);

        assertThat(greeting.getFirstName()).isEqualTo("John");
        assertThat(greeting.getLastName()).isEqualTo("Doe");
    }

    @Test
    void response_is_deserialized_with_the_given_mapper_type() {
        Greeting greeting = jsonResponse().as(Greeting.class, ObjectMapperType.JSONB);

        assertThat(greeting.getLastName()).isEqualTo("Doe");
    }

    @Test
    void response_is_deserialized_with_the_given_mapper() {
        Object result = jsonResponse().as(String.class, recordingMapper);

        assertThat(result).isEqualTo("deserialized");
        assertThat(deserializationContext.get().getType()).isEqualTo(String.class);
        assertThat(deserializationContext.get().getContentType()).isEqualTo("application/json; charset=ISO-8859-1");
        assertThat(deserializationContext.get().getCharset()).isEqualTo("ISO-8859-1");
        assertThat(deserializationContext.get().getDataToDeserialize().asString()).isEqualTo(GREETING_JSON);
    }

    @Test
    void multipart_content_is_serialized_with_the_given_mapper() {
        Greeting greeting = new Greeting("John", "Doe");

        MultiPartSpecification spec = new MultiPartSpecBuilder(greeting, recordingMapper).mimeType("application/json").build();

        assertThat(spec.getContent()).isEqualTo("serialized");
        assertThat(serializationContext.get().getObjectToSerialize()).isSameAs(greeting);
        assertThat(serializationContext.get().getContentType()).isEqualTo("application/json");
        assertThat(serializationContext.get().getCharset()).isNull();
    }

    @Test
    void multipart_content_is_serialized_with_the_given_mapper_type() {
        MultiPartSpecification spec = new MultiPartSpecBuilder(new Greeting("John", "Doe"), ObjectMapperType.GSON).mimeType("application/xml").build();

        assertThat(spec.getContent()).isEqualTo(GREETING_JSON);
    }

    private static Response jsonResponse() {
        return new ResponseBuilder()
                .setStatusCode(200)
                .setContentType("application/json; charset=ISO-8859-1")
                .setBody(GREETING_JSON)
                .build();
    }

    private static FilterableRequestSpecification send(RequestSpecification specification) {
        AtomicReference<FilterableRequestSpecification> captured = new AtomicReference<>();
        specification
                .filter((requestSpec, responseSpec, ctx) -> {
                    captured.set(requestSpec);
                    return new ResponseBuilder().setStatusCode(200).build();
                })
                .post("http://localhost:8080/greetings");
        return captured.get();
    }
}
