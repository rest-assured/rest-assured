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

package io.restassured.config;

import io.restassured.common.mapper.resolver.ObjectMapperResolver;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.mapper.factory.*;
import io.restassured.path.xml.mapper.factory.DefaultJAXBObjectMapperFactory;
import io.restassured.path.xml.mapper.factory.DefaultJakartaEEObjectMapperFactory;
import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory;
import io.restassured.path.xml.mapper.factory.JakartaEEObjectMapperFactory;

/**
 * Allows you to specify configuration for the object mapping functionality.
 */
public class ObjectMapperConfig implements Config {

    private final ObjectMapper defaultObjectMapper;
    private final ObjectMapperType defaultObjectMapperType;
    private final GsonObjectMapperFactory gsonObjectMapperFactory;
    private final Jackson1ObjectMapperFactory jackson1ObjectMapperFactory;
    private final Jackson2ObjectMapperFactory jackson2ObjectMapperFactory;
    private final Jackson3ObjectMapperFactory jackson3ObjectMapperFactory;
    private final JAXBObjectMapperFactory jaxbObjectMapperFactory;
    private final JohnzonObjectMapperFactory johnzonObjectMapperFactory;
    private final JsonbObjectMapperFactory jsonbObjectMapperFactory;
    private final JakartaEEObjectMapperFactory jakartaEEObjectMapperFactory;
    private final boolean isUserConfigured;

    /**
     * Default object mapper configuration that uses no explicit object mapper. An object mapper
     * will be found automatically in classpath if available. For more details see <a href="http://code.google.com/p/rest-assured/wiki/Usage#Object_Mapping">documentation</a>.
     * <p/>
     * Also default object mapper factories will be used.
     */
    public ObjectMapperConfig() {
        defaultObjectMapper = null;
        defaultObjectMapperType = null;
        gsonObjectMapperFactory = newGsonObjectMapperFactoryOrNullIfNotInClasspath();
        jackson1ObjectMapperFactory = newJackson1ObjectMapperFactoryOrNullIfNotInClasspath();
        jackson2ObjectMapperFactory = newJackson2ObjectMapperFactoryOrNullIfNotInClasspath();
        jackson3ObjectMapperFactory = newJackson3ObjectMapperFactoryOrNullIfNotInClasspath();
        jaxbObjectMapperFactory = newJaxbObjectMapperFactoryOrNullIfNotInClasspath();
        johnzonObjectMapperFactory = newJohnzonObjectMapperFactoryOrNullIfNotInClasspath();
        jsonbObjectMapperFactory = newYassinObjectMapperFactoryOrNullIfNotInClasspath();
        jakartaEEObjectMapperFactory = newJakartaEEObjectMapperFactoryOrNullIfNotInClasspath();
        isUserConfigured = false;
    }

    /**
     * Creates an object mapper configuration that uses the specified object mapper as default.
     *
     * @param defaultObjectMapperType The object mapper to use. If <code>null</code> then classpath scanning will be used.
     */
    public ObjectMapperConfig(ObjectMapperType defaultObjectMapperType) {
        this(null, defaultObjectMapperType, newGsonObjectMapperFactoryOrNullIfNotInClasspath(), newJackson1ObjectMapperFactoryOrNullIfNotInClasspath(),
                newJackson2ObjectMapperFactoryOrNullIfNotInClasspath(), newJackson3ObjectMapperFactoryOrNullIfNotInClasspath(), newJaxbObjectMapperFactoryOrNullIfNotInClasspath(),
                newJohnzonObjectMapperFactoryOrNullIfNotInClasspath(), newYassinObjectMapperFactoryOrNullIfNotInClasspath(), newJakartaEEObjectMapperFactoryOrNullIfNotInClasspath(),
                true);
    }

    /**
     * Creates an object mapper configuration that uses the specified object mapper as default.
     *
     * @param defaultObjectMapper The object mapper to use. If <code>null</code> then classpath scanning will be used.
     */
    public ObjectMapperConfig(ObjectMapper defaultObjectMapper) {
        this(defaultObjectMapper, null, newGsonObjectMapperFactoryOrNullIfNotInClasspath(), newJackson1ObjectMapperFactoryOrNullIfNotInClasspath(),
                newJackson2ObjectMapperFactoryOrNullIfNotInClasspath(), newJackson3ObjectMapperFactoryOrNullIfNotInClasspath(),
                newJaxbObjectMapperFactoryOrNullIfNotInClasspath(), newJohnzonObjectMapperFactoryOrNullIfNotInClasspath(),
                newYassinObjectMapperFactoryOrNullIfNotInClasspath(), newJakartaEEObjectMapperFactoryOrNullIfNotInClasspath(),
                true);
    }

