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

import io.restassured.path.xml.XmlPath;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows you to configure properties of XML and HTML parsing.
 */
public class XmlConfig implements Config {
    private static final boolean DEFAULT_VALIDATING = false;
    private static final boolean DEFAULT_NAMESPACE_AWARE = true;
    private static final boolean DEFAULT_ALLOW_DOC_TYPE_DECLARATION = false;

    private final Map<String, Object> properties;
    private final Map<String, Boolean> features;
    private final Map<String, String> declaredNamespaces;
    private final boolean validating;
    private final boolean namespaceAware;
    private final boolean allowDocTypeDeclaration;
    private final boolean isUserConfigured;

    /**
     * Create a new instance of XmlConfig without any features and that is namespace unaware.
     */
    public XmlConfig() {
        this(new HashMap<String, Boolean>(), new HashMap<String, String>(), new HashMap<String, Object>(),
                DEFAULT_VALIDATING, DEFAULT_NAMESPACE_AWARE, DEFAULT_ALLOW_DOC_TYPE_DECLARATION, false);
    }

    private XmlConfig(Map<String, Boolean> features, Map<String, String> declaredNamespaces, Map<String, Object> properties,
                      boolean validating, boolean namespaceAware, boolean allowDocTypeDeclaration, boolean isUserConfigured) {
        Validate.notNull(features, "Features cannot be null");
        Validate.notNull(declaredNamespaces, "Declared namespaces cannot be null");
        Validate.notNull(properties, "Properties cannot be null");
        this.validating = validating;
        this.namespaceAware = namespaceAware;
        this.allowDocTypeDeclaration = allowDocTypeDeclaration;
        this.features = features;
        this.declaredNamespaces = declaredNamespaces;
        this.properties = properties;
        this.isUserConfigured = isUserConfigured;
    }

    /**
     * @return A map containing features that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public Map<String, Boolean> features() {
        return new HashMap<String, Boolean>(features);
    }

    /**
     * @return A map containing properties that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @see org.xml.sax.XMLReader#setProperty(String, Object)
     */
    public Map<String, Object> properties() {
        return new HashMap<String, Object>(properties);
    }

    /**
     * Specify features that will be used when parsing XML.
     *
     * @param features A map containing features that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @return
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlConfig features(Map<String, Boolean> features) {
        return new XmlConfig(features, declaredNamespaces, properties, validating, namespaceAware, allowDocTypeDeclaration, true);
    }

    /**
     * Specify properties that will be used when parsing XML.
     *
     * @param properties A map containing properties that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @return
     * @see org.xml.sax.XMLReader#setProperty(String, Object)
     */
    public XmlConfig properties(Map<String, Object> properties) {
        return new XmlConfig(features, declaredNamespaces, this.properties, validating, namespaceAware, allowDocTypeDeclaration, true);
    }

