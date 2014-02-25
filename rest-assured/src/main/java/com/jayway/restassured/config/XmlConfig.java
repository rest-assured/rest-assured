package com.jayway.restassured.config;

import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows you to configure properties of XML and HTML parsing.
 */
public class XmlConfig {
    private final Map<String, Object> properties;
    private final Map<String, Boolean> features;
    private final Map<String, String> declaredNamespaces;
    private final boolean namespaceAware;

    /**
     * Create a new instance of XmlConfig without any features and that is namespace unaware.
     */
    public XmlConfig() {
        this(new HashMap<String, Boolean>(), new HashMap<String, String>(), new HashMap<String, Object>(), false);
    }

    private XmlConfig(Map<String, Boolean> features, Map<String, String> declaredNamespaces, Map<String, Object> properties, boolean namespaceAware) {
        Validate.notNull(features, "Features cannot be null");
        Validate.notNull(declaredNamespaces, "Declared namespaces cannot be null");
        Validate.notNull(properties, "Properties cannot be null");
        this.namespaceAware = namespaceAware;
        this.features = features;
        this.declaredNamespaces = declaredNamespaces;
        this.properties = properties;
    }

    /**
     * @return A map containing features that will be used by the underlying {@link groovy.util.XmlSlurper}.
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public Map<String, Boolean> features() {
        return new HashMap<String, Boolean>(features);
    }

    /**
     * @return A map containing properties that will be used by the underlying {@link groovy.util.XmlSlurper}.
     * @see org.xml.sax.XMLReader#setProperty(String, Object)
     */
    public Map<String, Object> properties() {
        return new HashMap<String, Object>(properties);
    }

    /**
     * Specify features that will be used when parsing XML.
     *
     * @param features A map containing features that will be used by the underlying {@link groovy.util.XmlSlurper}.
     * @return
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlConfig features(Map<String, Boolean> features) {
        return new XmlConfig(features, declaredNamespaces, properties, namespaceAware);
    }

    /**
     * Specify properties that will be used when parsing XML.
     *
     * @param properties A map containing properties that will be used by the underlying {@link groovy.util.XmlSlurper}.
     * @return
     * @see org.xml.sax.XMLReader#setProperty(String, Object)
     */
    public XmlConfig properties(Map<String, Object> properties) {
        return new XmlConfig(features, declaredNamespaces, this.properties, namespaceAware);
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
        return new XmlConfig(newFeatures, declaredNamespaces, properties, namespaceAware);
    }

    /**
     * Set a value of a property.
     *
     * @param name     The property name.
     * @param value The requested value of the feature (true or false).
     * @return A new XmlConfig instance
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlConfig property(String name, Object value) {
        Validate.notEmpty(name, "Name cannot be empty");
        Map<String, Object> newProperties = new HashMap<String, Object>(properties);
        newProperties.put(name, value);
        return new XmlConfig(features, declaredNamespaces, newProperties, namespaceAware);
    }

    /**
     * @return A map containing namespaces that will be used by the underlying {@link groovy.util.XmlSlurper}.
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
     * @param namespacesToDeclare A map containing features that will be used by the underlying {@link groovy.util.XmlSlurper}.
     * @return A new instance of XmlConfig
     * @see org.xml.sax.XMLReader#setFeature(java.lang.String, boolean)
     */
    public XmlConfig declareNamespaces(Map<String, String> namespacesToDeclare) {
        final boolean shouldBeNamespaceAware = namespacesToDeclare == null ? namespaceAware : !namespacesToDeclare.isEmpty();
        return new XmlConfig(features, namespacesToDeclare, properties, shouldBeNamespaceAware);
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
        return new XmlConfig(features, updatedNamespaces, properties, true);
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
        return new XmlConfig(newFeatures, declaredNamespaces, properties, namespaceAware);
    }

    /**
     * Configure whether or not REST Assured should be aware of namespaces when parsing XML.
     *
     * @param shouldBeAwareOfNamespaces <code>true</code> if xml parsing should take namespaces into account.
     * @return A new XmlConfig instance
     */
    public XmlConfig namespaceAware(boolean shouldBeAwareOfNamespaces) {
        return new XmlConfig(features, declaredNamespaces, properties, shouldBeAwareOfNamespaces);
    }

    /**
     * @return <code>true</code> if REST Assured should be namespace aware.
     */
    public boolean isNamespaceAware() {
        return namespaceAware;
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

}

