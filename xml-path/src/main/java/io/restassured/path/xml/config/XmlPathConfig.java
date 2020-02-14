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

package io.restassured.path.xml.config;

import io.restassured.path.xml.XmlPath;
import io.restassured.path.xml.mapper.factory.DefaultJAXBObjectMapperFactory;
import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory;
import io.restassured.path.xml.mapping.XmlPathObjectDeserializer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;


/**
 * Allows you to configure how XmlPath will handle object mapping (de-serialization).
 */
public class XmlPathConfig {

    private static final boolean DEFAULT_VALIDATING = false;
    private static final boolean DEFAULT_NAMESPACE_AWARE = true;
    private static final boolean DEFAULT_ALLOW_DOC_TYPE_DECLARATION = false;

    private final XmlPathObjectDeserializer defaultDeserializer;
    private final JAXBObjectMapperFactory jaxbObjectMapperFactory;
    private final XmlParserType defaultParserType;
    private final String charset;
    private final Map<String, Boolean> features;
    private final Map<String, Object> properties;
    private final Map<String, String> declaredNamespaces;
    private final boolean validating;
    private final boolean namespaceAware;
    private final boolean allowDocTypeDeclaration;

    /**
     * Create a new instance of a XmlPathConfig based on the properties in the supplied config.
     *
     * @param config The config to copy.
     */
    public XmlPathConfig(XmlPathConfig config) {
        this(config.jaxbObjectMapperFactory(), config.defaultParserType(), config.defaultDeserializer(), config.charset(),
                new HashMap<String, Boolean>(), new HashMap<String, String>(), new HashMap<String, Object>(), config.isValidating(),
                config.isNamespaceAware(), config.isAllowDocTypeDeclaration());
    }

    /**
     * Creates a new XmlPathConfig that is configured to use the default JAXBObjectMapperFactory.
     */
    public XmlPathConfig() {
        this(new DefaultJAXBObjectMapperFactory(), null, null, defaultCharset(), new HashMap<String, Boolean>(), new HashMap<String, String>(),
                new HashMap<String, Object>(), DEFAULT_VALIDATING, DEFAULT_NAMESPACE_AWARE, DEFAULT_ALLOW_DOC_TYPE_DECLARATION);
    }


    /**
     * Create a new XmlPathConfig that uses the <code>defaultCharset</code> when deserializing XML data.
     */
    public XmlPathConfig(String defaultCharset) {
        this(new DefaultJAXBObjectMapperFactory(), null, null, defaultCharset, new HashMap<String, Boolean>(), new HashMap<String, String>(), new HashMap<String, Object>(),
                DEFAULT_VALIDATING, DEFAULT_NAMESPACE_AWARE, DEFAULT_ALLOW_DOC_TYPE_DECLARATION);

    }

    private XmlPathConfig(JAXBObjectMapperFactory jaxbObjectMapperFactory, XmlParserType defaultParserType,
                          XmlPathObjectDeserializer defaultDeserializer, String charset, Map<String, Boolean> features,
                          Map<String, String> declaredNamespaces, Map<String, Object> properties, boolean validating,
                          boolean namespaceAware, boolean allowDocTypeDeclaration) {
        charset = StringUtils.trimToNull(charset);
        if (charset == null) throw new IllegalArgumentException("Charset cannot be empty");
        this.charset = charset;
        this.defaultDeserializer = defaultDeserializer;
        this.defaultParserType = defaultParserType;
        this.jaxbObjectMapperFactory = jaxbObjectMapperFactory;
        this.features = features;
        this.declaredNamespaces = declaredNamespaces;
        this.properties = properties;
        this.validating = validating;
        this.namespaceAware = namespaceAware;
        this.allowDocTypeDeclaration = allowDocTypeDeclaration;
    }

    private static String defaultCharset() {
        return Charset.defaultCharset().name();
    }