    /**
     * Set a value of a feature flag.
     *
     * @param uri     The feature name, which is a fully-qualified URI.
     * @param enabled The requested value of the feature (true or false).
     * @return A new XmlConfig instance
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlConfig feature(String uri, boolean enabled) {
        Validate.notEmpty(uri, "URI cannot be empty");
        Map<String, Boolean> newFeatures = new HashMap<String, Boolean>(features);
        newFeatures.put(uri, enabled);
        return new XmlConfig(newFeatures, declaredNamespaces, properties, validating, namespaceAware, allowDocTypeDeclaration, true);
    }

    /**
     * Set a value of a property.
     *
     * @param name  The property name.
     * @param value The requested value of the feature (true or false).
     * @return A new XmlConfig instance
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlConfig property(String name, Object value) {
        Validate.notEmpty(name, "Name cannot be empty");
        Map<String, Object> newProperties = new HashMap<String, Object>(properties);
        newProperties.put(name, value);
        return new XmlConfig(features, declaredNamespaces, newProperties, validating, namespaceAware, allowDocTypeDeclaration, true);
    }

    /**
     * @return A map containing namespaces that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public Map<String, String> declaredNamespaces() {
        return new HashMap<String, String>(declaredNamespaces);
    }

    /**
     * Specify declared namespaces that will be used when parsing XML. Will also set {@link #namespaceAware(boolean)} to <code>true</code> of namespaces are not empty.
     * <p>Note that you cannot use this to add namespaces for the {@link org.hamcrest.xml.HasXPath} matcher.
     * This has to be done by providing a {@link javax.xml.namespace.NamespaceContext} to the matcher instance.</p>
     *
     * @param namespacesToDeclare A map containing features that will be used by the underlying {@link groovy.xml.XmlSlurper}.
     * @return A new instance of XmlConfig
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlConfig declareNamespaces(Map<String, String> namespacesToDeclare) {
        final boolean shouldBeNamespaceAware = namespacesToDeclare == null ? namespaceAware : !namespacesToDeclare.isEmpty();
        return new XmlConfig(features, namespacesToDeclare, properties, validating, shouldBeNamespaceAware, allowDocTypeDeclaration, true);
    }

    /**
     * Declares a namespace and also sets {@link #namespaceAware(boolean)} to <code>true</code>.
     * <p/>
     * <p>Note that you cannot use this to add namespaces for the {@link org.hamcrest.xml.HasXPath} matcher.
     * This has to be done by providing a {@link javax.xml.namespace.NamespaceContext} to the matcher instance.</p>
     *
     * @param prefix       The feature name, which is a fully-qualified URI.
     * @param namespaceURI The requested value of the feature (true or false).
     * @return A new XmlConfig instance
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlConfig declareNamespace(String prefix, String namespaceURI) {
        Validate.notEmpty(prefix, "Prefix cannot be empty");
        Validate.notEmpty(namespaceURI, "Namespace URI cannot be empty");
        Map<String, String> updatedNamespaces = new HashMap<String, String>(declaredNamespaces);
        updatedNamespaces.put(prefix, namespaceURI);
        return new XmlConfig(features, updatedNamespaces, properties, validating, true, allowDocTypeDeclaration, true);
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
     * @return A new XmlConfig instance
     * @see #feature(String, boolean)
     */
    public XmlConfig disableLoadingOfExternalDtd() {
        Map<String, Boolean> newFeatures = new HashMap<String, Boolean>(features);
        newFeatures.put("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return new XmlConfig(newFeatures, declaredNamespaces, properties, validating, namespaceAware, allowDocTypeDeclaration, true);
    }

    /**
     * Configure if XmlPath should validate documents as they are parsed (default is {@value #DEFAULT_VALIDATING}).
     * Note that this is only applicable when {@link XmlPath.CompatibilityMode} is equal to
     * {@link XmlPath.CompatibilityMode#XML}.
     *
     * @param isValidating <code>true</code> if the parser should validate documents as they are parsed; <code>false</code> otherwise.
     * @return A new XmlPathConfig instance
     */
    public XmlConfig validating(boolean isValidating) {
        return new XmlConfig(features, declaredNamespaces, properties, isValidating, namespaceAware, allowDocTypeDeclaration, true);
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
     * Configure whether or not REST Assured should be aware of namespaces when parsing XML (default is {@value #DEFAULT_NAMESPACE_AWARE}).
     * Note that this is only applicable when {@link XmlPath.CompatibilityMode} is equal to
     * {@link XmlPath.CompatibilityMode#XML}.
     *
     * @param shouldBeAwareOfNamespaces <code>true</code> if xml parsing should take namespaces into account.
     * @return A new XmlConfig instance
     */
    public XmlConfig namespaceAware(boolean shouldBeAwareOfNamespaces) {
        return new XmlConfig(features, declaredNamespaces, properties, validating, shouldBeAwareOfNamespaces, allowDocTypeDeclaration, true);
    }

    /**
     * @return <code>true</code> if REST Assured should be namespace aware.
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
    public XmlConfig allowDocTypeDeclaration(boolean allowDocTypeDeclaration) {
        return new XmlConfig(features, declaredNamespaces, properties, validating, namespaceAware, allowDocTypeDeclaration, true);
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
     * For syntactic sugar.
     *
     * @return The same XmlConfig instance
     */
    public XmlConfig with() {
        return this;
    }

    /**
     * For syntactic sugar.
     *
     * @return The same XmlConfig instance
     */
    public XmlConfig and() {
        return this;
    }

    /**
     * @return A static way to create a new XmlConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static XmlConfig xmlConfig() {
        return new XmlConfig();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUserConfigured() {
        return isUserConfigured;
    }
}

