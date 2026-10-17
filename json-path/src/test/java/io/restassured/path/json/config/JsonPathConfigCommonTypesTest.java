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

package io.restassured.path.json.config;

import io.restassured.common.config.NumberReturnType;
import io.restassured.common.mapper.factory.DefaultGsonObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultJackson2ObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultJackson3ObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultYassonObjectMapperFactory;
import io.restassured.common.mapper.factory.GsonObjectMapperFactory;
import io.restassured.common.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.path.json.JsonPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.path.json.config.JsonPathConfig.jsonPathConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
class JsonPathConfigCommonTypesTest {

    @Test
    void common_default_factories_dont_count_as_custom() {
        JsonPathConfig config = jsonPathConfig()
                .gsonObjectMapperFactory(new DefaultGsonObjectMapperFactory())
                .jackson2ObjectMapperFactory(new DefaultJackson2ObjectMapperFactory())
                .jackson3ObjectMapperFactory(new DefaultJackson3ObjectMapperFactory())
                .jsonbObjectMapperFactory(new DefaultYassonObjectMapperFactory());

        assertThat(config.hasCustomGsonObjectMapperFactory()).isFalse();
        assertThat(config.hasCustomJackson20ObjectMapperFactory()).isFalse();
        assertThat(config.hasCustomJackson30ObjectMapperFactory()).isFalse();
        assertThat(config.hasCustomJsonbObjectMapperFactory()).isFalse();
    }

    @Test
    void custom_factory_of_the_common_type_is_used_by_json_path() {
        ObjectMapper objectMapper = new ObjectMapper();
        Jackson2ObjectMapperFactory factory = (type, charset) -> objectMapper;

        JsonPathConfig config = jsonPathConfig().jackson2ObjectMapperFactory(factory);

        assertThat(config.hasCustomJackson20ObjectMapperFactory()).isTrue();
        assertThat(config.jackson2ObjectMapperFactory().create(Object.class, "UTF-8")).isSameAs(objectMapper);
        AtomicInteger calls = new AtomicInteger();
        Jackson2ObjectMapperFactory counting = (type, charset) -> {
            calls.incrementAndGet();
            return objectMapper;
        };
        assertThat(new JsonPath("{ \"a\" : 1 }").using(counting).getObject("", Map.class)).containsEntry("a", 1);
        assertThat(calls).hasValue(1);
    }

    @Test
    void number_return_type_of_the_common_type_is_used_by_json_path() {
        JsonPath jsonPath = new JsonPath("{ \"price\" : 12.1 }").using(jsonPathConfig().numberReturnType(NumberReturnType.BIG_DECIMAL));

        assertThat(jsonPath.<Object>get("price")).isEqualTo(new BigDecimal("12.1"));
    }

    @Test
    void constructor_taking_the_common_number_return_type_sets_it() {
        JsonPath jsonPath = new JsonPath("{ \"price\" : 12.1 }").using(new JsonPathConfig(NumberReturnType.BIG_DECIMAL));

        assertThat(jsonPath.<Object>get("price")).isEqualTo(new BigDecimal("12.1"));
    }

    @Test
    void subclass_of_a_common_default_factory_counts_as_custom() {
        JsonPathConfig config = jsonPathConfig().gsonObjectMapperFactory(new DefaultGsonObjectMapperFactory() {
        });

        assertThat(config.hasCustomGsonObjectMapperFactory()).isTrue();
    }

    @Test
    void custom_gson_factory_of_the_common_type_is_used_by_json_path() {
        AtomicInteger calls = new AtomicInteger();
        GsonObjectMapperFactory factory = (type, charset) -> {
            calls.incrementAndGet();
            return new Gson();
        };

        assertThat(new JsonPath("{ \"a\" : \"b\" }").using(factory).getObject("", Map.class)).containsEntry("a", "b");
        assertThat(calls).hasValue(1);
    }
}
