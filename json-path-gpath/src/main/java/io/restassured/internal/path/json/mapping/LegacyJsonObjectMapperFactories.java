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

package io.restassured.internal.path.json.mapping;

import io.restassured.common.mapper.factory.GsonObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultGsonObjectMapperFactory;
import io.restassured.common.mapper.factory.Jackson2ObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultJackson2ObjectMapperFactory;
import io.restassured.common.mapper.factory.Jackson3ObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultJackson3ObjectMapperFactory;
import io.restassured.common.mapper.factory.JohnzonObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultJohnzonObjectMapperFactory;
import io.restassured.common.mapper.factory.JsonbObjectMapperFactory;
import io.restassured.common.mapper.factory.DefaultYassonObjectMapperFactory;

/**
 * Turns the object mapper factory types in rest-assured-common into the deprecated ones that the configs still return in 6.x. A default factory becomes the matching deprecated default, so that it doesn't count as a custom factory.
 */
@SuppressWarnings("deprecation")
public class LegacyJsonObjectMapperFactories {

    private LegacyJsonObjectMapperFactories() {
    }

    public static io.restassured.path.json.mapper.factory.GsonObjectMapperFactory toLegacy(GsonObjectMapperFactory factory) {
        if (factory == null || factory instanceof io.restassured.path.json.mapper.factory.GsonObjectMapperFactory) {
            return (io.restassured.path.json.mapper.factory.GsonObjectMapperFactory) factory;
        } else if (factory.getClass() == DefaultGsonObjectMapperFactory.class) {
            return new io.restassured.path.json.mapper.factory.DefaultGsonObjectMapperFactory();
        }
        return factory::create;
    }

    public static io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory toLegacy(Jackson2ObjectMapperFactory factory) {
        if (factory == null || factory instanceof io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory) {
            return (io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory) factory;
        } else if (factory.getClass() == DefaultJackson2ObjectMapperFactory.class) {
            return new io.restassured.path.json.mapper.factory.DefaultJackson2ObjectMapperFactory();
        }
        return factory::create;
    }

    public static io.restassured.path.json.mapper.factory.Jackson3ObjectMapperFactory toLegacy(Jackson3ObjectMapperFactory factory) {
        if (factory == null || factory instanceof io.restassured.path.json.mapper.factory.Jackson3ObjectMapperFactory) {
            return (io.restassured.path.json.mapper.factory.Jackson3ObjectMapperFactory) factory;
        } else if (factory.getClass() == DefaultJackson3ObjectMapperFactory.class) {
            return new io.restassured.path.json.mapper.factory.DefaultJackson3ObjectMapperFactory();
        }
        return factory::create;
    }

    public static io.restassured.path.json.mapper.factory.JohnzonObjectMapperFactory toLegacy(JohnzonObjectMapperFactory factory) {
        if (factory == null || factory instanceof io.restassured.path.json.mapper.factory.JohnzonObjectMapperFactory) {
            return (io.restassured.path.json.mapper.factory.JohnzonObjectMapperFactory) factory;
        } else if (factory.getClass() == DefaultJohnzonObjectMapperFactory.class) {
            return new io.restassured.path.json.mapper.factory.DefaultJohnzonObjectMapperFactory();
        }
        return factory::create;
    }

    public static io.restassured.path.json.mapper.factory.JsonbObjectMapperFactory toLegacy(JsonbObjectMapperFactory factory) {
        if (factory == null || factory instanceof io.restassured.path.json.mapper.factory.JsonbObjectMapperFactory) {
            return (io.restassured.path.json.mapper.factory.JsonbObjectMapperFactory) factory;
        } else if (factory.getClass() == DefaultYassonObjectMapperFactory.class) {
            return new io.restassured.path.json.mapper.factory.DefaultYassonObjectMapperFactory();
        }
        return factory::create;
    }
}
