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

import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.mapper.factory.*;
import io.restassured.path.xml.mapper.factory.DefaultJAXBObjectMapperFactory;
import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory;
import org.apache.commons.lang3.Validate;

/**
 * Allows you to specify configuration for the object mapping functionality.
 */
public class ObjectMapperConfig implements Config {

    private final ObjectMapper defaultObjectMapper;
    private final ObjectMapperType defaultObjectMapperType;
    private final GsonObjectMapperFactory gsonObjectMapperFactory;
    private final Jackson1ObjectMapperFactory jackson1ObjectMapperFactory;
    private final Jackson2ObjectMapperFactory jackson2ObjectMapperFactory;
    private final JAXBObjectMapperFactory jaxbObjectMapperFactory;
    private final JohnzonObjectMapperFactory johnzonObjectMapperFactory;
    private final JsonbObjectMapperFactory jsonbObjectMapperFactory;
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
        gsonObjectMapperFactory = new DefaultGsonObjectMapperFactory();
        jackson1ObjectMapperFactory = new DefaultJackson1ObjectMapperFactory();
        jackson2ObjectMapperFactory = new DefaultJackson2ObjectMapperFactory();
        jaxbObjectMapperFactory = new DefaultJAXBObjectMapperFactory();
        johnzonObjectMapperFactory = new DefaultJohnzonObjectMapperFactory();
        jsonbObjectMapperFactory = new DefaultYassonObjectMapperFactory();
        isUserConfigured = false;
    }

    /**
     * Creates an object mapper configuration that uses the specified object mapper as default.
     *
     * @param defaultObjectMapperType The object mapper to use. If <code>null</code> then classpath scanning will be used.
     */
    public ObjectMapperConfig(ObjectMapperType defaultObjectMapperType) {
        this(null, defaultObjectMapperType, new DefaultGsonObjectMapperFactory(), new DefaultJackson1ObjectMapperFactory(),
                new DefaultJackson2ObjectMapperFactory(), new DefaultJAXBObjectMapperFactory(), 
                    new DefaultJohnzonObjectMapperFactory(), new DefaultYassonObjectMapperFactory(), true);
    }

    /**
     * Creates an object mapper configuration that uses the specified object mapper as default.
     *
     * @param defaultObjectMapper The object mapper to use. If <code>null</code> then classpath scanning will be used.
     */
    public ObjectMapperConfig(ObjectMapper defaultObjectMapper) {
        this(defaultObjectMapper, null, new DefaultGsonObjectMapperFactory(), new DefaultJackson1ObjectMapperFactory(),
                new DefaultJackson2ObjectMapperFactory(), new DefaultJAXBObjectMapperFactory(), 
                    new DefaultJohnzonObjectMapperFactory(), new DefaultYassonObjectMapperFactory(), true);
    }

    private ObjectMapperConfig(ObjectMapper defaultObjectMapper, ObjectMapperType defaultObjectMapperType,
                               GsonObjectMapperFactory gsonObjectMapperFactory, Jackson1ObjectMapperFactory jackson1ObjectMapperFactory,
                               Jackson2ObjectMapperFactory jackson2ObjectMapperFactory, JAXBObjectMapperFactory jaxbObjectMapperFactory,
                               JohnzonObjectMapperFactory johnzonObjectMapperFactory, JsonbObjectMapperFactory jsonbObjectMapperFactory, 
                               boolean isUserConfigured) {
        Validate.notNull(gsonObjectMapperFactory, GsonObjectMapperFactory.class.getSimpleName() + " cannot be null");
        Validate.notNull(jackson1ObjectMapperFactory, Jackson1ObjectMapperFactory.class.getSimpleName() + " cannot be null");
        Validate.notNull(jackson2ObjectMapperFactory, Jackson2ObjectMapperFactory.class.getSimpleName() + " cannot be null");
        Validate.notNull(jaxbObjectMapperFactory, JAXBObjectMapperFactory.class.getSimpleName() + " cannot be null");
        this.defaultObjectMapperType = defaultObjectMapperType;
        this.defaultObjectMapper = defaultObjectMapper;
        this.gsonObjectMapperFactory = gsonObjectMapperFactory;
        this.jackson1ObjectMapperFactory = jackson1ObjectMapperFactory;
        this.jackson2ObjectMapperFactory = jackson2ObjectMapperFactory;
        this.jaxbObjectMapperFactory = jaxbObjectMapperFactory;
        this.johnzonObjectMapperFactory = johnzonObjectMapperFactory;
        this.jsonbObjectMapperFactory = jsonbObjectMapperFactory;
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
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jaxbObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, true);
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
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jaxbObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, true);
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
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jaxbObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, true);
    }

    public Jackson2ObjectMapperFactory jackson2ObjectMapperFactory() {
        return jackson2ObjectMapperFactory;
    }

    /**
     * Specify a custom Jackson 1.0 object mapper factory.
     *
     * @param jackson2ObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig jackson2ObjectMapperFactory(Jackson2ObjectMapperFactory jackson2ObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jaxbObjectMapperFactory, 
                   johnzonObjectMapperFactory, jsonbObjectMapperFactory, true);
    }

    public JAXBObjectMapperFactory jaxbObjectMapperFactory() {
        return jaxbObjectMapperFactory;
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
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jaxbObjectMapperFactory, 
                   johnzonObjectMapperFactory, jsonbObjectMapperFactory, true);
    }

    /**
     * Specify a custom JAXB object mapper factory.
     *
     * @param jaxbObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig jaxbObjectMapperFactory(JAXBObjectMapperFactory jaxbObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory,
                jackson1ObjectMapperFactory, jackson2ObjectMapperFactory, jaxbObjectMapperFactory, 
                    johnzonObjectMapperFactory, jsonbObjectMapperFactory, true);
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
}