    private ObjectMapperConfig(ObjectMapper defaultObjectMapper, ObjectMapperType defaultObjectMapperType,
                               GsonObjectMapperFactory gsonObjectMapperFactory, Jackson1ObjectMapperFactory jackson1ObjectMapperFactory,
                               Jackson2ObjectMapperFactory jackson2ObjectMapperFactory, Jackson3ObjectMapperFactory jackson3ObjectMapperFactory,
                               JAXBObjectMapperFactory jaxbObjectMapperFactory, JohnzonObjectMapperFactory johnzonObjectMapperFactory,
                               JsonbObjectMapperFactory jsonbObjectMapperFactory, JakartaEEObjectMapperFactory jakartaEEObjectMapperFactory,
                               boolean isUserConfigured) {
        this.defaultObjectMapperType = defaultObjectMapperType;
        this.defaultObjectMapper = defaultObjectMapper;
        this.gsonObjectMapperFactory = gsonObjectMapperFactory;
        this.jackson1ObjectMapperFactory = jackson1ObjectMapperFactory;
        this.jackson2ObjectMapperFactory = jackson2ObjectMapperFactory;
        this.jackson3ObjectMapperFactory = jackson3ObjectMapperFactory;
        this.jaxbObjectMapperFactory = jaxbObjectMapperFactory;
        this.johnzonObjectMapperFactory = johnzonObjectMapperFactory;
        this.jsonbObjectMapperFactory = jsonbObjectMapperFactory;
        this.jakartaEEObjectMapperFactory = jakartaEEObjectMapperFactory;
        this.isUserConfigured = isUserConfigured;
    }

    public ObjectMapperType defaultObjectMapperType() {
        return defaultObjectMapperType;
    }

    public boolean hasDefaultObjectMapperType() {
        return defaultObjectMapperType != null;
    }

