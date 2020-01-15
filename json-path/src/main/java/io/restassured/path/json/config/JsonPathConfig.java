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

package io.restassured.path.json.config;

import io.restassured.path.json.mapper.factory.*;
import io.restassured.path.json.mapping.JsonPathObjectDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;

import static io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;
import static io.restassured.path.json.config.JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE;

/**
 * Allows you to configure how JsonPath will handle JSON numbers and object mappers. By default JsonPath and body expectations
 * in REST Assured will return floats for numbers that are less than or equal to {@link Float#MAX_VALUE} or doubles
 * for larger numbers. In Groovy 1.8.5 they changed so that all numbers are now BigDecimals. If you prefer that
 * you can configure the {@link NumberReturnType} to be {@link NumberReturnType#BIG_DECIMAL}.
 */
public class JsonPathConfig {

    private final NumberReturnType numberReturnType;
    private final JsonPathObjectDeserializer defaultDeserializer;
    private final JsonParserType defaultParserType;
    private final GsonObjectMapperFactory gsonObjectMapperFactory;
    private final Jackson1ObjectMapperFactory jackson1ObjectMapperFactory;
    private final Jackson2ObjectMapperFactory jackson2ObjectMapperFactory;
    private final JohnzonObjectMapperFactory johnzonObjectMapperFactory;
    private final JsonbObjectMapperFactory jsonbObjectMapperFactory;
    private final String charset;


    /**
     * Create a new instance of a JsonPathConfig based on the properties in the supplied config.
     *
     * @param config The config to copy.
     */
    public JsonPathConfig(JsonPathConfig config) {
        this(config.numberReturnType(), config.defaultParserType(), config.gsonObjectMapperFactory(), config.jackson1ObjectMapperFactory(),
                config.jackson2ObjectMapperFactory(), config.johnzonObjectMapperFactory(), config.jsonbObjectMapperFactory(),
                config.defaultDeserializer(), config.charset());
    }

    /**
     * Creates a new JsonPathConfig that is configured to return floats and doubles and use the systems default charset for JSON data.
     */
    public JsonPathConfig() {
        this(FLOAT_AND_DOUBLE, null, new DefaultGsonObjectMapperFactory(), new DefaultJackson1ObjectMapperFactory(),
                new DefaultJackson2ObjectMapperFactory(), new DefaultJohnzonObjectMapperFactory(), 
                new DefaultYassonObjectMapperFactory(), null, defaultCharset());
    }


    /**
     * Create a new JsonPathConfig that returns JSON numbers as either Doubles and Floats or BigDecimals
     */
    public JsonPathConfig(NumberReturnType numberReturnType) {
        this(numberReturnType, null, new DefaultGsonObjectMapperFactory(), new DefaultJackson1ObjectMapperFactory(),
                new DefaultJackson2ObjectMapperFactory(), new DefaultJohnzonObjectMapperFactory(), 
                new DefaultYassonObjectMapperFactory(), null, defaultCharset());

    }

    /**
     * Create a new JsonPathConfig that uses the <code>defaultCharset</code> when deserializing JSON data.
     */
    public JsonPathConfig(String defaultCharset) {
        this(FLOAT_AND_DOUBLE, null, new DefaultGsonObjectMapperFactory(), new DefaultJackson1ObjectMapperFactory(),
                new DefaultJackson2ObjectMapperFactory(), new DefaultJohnzonObjectMapperFactory(), 
                new DefaultYassonObjectMapperFactory(), null, defaultCharset);

    }

