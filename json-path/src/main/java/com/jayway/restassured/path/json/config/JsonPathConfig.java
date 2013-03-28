/*
 * Copyright 2012 the original author or authors.
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
package com.jayway.restassured.path.json.config;

import com.jayway.restassured.mapper.ObjectMapperType;
import com.jayway.restassured.mapper.factory.*;
import com.jayway.restassured.path.json.mapping.JsonPathObjectMapper;

import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;
import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE;

/**
 * Allows you to configure how the JsonPath will handle JSON numbers and object mappers. By default JsonPath and body expectations
 * in REST Assured will return floats for numbers that are less than or equal to {@link Float#MAX_VALUE} or doubles
 * for larger numbers. In Groovy 1.8.5 they changed so that all numbers are now BigDecimals. If you prefer that
 * you can configure the {@link NumberReturnType} to be {@link NumberReturnType#BIG_DECIMAL}.
 */
public class JsonPathConfig {

    private final NumberReturnType numberReturnType;
    private final JsonPathObjectMapper defaultObjectMapper;
    private final ObjectMapperType defaultObjectMapperType;
    private final GsonObjectMapperFactory gsonObjectMapperFactory;
    private final Jackson1ObjectMapperFactory jackson1ObjectMapperFactory;
    private final Jackson2ObjectMapperFactory jackson2ObjectMapperFactory;

    /**
     * Create a new instance of a JsonPathConfig based on the properties in the supplied config.
     *
     * @param config The config to copy.
     */
    public JsonPathConfig(JsonPathConfig config) {
        this(config.numberReturnType(), config.defaultObjectMapperType(), config.gsonObjectMapperFactory(), config.jackson1ObjectMapperFactory(),
                config.jackson2ObjectMapperFactory(), config.defaultObjectMapper());
    }

    /**
     * Creates a new JsonPathConfig that is configured to return floats and doubles.
     */
    public JsonPathConfig() {
        this(FLOAT_AND_DOUBLE, null, new DefaultGsonObjectMapperFactory(), new DefaultJackson1ObjectMapperFactory(),
                new DefaultJackson2ObjectMapperFactory(), null);
    }

    public JsonPathConfig(NumberReturnType numberReturnType) {
        this(numberReturnType, null, new DefaultGsonObjectMapperFactory(), new DefaultJackson1ObjectMapperFactory(),
                new DefaultJackson2ObjectMapperFactory(), null);

    }

    private JsonPathConfig(NumberReturnType numberReturnType, ObjectMapperType objectMapperType, GsonObjectMapperFactory gsonObjectMapperFactory,
                           Jackson1ObjectMapperFactory jackson1ObjectMapperFactory, Jackson2ObjectMapperFactory jackson2ObjectMapperFactory,
                           JsonPathObjectMapper defaultObjectMapper) {
        if (numberReturnType == null) throw new IllegalArgumentException("numberReturnType cannot be null");
        this.numberReturnType = numberReturnType;
        this.defaultObjectMapper = defaultObjectMapper;
        this.defaultObjectMapperType = objectMapperType;
        this.gsonObjectMapperFactory = gsonObjectMapperFactory;
        this.jackson1ObjectMapperFactory = jackson1ObjectMapperFactory;
        this.jackson2ObjectMapperFactory = jackson2ObjectMapperFactory;
    }

    public NumberReturnType numberReturnType() {
        return numberReturnType;
    }

    public JsonPathConfig numberReturnType(NumberReturnType numberReturnType) {
        return new JsonPathConfig(numberReturnType);
    }

    public boolean shouldRepresentJsonNumbersAsBigDecimal() {
        return numberReturnType() == BIG_DECIMAL;
    }

    public ObjectMapperType defaultObjectMapperType() {
        return defaultObjectMapperType;
    }

    public boolean hasDefaultObjectMapperType() {
        return defaultObjectMapperType != null;
    }

    public boolean hasObjectMapperFactory() {
        return hasGsonObjectMapperFactory() && hasJackson10ObjectMapperFactory() && hasJackson20ObjectMapperFactory();
    }

    public boolean hasGsonObjectMapperFactory() {
        return gsonObjectMapperFactory() != null;
    }

    public boolean hasJackson10ObjectMapperFactory() {
        return jackson1ObjectMapperFactory() != null;
    }

    public boolean hasJackson20ObjectMapperFactory() {
        return jackson2ObjectMapperFactory() != null;
    }

    /**
     * Creates an json path configuration that uses the specified object mapper type as default.
     *
     * @param defaultObjectMapperType The object mapper type to use. If <code>null</code> then classpath scanning will be used.
     */
    public JsonPathConfig defaultObjectMapperType(ObjectMapperType defaultObjectMapperType) {
        return new JsonPathConfig(numberReturnType, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, defaultObjectMapper);
    }

    public JsonPathObjectMapper defaultObjectMapper() {
        return defaultObjectMapper;
    }

    public boolean hasDefaultObjectMapper() {
        return defaultObjectMapper != null;
    }

    /**
     * Creates an json path configuration that uses the specified object mapper as default.
     *
     * @param defaultObjectMapper The object mapper to use. If <code>null</code> then classpath scanning will be used.
     */
    public JsonPathConfig defaultObjectMapper(JsonPathObjectMapper defaultObjectMapper) {
        return new JsonPathConfig(numberReturnType, null, gsonObjectMapperFactory, jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, defaultObjectMapper);
    }

    public GsonObjectMapperFactory gsonObjectMapperFactory() {
        return gsonObjectMapperFactory;
    }

    /**
     * Specify a custom Gson object mapper factory.
     *
     * @param gsonObjectMapperFactory The object mapper factory
     */
    public JsonPathConfig gsonObjectMapperFactory(GsonObjectMapperFactory gsonObjectMapperFactory) {
        return new JsonPathConfig(numberReturnType, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, defaultObjectMapper);
    }

    public Jackson1ObjectMapperFactory jackson1ObjectMapperFactory() {
        return jackson1ObjectMapperFactory;
    }

    /**
     * Specify a custom Jackson 1.0 object mapper factory.
     *
     * @param jackson1ObjectMapperFactory The object mapper factory
     */
    public JsonPathConfig jackson1ObjectMapperFactory(Jackson1ObjectMapperFactory jackson1ObjectMapperFactory) {
        return new JsonPathConfig(numberReturnType, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, defaultObjectMapper);
    }

    public Jackson2ObjectMapperFactory jackson2ObjectMapperFactory() {
        return jackson2ObjectMapperFactory;
    }

    /**
     * Specify a custom Jackson 1.0 object mapper factory.
     *
     * @param jackson2ObjectMapperFactory The object mapper factory
     */
    public JsonPathConfig jackson2ObjectMapperFactory(Jackson2ObjectMapperFactory jackson2ObjectMapperFactory) {
        return new JsonPathConfig(numberReturnType, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, defaultObjectMapper);
    }

    /**
     * @return A static way to create a new JsonPathConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static JsonPathConfig jsonPathConfig() {
        return new JsonPathConfig();
    }

    public static enum NumberReturnType {
        FLOAT_AND_DOUBLE, BIG_DECIMAL
    }
}
