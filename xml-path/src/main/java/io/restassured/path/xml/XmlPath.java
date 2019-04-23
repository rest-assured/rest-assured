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

package io.restassured.path.xml;

import groovy.lang.GroovyRuntimeException;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import groovy.xml.XmlUtil;
import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.internal.common.path.ObjectConverter;
import io.restassured.internal.path.xml.*;
import io.restassured.internal.path.xml.mapping.XmlObjectDeserializer;
import io.restassured.path.xml.config.XmlParserType;
import io.restassured.path.xml.config.XmlPathConfig;
import io.restassured.path.xml.element.Node;
import io.restassured.path.xml.element.NodeChildren;
import io.restassured.path.xml.exception.XmlPathException;
import io.restassured.path.xml.mapper.factory.JAXBObjectMapperFactory;
import org.apache.commons.lang3.Validate;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import static io.restassured.path.xml.XmlPath.CompatibilityMode.XML;

/**
 * XmlPath is an alternative to using XPath for easily getting values from an XML document. It follows the Groovy <a href="http://docs.groovy-lang.org/latest/html/documentation/#_gpath">GPath</a> syntax
 * described <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">here</a>. <br>Let's say we have an XML defined as;
 * <pre>
 * &lt;shopping&gt;
 * &lt;category type=&quot;groceries&quot;&gt;
 * &lt;item&gt;
 * &lt;name&gt;Chocolate&lt;/name&gt;
 * &lt;price&gt;10&lt;/price&gt;
 * &lt;/item&gt;
 * &lt;item&gt;
 * &lt;name&gt;Coffee&lt;/name&gt;
 * &lt;price&gt;20&lt;/price&gt;
 * &lt;/item&gt;
 * &lt;/category&gt;
 * &lt;category type=&quot;supplies&quot;&gt;
 * &lt;item&gt;
 * &lt;name&gt;Paper&lt;/name&gt;
 * &lt;price&gt;5&lt;/price&gt;
 * &lt;/item&gt;
 * &lt;item quantity=&quot;4&quot;&gt;
 * &lt;name&gt;Pens&lt;/name&gt;
 * &lt;price&gt;15&lt;/price&gt;
 * &lt;/item&gt;
 * &lt;/category&gt;
 * &lt;category type=&quot;present&quot;&gt;
 * &lt;item when=&quot;Aug 10&quot;&gt;
 * &lt;name&gt;Kathryn&#39;s Birthday&lt;/name&gt;
 * &lt;price&gt;200&lt;/price&gt;
 * &lt;/item&gt;
 * &lt;/category&gt;
 * &lt;/shopping&gt;
 * </pre>
 * <p/>
 * Get the name of the first category item:
 * <pre>
 *     String name = with(XML).get("shopping.category.item[0].name");
 * </pre>
 * <p/>
 * To get the number of category items:
 * <pre>
 *     int items = with(XML).get("shopping.category.item.size()");
 * </pre>
 * <p/>
 * Get a specific category:
 * <pre>
 *     Node category = with(XML).get("shopping.category[0]");
 * </pre>
 * <p/>
 * To get the number of categories with type attribute equal to 'groceries':
 * <pre>
 *    int items = with(XML).get("shopping.category.findAll { it.@type == 'groceries' }.size()");
 * </pre>
 * <p/>
 * Get all items with price greater than or equal to 10 and less than or equal to 20:
 * <pre>
 * List&lt;Node&gt; itemsBetweenTenAndTwenty = with(XML).get("shopping.category.item.findAll { item -> def price = item.price.toFloat(); price >= 10 && price <= 20 }");
 * </pre>
 * <p/>
 * Get the chocolate price:
 * <pre>
 * int priceOfChocolate = with(XML).getInt("**.find { it.name == 'Chocolate' }.price"
 * </pre>
 * <p/>
 * You can also parse HTML by setting compatibility mode to HTML:
 * <pre>
 * XmlPath xmlPath = new XmlPath(CompatibilityMode.HTML,&lt;some html&gt;);
 * </pre>
 * The XmlPath implementation of rest-assured uses a Groovy shell to evaluate expressions so be careful when injecting
 * user input into the expression. For example avoid doing this:
 * <pre>
 * String type = System.console().readLine();
 * List&lt;Map&gt; books = with(Object).get("shopping.category.findAll { it.@type == '"+type+"' }");
 * </pre>
 * Instead use the {@link #param(java.lang.String, java.lang.Object)} method like this:
 * <pre>
 * String type = System.console().readLine();
 * List&lt;Map&gt; books = with(Object).param("type", type).get("shopping.category.findAll { it.@type == type}");
 * </pre>
 */
