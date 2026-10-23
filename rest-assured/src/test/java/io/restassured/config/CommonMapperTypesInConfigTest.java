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

package io.restassured.config;

import io.restassured.common.config.NumberReturnType;
import io.restassured.common.mapper.factory.DefaultGsonObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultJAXBObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultJackson2ObjectMapperFactory;
import io.restassured.common.mapper.factory.GsonObjectMapperFactory;
import io.restassured.common.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.path.json.config.JsonPathConfig;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.config.ObjectMapperConfig.objectMapperConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Core's configs take and return the object mapper factory types and NumberReturnType in rest-assured-common, and
 * still take the old factory types, which extend them.
 */
@SuppressWarnings("deprecation")
class CommonMapperTypesInConfigTest {

    @Test
    void factory_of_the_common_type_is_returned_as_is() {
        Gson gson = new Gson();
        GsonObjectMapperFactory factory = (type, charset) -> gson;

        GsonObjectMapperFactory returned = objectMapperConfig().gsonObjectMapperFactory(factory).gsonObjectMapperFactory();

        assertThat(returned).isSameAs(factory);
        assertThat(returned.create(Object.class, "UTF-8")).isSameAs(gson);
    }

    @Test
    void factory_of_the_old_type_is_kept_as_is() {
        io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory factory = (type, charset) -> null;

        assertThat(objectMapperConfig().jackson2ObjectMapperFactory(factory).jackson2ObjectMapperFactory()).isSameAs(factory);
    }

    @Test
    void lambda_passed_to_the_setter_compiles_without_a_cast() {
        Jackson2ObjectMapperFactory common = (type, charset) -> null;

        objectMapperConfig().jackson2ObjectMapperFactory((type, charset) -> null).jackson2ObjectMapperFactory(common);
    }

    @Test
    void default_factories_are_the_common_default_factories() {
        ObjectMapperConfig config = objectMapperConfig();

        assertThat(config.gsonObjectMapperFactory()).isExactlyInstanceOf(DefaultGsonObjectMapperFactory.class);
        assertThat(config.jackson2ObjectMapperFactory()).isExactlyInstanceOf(DefaultJackson2ObjectMapperFactory.class);
        assertThat(config.jaxbObjectMapperFactory()).isExactlyInstanceOf(DefaultJAXBObjectMapperFactory.class);
    }

    @Test
    void number_return_type_is_returned_as_is() {
        for (NumberReturnType numberReturnType : NumberReturnType.values()) {
            assertThat(jsonConfig().numberReturnType(numberReturnType).numberReturnType()).isSameAs(numberReturnType);
            assertThat(new JsonConfig(numberReturnType).numberReturnType()).isSameAs(numberReturnType);
        }
        // JsonPathConfig converts it to its own enum by name
        assertThat(Arrays.stream(JsonPathConfig.NumberReturnType.values()).map(Enum::name))
                .containsExactly(Arrays.stream(NumberReturnType.values()).map(Enum::name).toArray(String[]::new));
    }

    @Test
    void json_config_number_length_limit_matches_json_path() {
        assertThat(JsonConfig.DEFAULT_NUMBER_LENGTH_LIMIT).isEqualTo(JsonPathConfig.DEFAULT_NUMBER_LENGTH_LIMIT);
        assertThat(jsonConfig().numberLengthLimit()).isEqualTo(JsonConfig.DEFAULT_NUMBER_LENGTH_LIMIT);
    }
}
