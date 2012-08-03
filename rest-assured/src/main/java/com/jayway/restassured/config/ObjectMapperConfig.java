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
package com.jayway.restassured.config;

import com.jayway.restassured.mapper.ObjectMapper;
import com.jayway.restassured.mapper.ObjectMapperType;
import com.jayway.restassured.mapper.factory.*;
import org.apache.commons.lang3.Validate;

/**
 * Allows you to specify configuration for the object mapping functionality.
 */
public class ObjectMapperConfig {

    private final ObjectMapper defaultObjectMapper;
    private final ObjectMapperType defaultObjectMapperType;
    private final GsonObjectMapperFactory gsonObjectMapperFactory;
    private final JacksonObjectMapperFactory jacksonObjectMapperFactory;
    private final JAXBObjectMapperFactory jaxbObjectMapperFactory;

    /**
     * Default object mapper configuration that uses no explicit object mapper. An object mapper
     * will be found automatically in classpath if available. For more details see <a href="http://code.google.com/p/rest-assured/wiki/Usage#Object_Mapping">documentation</a>.
     *
     * Also default object mapper factories will be used.
     */
    public ObjectMapperConfig() {
        defaultObjectMapper = null;
        defaultObjectMapperType = null;
        gsonObjectMapperFactory = new DefaultGsonObjectMapperFactory();
        jacksonObjectMapperFactory = new DefaultJacksonObjectMapperFactory();
        jaxbObjectMapperFactory = new DefaultJAXBObjectMapperFactory();
    }

    /**
     * Creates an object mapper configuration that uses the specified object mapper as default.
     *
     * @param defaultObjectMapperType The object mapper to use. If <code>null</code> then classpath scanning will be used.
     */
    public ObjectMapperConfig(ObjectMapperType defaultObjectMapperType) {
        this(null, defaultObjectMapperType, new DefaultGsonObjectMapperFactory(), new DefaultJacksonObjectMapperFactory(),
                new DefaultJAXBObjectMapperFactory());
    }

    /**
     * Creates an object mapper configuration that uses the specified object mapper as default.
     *
     * @param defaultObjectMapper The object mapper to use. If <code>null</code> then classpath scanning will be used.
     */
    public ObjectMapperConfig(ObjectMapper defaultObjectMapper) {
        this(defaultObjectMapper, null, new DefaultGsonObjectMapperFactory(), new DefaultJacksonObjectMapperFactory(),
                new DefaultJAXBObjectMapperFactory());
    }

    private ObjectMapperConfig(ObjectMapper defaultObjectMapper, ObjectMapperType defaultObjectMapperType,
                               GsonObjectMapperFactory gsonObjectMapperFactory, JacksonObjectMapperFactory jacksonObjectMapperFactory,
                               JAXBObjectMapperFactory jaxbObjectMapperFactory) {
        Validate.notNull(gsonObjectMapperFactory, GsonObjectMapperFactory.class.getSimpleName() + " cannot be null");
        Validate.notNull(jacksonObjectMapperFactory, JacksonObjectMapperFactory.class.getSimpleName() + " cannot be null");
        Validate.notNull(jaxbObjectMapperFactory, JAXBObjectMapperFactory.class.getSimpleName() + " cannot be null");
        this.defaultObjectMapperType = defaultObjectMapperType;
        this.defaultObjectMapper = defaultObjectMapper;
        this.gsonObjectMapperFactory = gsonObjectMapperFactory;
        this.jacksonObjectMapperFactory = jacksonObjectMapperFactory;
        this.jaxbObjectMapperFactory = jaxbObjectMapperFactory;
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
        return new ObjectMapperConfig(defaultObjectMapperType);
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
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory, jacksonObjectMapperFactory, jaxbObjectMapperFactory);
    }

    public JacksonObjectMapperFactory jacksonObjectMapperFactory() {
        return jacksonObjectMapperFactory;
    }

    /**
     * Specify a custom Jackson object mapper factory.
     *
     * @param jacksonObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig jacksonObjectMapperFactory(JacksonObjectMapperFactory jacksonObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory, jacksonObjectMapperFactory, jaxbObjectMapperFactory);
    }

    public JAXBObjectMapperFactory jaxbObjectMapperFactory() {
        return jaxbObjectMapperFactory;
    }

    /**
     * Specify a custom JAXB object mapper factory.
     *
     * @param jaxbObjectMapperFactory The object mapper factory
     */
    public ObjectMapperConfig jaxbObjectMapperFactory(JAXBObjectMapperFactory jaxbObjectMapperFactory) {
        return new ObjectMapperConfig(defaultObjectMapper, defaultObjectMapperType, gsonObjectMapperFactory, jacksonObjectMapperFactory, jaxbObjectMapperFactory);
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
}