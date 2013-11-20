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
import org.apache.commons.lang3.Validate;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


/**
 * Allows you to configure how XmlPath will handle object mapping (de-serialization).
 */
public class XmlPathConfig {

    private final XmlPathObjectDeserializer defaultDeserializer;
    private final JAXBObjectMapperFactory jaxbObjectMapperFactory;
    private final XmlParserType defaultParserType;
    private final String charset;
    private final Map<String, Boolean> features;


    /**
     * Create a new instance of a XmlPathConfig based on the properties in the supplied config.
     *
     * @param config The config to copy.
     */
    public XmlPathConfig(XmlPathConfig config) {
        this(config.jaxbObjectMapperFactory(), config.defaultParserType(), config.defaultDeserializer(), config.charset(),
                new HashMap<String, Boolean>());
    }

    /**
     * Creates a new XmlPathConfig that is configured to use the default JAXBObjectMapperFactory.
     */
    public XmlPathConfig() {
        this(new DefaultJAXBObjectMapperFactory(), null, null, defaultCharset(), new HashMap<String, Boolean>());
    }


    /**
     * Create a new XmlPathConfig that uses the <code>defaultCharset</code> when deserializing XML data.
     */
    public XmlPathConfig(String defaultCharset) {
        this(new DefaultJAXBObjectMapperFactory(), null, null, defaultCharset, new HashMap<String, Boolean>());

    }

    private XmlPathConfig(JAXBObjectMapperFactory jaxbObjectMapperFactory, XmlParserType defaultParserType,
                          XmlPathObjectDeserializer defaultDeserializer, String charset, Map<String, Boolean> features) {
        charset = StringUtils.trimToNull(charset);
        if (charset == null) throw new IllegalArgumentException("Charset cannot be empty");
        this.charset = charset;
        this.defaultDeserializer = defaultDeserializer;
        this.defaultParserType = defaultParserType;
        this.jaxbObjectMapperFactory = jaxbObjectMapperFactory;
        this.features = features;
    }

    private static String defaultCharset() {
        return Charset.defaultCharset().name();
    }

    /**
     * @return A map containing features that will be used by the underlying {@link groovy.util.XmlSlurper}.
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public Map<String, Boolean> features() {
        return new HashMap<String, Boolean>(features);
    }

    /**
     * Specify features that will be used when parsing XML.
     *
     * @param features A map containing features that will be used by the underlying {@link groovy.util.XmlSlurper}.
     * @return
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlPathConfig features(Map<String, Boolean> features) {
        Validate.notNull(features, "Features cannot be null");
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features);
    }

    /**
     * Set a value of a feature flag.
     *
     * @param uri     The feature name, which is a fully-qualified URI.
     * @param enabled The requested value of the feature (true or false).
     * @return A new XmlPathConfig instance
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlPathConfig feature(String uri, boolean enabled) {
        Validate.notEmpty(uri, "URI cannot be empty");
        Map<String, Boolean> newFeatures = new HashMap<String, Boolean>(features);
        newFeatures.put(uri, enabled);
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, newFeatures);
    }

    /**
     * Disables external DTD loading.
     * <p>
     * This is a shortcut for doing:<br>
     * <pre>
     * setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
     * </pre>
     * </p>
     *
     * @return A new XmlPathConfig instance
     * @see #feature(String, boolean)
     */
    public XmlPathConfig disableLoadingOfExternalDtd() {
        Map<String, Boolean> newFeatures = new HashMap<String, Boolean>(features);
        newFeatures.put("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, newFeatures);
    }

    /**
     * @return The charset to assume when parsing XML data
     */
    public String charset() {
        return charset;
    }

    /**
     * @return A new XmlPathConfig instance with that assumes the supplied charset when parsing XML documents.
     */
    public XmlPathConfig charset(String charset) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features);
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
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features);
    }

    /**
     * Creates an json path configuration that uses the specified object de-serializer as default.
     *
     * @param defaultObjectDeserializer The object de-serializer to use. If <code>null</code> then classpath scanning will be used.
     */
    public XmlPathConfig defaultObjectDeserializer(XmlPathObjectDeserializer defaultObjectDeserializer) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultObjectDeserializer, charset, features);
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
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features);
    }

    /**
     * For syntactic sugar.
     *
     * @return The same XmlPathConfig instance
     */
    public XmlPathConfig with() {
        return this;
    }

    /**
     * For syntactic sugar.
     *
     * @return The same XmlPathConfig instance
     */
    public XmlPathConfig and() {
        return this;
    }

    /**
     * @return A static way to create a new XmlPathConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static XmlPathConfig xmlPathConfig() {
        return new XmlPathConfig();
    }
}
