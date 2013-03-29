/*
 * Copyright 2013 the original author or authors.
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

package com.jayway.restassured.path.xml.config;

import com.jayway.restassured.mapper.factory.DefaultJAXBObjectMapperFactory;
import com.jayway.restassured.mapper.factory.JAXBObjectMapperFactory;
import com.jayway.restassured.path.xml.mapping.XmlPathObjectDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;


/**
 * Allows you to configure how XmlPath will handle object mapping (de-serialization).
 */
public class XmlPathConfig {

    private final XmlPathObjectDeserializer defaultDeserializer;
    private final JAXBObjectMapperFactory jaxbObjectMapperFactory;
    private final XmlParserType defaultParserType;
    private final String charset;


    /**
     * Create a new instance of a XmlPathConfig based on the properties in the supplied config.
     *
     * @param config The config to copy.
     */
    public XmlPathConfig(XmlPathConfig config) {
        this(config.jaxbObjectMapperFactory(), config.defaultParserType(), config.defaultDeserializer(), config.charset());
    }

    /**
     * Creates a new XmlPathConfig that is configured to use the default JAXBObjectMapperFactory.
     */
    public XmlPathConfig() {
        this(new DefaultJAXBObjectMapperFactory(), null, null, defaultCharset());
    }


    /**
     * Create a new XmlPathConfig that uses the <code>defaultCharset</code> when deserializing XML data.
     */
    public XmlPathConfig(String defaultCharset) {
        this(new DefaultJAXBObjectMapperFactory(), null, null, defaultCharset);

    }

    private XmlPathConfig(JAXBObjectMapperFactory jaxbObjectMapperFactory, XmlParserType defaultParserType,
                          XmlPathObjectDeserializer defaultDeserializer, String charset) {
        charset = StringUtils.trimToNull(charset);
        if (charset == null) throw new IllegalArgumentException("Charset cannot be empty");
        this.charset = charset;
        this.defaultDeserializer = defaultDeserializer;
        this.defaultParserType = defaultParserType;
        this.jaxbObjectMapperFactory = jaxbObjectMapperFactory;
    }

    private static String defaultCharset() {
        return Charset.defaultCharset().name();
    }

    /**
     * @return The charset to assume when parsing XML data
     */
    public String charset() {
        return charset;
    }

    /**
     * @return A new XmlPathConfig instance with that assumes the  supplied charset when parsing XML documents.
     */
    public XmlPathConfig charset(String charset) {
        return new XmlPathConfig();
    }

    public XmlParserType defaultParserType() {
        return defaultParserType;
    }

    public boolean hasDefaultParserType() {
        return defaultParserType != null;
    }

    public boolean hasCustomJaxbObjectMapperFactory() {
        return jaxbObjectMapperFactory() != null && jaxbObjectMapperFactory().getClass() != DefaultJAXBObjectMapperFactory.class;
    }

    public XmlPathObjectDeserializer defaultDeserializer() {
        return defaultDeserializer;
    }

    public boolean hasDefaultDeserializer() {
        return defaultDeserializer != null;
    }

    /**
     * Creates an xml path configuration that uses the specified parser type as default.
     *
     * @param defaultParserType The default parser type to use. If <code>null</code> then classpath scanning will be used.
     */
    public XmlPathConfig defaultParserType(XmlParserType defaultParserType) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset);
    }

    /**
     * Creates an json path configuration that uses the specified object de-serializer as default.
     *
     * @param defaultObjectDeserializer The object de-serializer to use. If <code>null</code> then classpath scanning will be used.
     */
    public XmlPathConfig defaultObjectDeserializer(XmlPathObjectDeserializer defaultObjectDeserializer) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultObjectDeserializer, charset);
    }

    public JAXBObjectMapperFactory jaxbObjectMapperFactory() {
        return jaxbObjectMapperFactory;
    }

    /**
     * Specify a custom Gson object mapper factory.
     *
     * @param jaxbObjectMapperFactory The object mapper factory
     */
    public XmlPathConfig jaxbObjectMapperFactory(JAXBObjectMapperFactory jaxbObjectMapperFactory) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset);
    }

    /**
     * @return A static way to create a new XmlPathConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static XmlPathConfig xmlPathConfig() {
        return new XmlPathConfig();
    }
}
