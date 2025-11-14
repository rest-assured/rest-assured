/*
 * Copyright 2019 the original author or authors.
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

import com.google.gson.reflect.TypeToken;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.itest.java.objects.Message;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.mapper.ObjectMapperType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Type;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.mapper.ObjectMapperType.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TypeObjectMappingITest extends WithJetty {

    static java.util.stream.Stream<ObjectMapperType> getParameters() {
        return java.util.stream.Stream.of(GSON, JACKSON_1, JACKSON_2);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getParameters")
    void shouldUseMapTypeWithObjectMappers(ObjectMapperType mapperType) {
        String expected = "A message";
        final Message message = new Message();
        message.setMessage(expected);
        RestAssured.config = RestAssuredConfig.config().objectMapperConfig(new ObjectMapperConfig(mapperType));
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        final Map<String, String> returnedMessage = given().body(message).when().post("/reflect").as(type);
        assertThat(returnedMessage.get("message"), equalTo(expected));
    }
}