    /**
     * @return A map containing features that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public Map<String, Boolean> features() {
        return new HashMap<String, Boolean>(features);
    }

    /**
     * Specify features that will be used when parsing XML.
     *
     * @param features A map containing features that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @return
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlPathConfig features(Map<String, Boolean> features) {
        Validate.notNull(features, "Features cannot be null");
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, declaredNamespaces,
                properties, validating, namespaceAware, allowDocTypeDeclaration);
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
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, newFeatures, declaredNamespaces, properties, validating, namespaceAware, allowDocTypeDeclaration);
    }

    /**
     * Configure if XmlPath should validate documents as they are parsed (default is {@value #DEFAULT_VALIDATING}).
     * Note that this is only applicable when {@link XmlPath.CompatibilityMode} is equal to
     * {@link XmlPath.CompatibilityMode#XML}.
     *
     * @param isValidating <code>true</code> if the parser should validate documents as they are parsed; <code>false</code> otherwise.
     * @return A new XmlPathConfig instance
     */
    public XmlPathConfig validating(boolean isValidating) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, declaredNamespaces, properties, isValidating, namespaceAware, allowDocTypeDeclaration);
    }

    /**
     * Whether XmlPath should validate documents as they are parsed.
     *
     * @return a boolean indicating whether this is true or false
     */
    public boolean isValidating() {
        return validating;
    }

    /**
     * Configure if XmlPath should provide support for XML namespaces (default is {@value #DEFAULT_NAMESPACE_AWARE}).
     * Note that this is only applicable when {@link XmlPath.CompatibilityMode} is equal to
     * {@link XmlPath.CompatibilityMode#XML}.
     *
     * @param namespaceAware <code>true</code> if the parser should provide support for XML namespaces; <code>false</code> otherwise.
     * @return A new XmlPathConfig instance
     */
    public XmlPathConfig namespaceAware(boolean namespaceAware) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, declaredNamespaces, properties, validating, namespaceAware, allowDocTypeDeclaration);
    }

    /**
     * Whether XmlPath should provide support for XML namespaces
     *
     * @return a boolean indicating whether this is true or false
     */
    public boolean isNamespaceAware() {
        return namespaceAware;
    }

    /**
     * Configure if XmlPath should provide support for DOCTYPE declarations (default is {@value #DEFAULT_ALLOW_DOC_TYPE_DECLARATION}).
     * Note that this is only applicable when {@link XmlPath.CompatibilityMode} is equal to
     * {@link XmlPath.CompatibilityMode#XML}.
     *
     * @param allowDocTypeDeclaration <code>true</code> if the parser should provide support for DOCTYPE declarations; <code>false</code> otherwise.
     * @return A new XmlPathConfig instance
     */
    public XmlPathConfig allowDocTypeDeclaration(boolean allowDocTypeDeclaration) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, declaredNamespaces, properties, validating, namespaceAware, allowDocTypeDeclaration);
    }

    /**
     * Whether XmlPath should provide support for DOCTYPE declarations
     *
     * @return a boolean indicating whether this is true or false
     */
    public boolean isAllowDocTypeDeclaration() {
        return allowDocTypeDeclaration;
    }

    /**
     * @return A map containing properties that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @see org.xml.sax.XMLReader#setProperty(String, Object)
     */
    public Map<String, Object> properties() {
        return new HashMap<String, Object>(properties);
    }

    /**
     * Specify properties that will be used when parsing XML.
     *
     * @param properties A map containing properties that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @return A new XmlPathConfig instance
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlPathConfig properties(Map<String, Object> properties) {
        Validate.notNull(properties, "Properties cannot be null");
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, declaredNamespaces,
                properties, validating, namespaceAware, allowDocTypeDeclaration);
    }

    /**
     * Set a value of a property.
     *
     * @param name  The property name
     * @param value The value of the property
     * @return A new XmlPathConfig instance
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlPathConfig property(String name, Object value) {
        Validate.notEmpty(name, "Name cannot be empty");
        Map<String, Object> newProperties = new HashMap<String, Object>(properties);
        newProperties.put(name, value);
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, declaredNamespaces,
                newProperties, validating, namespaceAware, allowDocTypeDeclaration);
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
        newFeatures.put("http://apache.org/xml/features/disallow-doctype-decl", false);
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, newFeatures, declaredNamespaces,
                properties, validating, namespaceAware, allowDocTypeDeclaration);
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
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, declaredNamespaces,
                properties, validating, namespaceAware, allowDocTypeDeclaration);
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
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, declaredNamespaces,
                properties, validating, namespaceAware, allowDocTypeDeclaration);
    }

    /**
     * Creates an json path configuration that uses the specified object de-serializer as default.
     *
     * @param defaultObjectDeserializer The object de-serializer to use. If <code>null</code> then classpath scanning will be used.
     */
    public XmlPathConfig defaultObjectDeserializer(XmlPathObjectDeserializer defaultObjectDeserializer) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultObjectDeserializer, charset, features, declaredNamespaces,
                properties, validating, namespaceAware, allowDocTypeDeclaration);
    }

    public JAXBObjectMapperFactory jaxbObjectMapperFactory() {
        return jaxbObjectMapperFactory;
    }

    /**
     * @return A map containing namespaces that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public Map<String, String> declaredNamespaces() {
        return new HashMap<String, String>(declaredNamespaces);
    }

    /**
     * Specify declared namespaces that will be used when parsing XML.
     *
     * @param namespacesToDeclare A map containing features that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @return A new instance of XmlPathConfig
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlPathConfig declareNamespaces(Map<String, String> namespacesToDeclare) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, namespacesToDeclare,
                properties, validating, namespaceAware, allowDocTypeDeclaration);
    }

    /**
     * Declares a namespace.
     *
     * @param prefix       The feature name, which is a fully-qualified URI.
     * @param namespaceURI The requested value of the feature (true or false).
     * @return A new XmlPathConfig instance
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlPathConfig declaredNamespace(String prefix, String namespaceURI) {
        Validate.notEmpty(prefix, "Prefix cannot be empty");
        Validate.notEmpty(namespaceURI, "Namespace URI cannot be empty");
        Map<String, String> updatedNamespaces = new HashMap<String, String>(declaredNamespaces);
        updatedNamespaces.put(prefix, namespaceURI);
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, updatedNamespaces,
                properties, validating, true, allowDocTypeDeclaration);
    }

    /**
     * Specify a custom Gson object mapper factory.
     *
     * @param jaxbObjectMapperFactory The object mapper factory
     */
    public XmlPathConfig jaxbObjectMapperFactory(JAXBObjectMapperFactory jaxbObjectMapperFactory) {
        return new XmlPathConfig(jaxbObjectMapperFactory, defaultParserType, defaultDeserializer, charset, features, declaredNamespaces,
                properties, validating, namespaceAware, allowDocTypeDeclaration);
    }

    public boolean hasDeclaredNamespaces() {
        return !declaredNamespaces.isEmpty();
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