    /**
     * Creates an object mapper configuration that uses the specified object mapper as default.
     *
     * @param defaultObjectMapperType The object mapper to use. If <code>null</code> then classpath scanning will be used.
     */
    public ObjectMapperConfig defaultObjectMapperType(ObjectMapperType defaultObjectMapperType) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jackson3ObjectMapperFactory, jaxbObjectMapperFactory,
                johnzonObjectMapperFactory, jsonbObjectMapperFactory, jakartaEEObjectMapperFactory, true);
    }

    public ObjectMapper defaultObjectMapper() {
        return defaultObjectMapper;
    }

    public boolean hasDefaultObjectMapper() {
        return defaultObjectMapper != null;
    }

    /**
     * Creates an object mapper configuration that uses the specified object mapper as default.
     *
     * @param defaultObjectMapper The object mapper to use. If <code>null</code> then classpath scanning will be used.
     */
    public ObjectMapperConfig defaultObjectMapper(ObjectMapper defaultObjectMapper) {
        return new ObjectMapperConfig(defaultObjectMapper);
    }

    public GsonObjectMapperFactory gsonObjectMapperFactory() {
        return gsonObjectMapperFactory;
    }

    /**
     * Specify a custom Gson object mapper factory.
     *
     * @param gsonObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig gsonObjectMapperFactory(GsonObjectMapperFactory gsonObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jackson3ObjectMapperFactory, jaxbObjectMapperFactory,
                johnzonObjectMapperFactory, jsonbObjectMapperFactory, jakartaEEObjectMapperFactory, true);
    }

    public Jackson1ObjectMapperFactory jackson1ObjectMapperFactory() {
        return jackson1ObjectMapperFactory;
    }

    /**
     * Specify a custom Jackson 1.0 object mapper factory.
     *
     * @param jackson1ObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig jackson1ObjectMapperFactory(Jackson1ObjectMapperFactory jackson1ObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jackson3ObjectMapperFactory, jaxbObjectMapperFactory,
                johnzonObjectMapperFactory, jsonbObjectMapperFactory, jakartaEEObjectMapperFactory, true);
    }

    public Jackson2ObjectMapperFactory jackson2ObjectMapperFactory() {
        return jackson2ObjectMapperFactory;
    }

    public Jackson3ObjectMapperFactory jackson3ObjectMapperFactory() {
        return jackson3ObjectMapperFactory;
    }

    /**
     * Specify a custom Jackson 2 object mapper factory.
     *
     * @param jackson2ObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig jackson2ObjectMapperFactory(Jackson2ObjectMapperFactory jackson2ObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jackson3ObjectMapperFactory, jaxbObjectMapperFactory,
                johnzonObjectMapperFactory, jsonbObjectMapperFactory, jakartaEEObjectMapperFactory, true);
    }

    /**
     * Specify a custom Jackson 3 object mapper factory.
     *
     * @param jackson3ObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig jackson3ObjectMapperFactory(Jackson3ObjectMapperFactory jackson3ObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jackson3ObjectMapperFactory, jaxbObjectMapperFactory,
                johnzonObjectMapperFactory, jsonbObjectMapperFactory, jakartaEEObjectMapperFactory, true);
    }

    public JAXBObjectMapperFactory jaxbObjectMapperFactory() {
        return jaxbObjectMapperFactory;
    }

    public JakartaEEObjectMapperFactory jakartaEEObjectMapperFactory() {
        return jakartaEEObjectMapperFactory;
    }

    public JohnzonObjectMapperFactory johnzonObjectMapperFactory() {
        return johnzonObjectMapperFactory;
    }

    public JsonbObjectMapperFactory jsonbObjectMapperFactory() {
        return jsonbObjectMapperFactory;
    }

    /**
     * Specify a custom JSON-B object mapper factory.
     *
     * @param jsonbObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig jsonbObjectMapperFactory(JsonbObjectMapperFactory jsonbObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jackson3ObjectMapperFactory, jaxbObjectMapperFactory,
                johnzonObjectMapperFactory, jsonbObjectMapperFactory, jakartaEEObjectMapperFactory, true);
    }

    /**
     * Specify a custom JAXB object mapper factory.
     *
     * @param jaxbObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig jaxbObjectMapperFactory(JAXBObjectMapperFactory jaxbObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jackson3ObjectMapperFactory, jaxbObjectMapperFactory,
                johnzonObjectMapperFactory, jsonbObjectMapperFactory, jakartaEEObjectMapperFactory, true);
    }

    /**
     * Specify a custom JakartaEE object mapper factory.
     *
     * @param jakartaEEObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig jakartaEEObjectMapperFactory(JakartaEEObjectMapperFactory jakartaEEObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jackson3ObjectMapperFactory, jaxbObjectMapperFactory,
                johnzonObjectMapperFactory, jsonbObjectMapperFactory, jakartaEEObjectMapperFactory, true);
    }

    /**
     * @return A static way to create a new ObjectMapperConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static ObjectMapperConfig objectMapperConfig() {
        return new ObjectMapperConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same object mapper config instance.
     */
    public ObjectMapperConfig and() {
        return this;
    }

    public boolean isUserConfigured() {
        return isUserConfigured;
    }

    public static JAXBObjectMapperFactory newJaxbObjectMapperFactoryOrNullIfNotInClasspath() {
        return ObjectMapperResolver.isJAXBInClassPath() ? new DefaultJAXBObjectMapperFactory() : null;
    }

    private static GsonObjectMapperFactory newGsonObjectMapperFactoryOrNullIfNotInClasspath() {
        return ObjectMapperResolver.isGsonInClassPath() ? new DefaultGsonObjectMapperFactory() : null;
    }

    private static Jackson1ObjectMapperFactory newJackson1ObjectMapperFactoryOrNullIfNotInClasspath() {
        return ObjectMapperResolver.isJackson1InClassPath() ? new DefaultJackson1ObjectMapperFactory() : null;
    }

    private static Jackson2ObjectMapperFactory newJackson2ObjectMapperFactoryOrNullIfNotInClasspath() {
        return ObjectMapperResolver.isJackson2InClassPath() ? new DefaultJackson2ObjectMapperFactory() : null;
    }

    private static Jackson3ObjectMapperFactory newJackson3ObjectMapperFactoryOrNullIfNotInClasspath() {
        return ObjectMapperResolver.isJackson3InClassPath() ? new DefaultJackson3ObjectMapperFactory() : null;
    }

    private static JohnzonObjectMapperFactory newJohnzonObjectMapperFactoryOrNullIfNotInClasspath() {
        return ObjectMapperResolver.isJohnzonInClassPath() ? new DefaultJohnzonObjectMapperFactory() : null;
    }

    private static JakartaEEObjectMapperFactory newJakartaEEObjectMapperFactoryOrNullIfNotInClasspath() {
        return ObjectMapperResolver.isJakartaEEInClassPath() ? new DefaultJakartaEEObjectMapperFactory() : null;
    }

    private static JsonbObjectMapperFactory newYassinObjectMapperFactoryOrNullIfNotInClasspath() {
        return ObjectMapperResolver.isYassonInClassPath() ? new DefaultYassonObjectMapperFactory() : null;
    }
}