public class XmlPath {
    public static XmlPathConfig config = null;

    private final CompatibilityMode mode;
    private LazyXmlParser lazyXmlParser;
    private XmlPathConfig xmlPathConfig = null;
    /**
     * Parameters for groovy console (not initialized here to save memory for queries that don't use params)
     */
    private Map<String, Object> params;

    private String rootPath = "";

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param text The text containing the XML document
     */
    public XmlPath(String text) {
        this(XML, text);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param stream The stream containing the XML document
     */
    public XmlPath(InputStream stream) {
        this(XML, stream);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param source The source containing the XML document
     */
    public XmlPath(InputSource source) {
        this(XML, source);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param file The file containing the XML document
     */
    public XmlPath(File file) {
        this(XML, file);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param reader The reader containing the XML document
     */
    public XmlPath(Reader reader) {
        this(XML, reader);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param uri The URI containing the XML document
     */
    public XmlPath(URI uri) {
        this(XML, uri);
    }


    /**
     * Instantiate a new XmlPath instance.
     *
     * @param mode The compatibility mode
     * @param text The text containing the XML document
     */
    public XmlPath(CompatibilityMode mode, String text) {
        Validate.notNull(mode, "Compatibility mode cannot be null");
        this.mode = mode;
        lazyXmlParser = parseText(text);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param mode   The compatibility mode
     * @param stream The stream containing the XML document
     */
    public XmlPath(CompatibilityMode mode, InputStream stream) {
        Validate.notNull(mode, "Compatibility mode cannot be null");
        this.mode = mode;
        lazyXmlParser = parseInputStream(stream);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param mode   The compatibility mode
     * @param source The source containing the XML document
     */
    public XmlPath(CompatibilityMode mode, InputSource source) {
        Validate.notNull(mode, "Compatibility mode cannot be null");
        this.mode = mode;
        lazyXmlParser = parseInputSource(source);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param mode The compatibility mode
     * @param file The file containing the XML document
     */
    public XmlPath(CompatibilityMode mode, File file) {
        Validate.notNull(mode, "Compatibility mode cannot be null");
        this.mode = mode;
        lazyXmlParser = parseFile(file);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param mode   The compatibility mode
     * @param reader The reader containing the XML document
     */
    public XmlPath(CompatibilityMode mode, Reader reader) {
        Validate.notNull(mode, "Compatibility mode cannot be null");
        this.mode = mode;
        lazyXmlParser = parseReader(reader);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param mode The compatibility mode
     * @param uri  The URI containing the XML document
     */
    public XmlPath(CompatibilityMode mode, URI uri) {
        Validate.notNull(mode, "Compatibility mode cannot be null");
        this.mode = mode;
        lazyXmlParser = parseURI(uri);
    }

    /**
     * Configure XmlPath to use a specific JAXB object mapper factory
     *
     * @param factory The JAXB object mapper factory instance
     * @return a new XmlPath instance
     */
    public XmlPath using(JAXBObjectMapperFactory factory) {
        return new XmlPath(this, getXmlPathConfig().jaxbObjectMapperFactory(factory));
    }

    /**
     * Configure XmlPath to with a specific XmlPathConfig.
     *
     * @param config The XmlPath config
     * @return a new XmlPath instance
     */
    public XmlPath using(XmlPathConfig config) {
        return new XmlPath(this, config);
    }

    private XmlPath(XmlPath xmlPath, XmlPathConfig config) {
        this.xmlPathConfig = config;
        this.mode = xmlPath.mode;
        this.lazyXmlParser = xmlPath.lazyXmlParser.changeCompatibilityMode(mode).changeConfig(config);
        if (xmlPath.params != null) {
            this.params = new HashMap<String, Object>(xmlPath.params);
        }
    }

    /**
     * Get the entire XML graph as an Object
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @return The XML Node. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public Node get() {
        return (Node) get("$");
    }

    /**
     * Get the result of an XML path expression. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @param <T>  The type of the return value.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> T get(String path) {
        AssertParameter.notNull(path, "path");
        return getFromPath(path, true);
    }

    /**
     * Get the result of an XML path expression as a list. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @param <T>  The list type
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> List<T> getList(String path) {
        return getAsList(path);
    }

    /**
     * Get the result of an XML path expression as a list. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path        The XML path.
     * @param genericType The generic list type
     * @param <T>         The type
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> List<T> getList(String path, Class<T> genericType) {
        return getAsList(path, genericType);
    }

    /**
     * Get the result of an XML path expression as a map. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @param <K>  The type of the expected key
     * @param <V>  The type of the expected value
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <K, V> Map<K, V> getMap(String path) {
        return get(path);
    }

    /**
     * Get the result of an XML path expression as a map. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path      The XML path.
     * @param keyType   The type of the expected key
     * @param valueType The type of the expected value
     * @param <K>       The type of the expected key
     * @param <V>       The type of the expected value
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <K, V> Map<K, V> getMap(String path, Class<K> keyType, Class<V> valueType) {
        final Map<K, V> originalMap = get(path);
        final Map<K, V> newMap = new HashMap<K, V>();
        for (Entry<K, V> entry : originalMap.entrySet()) {
            final K key = entry.getKey() == null ? null : convertObjectTo(entry.getKey(), keyType);
            final V value = entry.getValue() == null ? null : convertObjectTo(entry.getValue(), valueType);
            newMap.put(key, value);
        }
        return Collections.unmodifiableMap(newMap);
    }

    /**
     * Get an XML document as a Java Object.
     *
     * @param objectType The type of the java object.
     * @param <T>        The type of the java object
     * @return A Java object representation of the XML document
     */
    public <T> T getObject(String path, Class<T> objectType) {
        Object object = getFromPath(path, false);
        return getObjectAsType(object, objectType);
    }

    private <T> T getObjectAsType(Object object, Class<T> objectType) {
        if (object == null) {
            return null;
        } else if (object instanceof GPathResult) {
            try {
                object = XmlUtil.serialize((GPathResult) object);
            } catch (GroovyRuntimeException e) {
                throw new IllegalArgumentException("Failed to convert XML to Java Object. If you're trying convert to a list then use the getList method instead.", e);
            }
        } else if (object instanceof groovy.util.slurpersupport.Node) {
            object = GroovyNodeSerializer.toXML((groovy.util.slurpersupport.Node) object);
        }

        XmlPathConfig cfg = new XmlPathConfig(getXmlPathConfig());
        if (cfg.hasCustomJaxbObjectMapperFactory()) {
            cfg = cfg.defaultParserType(XmlParserType.JAXB);
        }

        if (!(object instanceof String)) {
            throw new IllegalStateException("Internal error: XML object was not an instance of String, please report to the REST Assured mailing-list.");
        }

        //noinspection RedundantCast
        return (T) XmlObjectDeserializer.deserialize((String) object, objectType, cfg);
    }

    private <T> T getFromPath(String path, boolean convertToJavaObject) {
        final GPathResult input = lazyXmlParser.invoke();
        final XMLAssertion xmlAssertion = new XMLAssertion();
        if (params != null) {
            xmlAssertion.setParams(params);
        }
        final String root = rootPath.equals("") ? rootPath : rootPath.endsWith(".") ? rootPath : rootPath + ".";
        xmlAssertion.setKey(root + path);
        //noinspection unchecked
        return (T) xmlAssertion.getResult(input, convertToJavaObject, true);
    }

    /**
     * Get the result of an XML path expression as an int. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public int getInt(String path) {
        final Object object = get(path);
        return convertObjectTo(object, Integer.class);
    }

    /**
     * Get the result of an XML path expression as a boolean. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public boolean getBoolean(String path) {
        Object object = get(path);
        return convertObjectTo(object, Boolean.class);
    }

    /**
     * Get the result of an XML path expression as a {@link Node}. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public Node getNode(String path) {
        return convertObjectTo(get(path), Node.class);
    }

    /**
     * Get the result of an XML path expression as a {@link NodeChildren}. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public NodeChildren getNodeChildren(String path) {
        return convertObjectTo(get(path), NodeChildren.class);
    }

    /**
     * Get the result of an XML path expression as a char. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public char getChar(String path) {
        Object object = get(path);
        return convertObjectTo(object, Character.class);
    }

    /**
     * Get the result of an XML path expression as a byte. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public byte getByte(String path) {
        Object object = get(path);
        return convertObjectTo(object, Byte.class);
    }

    /**
     * Get the result of an XML path expression as a short. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public short getShort(String path) {
        Object object = get(path);
        return convertObjectTo(object, Short.class);
    }

    /**
     * Get the result of an XML path expression as a float. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public float getFloat(String path) {
        Object object = get(path);
        return convertObjectTo(object, Float.class);
    }

    /**
     * Get the result of an XML path expression as a double. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public double getDouble(String path) {
        Object object = get(path);
        return convertObjectTo(object, Double.class);
    }

    /**
     * Get the result of an XML path expression as a long. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public long getLong(String path) {
        Object object = get(path);
        return convertObjectTo(object, Long.class);
    }

    /**
     * Get the result of an XML path expression as a string. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public String getString(String path) {
        Object object = get(path);
        return convertObjectTo(object, String.class);
    }

    /**
     * Get the result of an XML path expression as a UUID. For syntax details please refer to
     * <a href="http://www.groovy-lang.org/processing-xml.html#_manipulating_xml">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public UUID getUUID(String path) {
        Object object = get(path);
        return convertObjectTo(object, UUID.class);
    }

    /**
     * Add a parameter for the expression. Example:
     * <pre>
     * String type = System.console().readLine();
     * List&lt;Map&gt; books = with(Object).param("type", type).get("shopping.category.findAll { it.@type == type}");
     * </pre>
     *
     * @param key   The name of the parameter. Just use this name in your expression as a variable
     * @param value The value of the parameter
     * @return New XmlPath instance with the parameter set
     */
    public XmlPath param(String key, Object value) {
        XmlPath newP = new XmlPath(this, getXmlPathConfig());
        if (newP.params == null) {
            newP.params = new HashMap<String, Object>();
        }
        newP.params.put(key, value);
        return newP;
    }

    /**
     * Peeks into the XML/HTML that XmlPath will parse by printing it to the console. You can
     * continue working with XmlPath afterwards. This is mainly for debug purposes. If you want to return a prettified version of the content
     * see {@link #prettify()}. If you want to return a prettified version of the content and also print it to the console use {@link #prettyPrint()}.
     * <p/>
     * <p>
     * Note that the content is not guaranteed to be looking exactly like the it does at the source. This is because once you peek
     * the content has been downloaded and transformed into another data structure (used by XmlPath) and the XML is rendered
     * from this data structure.
     * </p>
     *
     * @return The same XmlPath instance
     */
    public XmlPath peek() {
        final GPathResult result = lazyXmlParser.invoke();
        final String render = XmlRenderer.render(result);
        System.out.println(render);
        return this;
    }

    /**
     * Peeks into the XML/HTML that XmlPath will parse by printing it to the console in a prettified manner. You can
     * continue working with XmlPath afterwards. This is mainly for debug purposes. If you want to return a prettified version of the content
     * see {@link #prettify()}. If you want to return a prettified version of the content and also print it to the console use {@link #prettyPrint()}.
     * <p/>
     * <p>
     * Note that the content is not guaranteed to be looking exactly like the it does at the source. This is because once you peek
     * the content has been downloaded and transformed into another data structure (used by XmlPath) and the XML is rendered
     * from this data structure.
     * </p>
     *
     * @return The same XmlPath instance
     */
    public XmlPath prettyPeek() {
        final GPathResult result = lazyXmlParser.invoke();
        final String prettify = XmlPrettifier.prettify(result);
        System.out.println(prettify);
        return this;
    }

    /**
     * Get the XML as a prettified string.
     * <p/>
     * <p>
     * Note that the content is not guaranteed to be looking exactly like the it does at the source. This is because once you peek
     * the content has been downloaded and transformed into another data structure (used by XmlPath) and the XML is rendered
     * from this data structure.
     * </p>
     *
     * @return The XML as a prettified String.
     */
    public String prettify() {
        return XmlPrettifier.prettify(lazyXmlParser.invoke());
    }

    /**
     * Get and print the XML as a prettified string.
     * <p/>
     * <p>
     * Note that the content is not guaranteed to be looking exactly like the it does at the source. This is because once you peek
     * the content has been downloaded and transformed into another data structure (used by XmlPath) and the XML is rendered
     * from this data structure.
     * </p>
     *
     * @return The XML as a prettified String.
     */
    public String prettyPrint() {
        final String pretty = prettify();
        System.out.println(pretty);
        return pretty;
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param text The text containing the XML document
     */
    public static XmlPath given(String text) {
        return new XmlPath(text);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param stream The stream containing the XML document
     */
    public static XmlPath given(InputStream stream) {
        return new XmlPath(stream);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param source The source containing the XML document
     */
    public static XmlPath given(InputSource source) {
        return new XmlPath(source);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param file The file containing the XML document
     */
    public static XmlPath given(File file) {
        return new XmlPath(file);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param reader The reader containing the XML document
     */
    public static XmlPath given(Reader reader) {
        return new XmlPath(reader);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param uri The URI containing the XML document
     */
    public static XmlPath given(URI uri) {
        return new XmlPath(uri);
    }

    public static XmlPath with(InputStream stream) {
        return new XmlPath(stream);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param text The text containing the XML document
     */
    public static XmlPath with(String text) {
        return new XmlPath(text);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param source The source containing the XML document
     */
    public static XmlPath with(InputSource source) {
        return new XmlPath(source);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param file The file containing the XML document
     */
    public static XmlPath with(File file) {
        return new XmlPath(file);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param reader The reader containing the XML document
     */
    public static XmlPath with(Reader reader) {
        return new XmlPath(reader);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param uri The URI containing the XML document
     */
    public static XmlPath with(URI uri) {
        return new XmlPath(uri);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param stream The stream containing the XML document
     */
    public static XmlPath from(InputStream stream) {
        return new XmlPath(stream);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param text The text containing the XML document
     */
    public static XmlPath from(String text) {
        return new XmlPath(text);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param source The source containing the XML document
     */
    public static XmlPath from(InputSource source) {
        return new XmlPath(source);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param file The file containing the XML document
     */
    public static XmlPath from(File file) {
        return new XmlPath(file);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param reader The reader containing the XML document
     */
    public static XmlPath from(Reader reader) {
        return new XmlPath(reader);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param uri The URI containing the XML document
     */
    public static XmlPath from(URI uri) {
        return new XmlPath(uri);
    }

    private LazyXmlParser parseText(final String text) {
        return new LazyXmlParser(getXmlPathConfig(), mode) {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parseText(text);
            }
        };
    }

    /**
     * Set the root path of the document so that you don't need to write the entire path. E.g.
     * <pre>
     * final XmlPath xmlPath = new XmlPath(XML).setRoot("shopping.category.item");
     * assertThat(xmlPath.getInt("size()"), equalTo(5));
     * assertThat(xmlPath.getList("children().list()", String.class), hasItem("Pens"));
     * </pre>
     *
     * @param rootPath The root path to use.
     * @deprecated Use {@link #setRootPath(String)} instead
     */
    @Deprecated
    public XmlPath setRoot(String rootPath) {
        return setRootPath(rootPath);
    }

    /**
     * Set the root path of the document so that you don't need to write the entire path. E.g.
     * <pre>
     * final XmlPath xmlPath = new XmlPath(XML).setRootPath("shopping.category.item");
     * assertThat(xmlPath.getInt("size()"), equalTo(5));
     * assertThat(xmlPath.getList("children().list()", String.class), hasItem("Pens"));
     * </pre>
     *
     * @param rootPath The root path to use.
     */
    public XmlPath setRootPath(String rootPath) {
        AssertParameter.notNull(rootPath, "Root path");
        this.rootPath = rootPath;
        return this;
    }

    private <T> List<T> getAsList(String path) {
        return getAsList(path, null);
    }

    private <T> List<T> getAsList(String path, final Class<?> explicitType) {
        Object returnObject = get(path);
        if (returnObject instanceof NodeChildren) {
            final NodeChildren nodeChildren = (NodeChildren) returnObject;
            returnObject = convertElementsListTo(nodeChildren.list(), explicitType);
        } else if (!(returnObject instanceof List)) {
            final List<T> asList = new ArrayList<T>();
            if (returnObject != null) {
                final T e;
                if (explicitType == null) {
                    e = (T) returnObject.toString();
                } else {
                    e = (T) convertObjectTo(returnObject, explicitType);
                }
                asList.add(e);
            }
            returnObject = asList;
        } else if (explicitType != null) {
            final List<?> returnObjectAsList = (List<?>) returnObject;
            final List<T> convertedList = new ArrayList<T>();
            for (Object o : returnObjectAsList) {
                convertedList.add((T) convertObjectTo(o, explicitType));
            }
            returnObject = convertedList;
        }
        return returnObject == null ? null : Collections.unmodifiableList((List<T>) returnObject);
    }

    private List<Object> convertElementsListTo(List<Node> list, Class<?> explicitType) {
        List<Object> convertedList = new ArrayList<Object>();
        if (list != null && list.size() > 0) {
            for (Node node : list) {
                if (explicitType == null) {
                    convertedList.add(node.toString());
                } else {
                    convertedList.add(convertObjectTo(node, explicitType));
                }
            }
        }
        return convertedList;
    }

    private <T> T convertObjectTo(Object object, Class<T> explicitType) {
        if (object instanceof NodeBase && !ObjectConverter.canConvert(object, explicitType)) {
            return getObjectAsType(((NodeBase) object).getBackingGroovyObject(), explicitType);
        }
        return ObjectConverter.convertObjectTo(object, explicitType);
    }

    private LazyXmlParser parseInputStream(final InputStream stream) {
        return new LazyXmlParser(getXmlPathConfig(), mode) {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parse(stream);
            }
        };
    }

    private LazyXmlParser parseReader(final Reader reader) {
        return new LazyXmlParser(getXmlPathConfig(), mode) {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parse(reader);
            }
        };
    }

    private LazyXmlParser parseFile(final File file) {
        return new LazyXmlParser(getXmlPathConfig(), mode) {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parse(file);
            }
        };
    }

    private LazyXmlParser parseURI(final URI uri) {
        return new LazyXmlParser(getXmlPathConfig(), mode) {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parse(uri.toString());
            }
        };
    }

    private LazyXmlParser parseInputSource(final InputSource source) {
        return new LazyXmlParser(getXmlPathConfig(), mode) {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parse(source);
            }
        };
    }

    private XmlPathConfig getXmlPathConfig() {
        XmlPathConfig cfg;
        if (config == null && xmlPathConfig == null) {
            cfg = new XmlPathConfig();
        } else if (xmlPathConfig != null) {
            cfg = xmlPathConfig;
        } else {
            cfg = config;
        }
        return cfg;
    }

    private static abstract class LazyXmlParser {
        protected abstract GPathResult method(XmlSlurper slurper) throws Exception;

        private volatile XmlPathConfig config;
        private volatile CompatibilityMode compatibilityMode;

        protected LazyXmlParser(XmlPathConfig config, CompatibilityMode compatibilityMode) {
            this.config = config;
            this.compatibilityMode = compatibilityMode;
        }

        public LazyXmlParser changeCompatibilityMode(CompatibilityMode mode) {
            this.compatibilityMode = mode;
            return this;
        }

        public LazyXmlParser changeConfig(XmlPathConfig config) {
            this.config = config;
            return this;
        }

        private GPathResult input;
        private boolean isInputParsed;

        public synchronized GPathResult invoke() {
            if (isInputParsed) {
                return input;
            }
            isInputParsed = true;
            try {
                final XmlSlurper slurper;
                if (compatibilityMode == XML) {
                    slurper = new XmlSlurper(config.isValidating(), config.isNamespaceAware(), config.isAllowDocTypeDeclaration());
                } else {
                    XMLReader p = new org.ccil.cowan.tagsoup.Parser();
                    slurper = new XmlSlurper(p);
                }

                // Apply features
                Map<String, Boolean> features = config.features();
                for (Entry<String, Boolean> feature : features.entrySet()) {
                    slurper.setFeature(feature.getKey(), feature.getValue());
                }

                // Apply properties
                Map<String, Object> properties = config.properties();
                for (Entry<String, Object> feature : properties.entrySet()) {
                    slurper.setProperty(feature.getKey(), feature.getValue());
                }

                final GPathResult result = method(slurper);
                if (config.hasDeclaredNamespaces()) {
                    result.declareNamespace(config.declaredNamespaces());
                }
                input = result;
                return input;
            } catch (Exception e) {
                throw new XmlPathException("Failed to parse the XML document", e);
            }
        }
    }

    public static enum CompatibilityMode {
        XML, HTML
    }

    /**
     * Resets static XmlPath configuration to default values
     */
    public static void reset() {
        XmlPath.config = null;
    }
}