    private JsonPathConfig(NumberReturnType numberReturnType, JsonParserType parserType, GsonObjectMapperFactory gsonObjectMapperFactory,
                           Jackson1ObjectMapperFactory jackson1ObjectMapperFactory, Jackson2ObjectMapperFactory jackson2ObjectMapperFactory,
                           JohnzonObjectMapperFactory johnzonObjectMapperFactory,JsonbObjectMapperFactory jsonbObjectMapperFactory, 
                           JsonPathObjectDeserializer defaultDeserializer, String charset) {
        if (numberReturnType == null) throw new IllegalArgumentException("numberReturnType cannot be null");
        charset = StringUtils.trimToNull(charset);
        if (charset == null) throw new IllegalArgumentException("Charset cannot be empty");
        this.charset = charset;
        this.numberReturnType = numberReturnType;
        this.defaultDeserializer = defaultDeserializer;
        this.defaultParserType = parserType;
        this.gsonObjectMapperFactory = gsonObjectMapperFactory;
        this.jackson1ObjectMapperFactory = jackson1ObjectMapperFactory;
        this.jackson2ObjectMapperFactory = jackson2ObjectMapperFactory;
        this.johnzonObjectMapperFactory = johnzonObjectMapperFactory;
        this.jsonbObjectMapperFactory = jsonbObjectMapperFactory;
    }

    private static String defaultCharset() {
        return Charset.defaultCharset().name();
    }

    /**
     * @return The charset to assume when parsing JSON data
     */
    public String charset() {
        return charset;
    }

    /**
     * @return A new JsonPathConfig instance with that assumes the supplied charset when parsing JSON documents.
     */
    public JsonPathConfig charset(String charset) {
        return new JsonPathConfig(numberReturnType, defaultParserType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, defaultDeserializer, charset);
    }


    public NumberReturnType numberReturnType() {
        return numberReturnType;
    }

    /**
     * Specifies if JsonPath should use floats and doubles or BigDecimals to represent Json numbers.
     *
     * @param numberReturnType The choice.
     * @return A new instance of JsonPathConfig with the given configuration
     */
    public JsonPathConfig numberReturnType(NumberReturnType numberReturnType) {
        return new JsonPathConfig(numberReturnType, defaultParserType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, defaultDeserializer, charset);
    }

    public boolean shouldRepresentJsonNumbersAsBigDecimal() {
        return numberReturnType() == BIG_DECIMAL;
    }

    public JsonParserType defaultParserType() {
        return defaultParserType;
    }

    public boolean hasDefaultParserType() {
        return defaultParserType != null;
    }

    public boolean hasCustomGsonObjectMapperFactory() {
        return gsonObjectMapperFactory() != null && gsonObjectMapperFactory().getClass() != DefaultGsonObjectMapperFactory.class;
    }

    public boolean hasCustomJackson10ObjectMapperFactory() {
        return jackson1ObjectMapperFactory() != null && jackson1ObjectMapperFactory().getClass() != DefaultJackson1ObjectMapperFactory.class;
    }

    public boolean hasCustomJackson20ObjectMapperFactory() {
        return jackson2ObjectMapperFactory() != null && jackson2ObjectMapperFactory().getClass() != DefaultJackson2ObjectMapperFactory.class;
    }

    public boolean hasCustomJohnzonObjectMapperFactory() {
        return johnzonObjectMapperFactory() != null && johnzonObjectMapperFactory().getClass() != DefaultJohnzonObjectMapperFactory.class;
    }
    
    public boolean hasCustomJsonbObjectMapperFactory() {
        return jsonbObjectMapperFactory() != null && jsonbObjectMapperFactory().getClass() != DefaultYassonObjectMapperFactory.class;
    }

    /**
     * Creates an json path configuration that uses the specified parser type as default.
     *
     * @param defaultParserType The parser type to use. If <code>null</code> then classpath scanning will be used.
     */
    public JsonPathConfig defaultParserType(JsonParserType defaultParserType) {
        return new JsonPathConfig(numberReturnType, defaultParserType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, defaultDeserializer, charset);
    }

    public JsonPathObjectDeserializer defaultDeserializer() {
        return defaultDeserializer;
    }

    public boolean hasDefaultDeserializer() {
        return defaultDeserializer != null;
    }

