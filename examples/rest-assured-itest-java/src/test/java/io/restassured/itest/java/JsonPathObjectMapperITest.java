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

package io.restassured.itest.java;

import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.itest.java.objects.MapperProbe;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.response.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Jackson 2, Jackson 3 (through json-schema-validator) and Gson are all on this module's test classpath.
 */
public class JsonPathObjectMapperITest extends WithJetty {

    static Stream<Arguments> objectMapperTypes() {
        return Stream.of(
                Arguments.of(null, "jackson3"),
                Arguments.of(ObjectMapperType.JACKSON_3, "jackson3"),
                Arguments.of(ObjectMapperType.JACKSON_2, "jackson2"),
                Arguments.of(ObjectMapperType.GSON, "none"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("objectMapperTypes")
    void as_and_json_path_get_object_use_the_same_object_mapper(ObjectMapperType objectMapperType, String expectedMapper) {
        Response response = given().
                config(RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig(objectMapperType))).
                contentType(JSON).
                body("{\"mapper\":\"none\"}").
        when().
                post("/reflect");

        assertThat(response.as(MapperProbe.class).getMapper(), equalTo(expectedMapper));
        assertThat(response.jsonPath().getObject("", MapperProbe.class).getMapper(), equalTo(expectedMapper));
    }
}