    /**
     * Creates an json path configuration that uses the specified object de-serializer as default.
     *
     * @param defaultObjectDeserializer The object de-serializer to use. If <code>null</code> then classpath scanning will be used.
     */
    public JsonPathConfig defaultObjectDeserializer(JsonPathObjectDeserializer defaultObjectDeserializer) {
        return new JsonPathConfig(numberReturnType, null, gsonObjectMapperFactory, jackson1ObjectMapperFactory,
                jackson2ObjectMapperFactory, johnzonObjectMapperFactory, jsonbObjectMapperFactory, defaultObjectDeserializer, charset);
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
        return new JsonPathConfig(numberReturnType, defaultParserType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, defaultDeserializer, charset);
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
        return new JsonPathConfig(numberReturnType, defaultParserType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, defaultDeserializer, charset);
    }

    public Jackson2ObjectMapperFactory jackson2ObjectMapperFactory() {
        return jackson2ObjectMapperFactory;
    }
    
    public JohnzonObjectMapperFactory johnzonObjectMapperFactory() {
        return johnzonObjectMapperFactory;
    }

    /**
     * Specify a custom Jackson 1.0 object mapper factory.
     *
     * @param jackson2ObjectMapperFactory The object mapper factory
     */
    public JsonPathConfig jackson2ObjectMapperFactory(Jackson2ObjectMapperFactory jackson2ObjectMapperFactory) {
        return new JsonPathConfig(numberReturnType, defaultParserType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, defaultDeserializer, charset);
    }
    
    public JsonbObjectMapperFactory jsonbObjectMapperFactory() {
        return jsonbObjectMapperFactory;
    }

    /**
     * Specify a custom JSON-B object mapper factory.
     *
     * @param jsonbObjectMapperFactory The object mapper factory
     */
    public JsonPathConfig jsonbObjectMapperFactory(JsonbObjectMapperFactory jsonbObjectMapperFactory) {
        return new JsonPathConfig(numberReturnType, defaultParserType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, defaultDeserializer, charset);
    }

    /**
     * @return A static way to create a new JsonPathConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static JsonPathConfig jsonPathConfig() {
        return new JsonPathConfig();
    }

    /**
     * For syntactic sugar.
     *
     * @return The same JsonPathConfig instance
     */
    public JsonPathConfig with() {
        return this;
    }

    /**
     * For syntactic sugar.
     *
     * @return The same JsonPathConfig instance
     */
    public JsonPathConfig and() {
        return this;
    }


    /**
     * Specifies what kind of numbers to return. <br>
     * <p>
     * To specify the return type of decimal numbers parsed from the json, use the following:
     * <ul>
     * <li>FLOAT_AND_DOUBLE</li> <li>BIG_DECIMAL</li> <li>DOUBLE</li>
     * </ul>
     * To specify the return type of non-decimal numbers in the json, use the following:
     * <ul>
     * <li>BIG_INTEGER</li>
     * </ul>
     */
    public enum NumberReturnType {
        /**
         * Convert all non-integer numbers to floats and doubles (depending on the size of the number)
         */
        FLOAT_AND_DOUBLE,
        /**
         * Convert all non-integer numbers to BigDecimal
         */
        BIG_DECIMAL,
        /**
         * Convert all non-integer numbers to doubles
         */
        DOUBLE,
        /**
         * Converts all non-decimal numbers to BigInteger
         */
        BIG_INTEGER;

        /**
         * Returns a boolean indicating whether this type is included in those that deal with floats
         * or doubles exclusive of BigDecimal.
         *
         * @return <code>true</code> if value is {@link #FLOAT_AND_DOUBLE} or {@link #DOUBLE}, <code>false</code> otherwise.
         */
        public final boolean isFloatOrDouble() {
            return this.equals(FLOAT_AND_DOUBLE)
                    || this.equals(DOUBLE);
        }

    }
}
