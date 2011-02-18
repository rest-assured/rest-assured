/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.path.xml;

import com.jayway.restassured.assertion.XMLAssertion;
import com.jayway.restassured.exception.ParsePathException;
import groovy.util.XmlSlurper;
import groovy.util.slurpersupport.GPathResult;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.assertion.AssertParameter.notNull;

/**
 * XmlPath is an alternative to using XPath for easily getting values from an XML document. It follows the Groovy syntax
 * described <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">here</a>. <br>Let's say we have an XML defined as;
 * <pre>
 * &lt;shopping&gt;
       &lt;category type=&quot;groceries&quot;&gt;
           &lt;item&gt;
              &lt;name&gt;Chocolate&lt;/name&gt;
              &lt;price&gt;10&lt;/price&gt;
           &lt;/item&gt;
           &lt;item&gt;
              &lt;name&gt;Coffee&lt;/name&gt;
              &lt;price&gt;20&lt;/price&gt;
           &lt;/item&gt;
       &lt;/category&gt;
       &lt;category type=&quot;supplies&quot;&gt;
           &lt;item&gt;
               &lt;name&gt;Paper&lt;/name&gt;
               &lt;price&gt;5&lt;/price&gt;
           &lt;/item&gt;
           &lt;item quantity=&quot;4&quot;&gt;
               &lt;name&gt;Pens&lt;/name&gt;
               &lt;price&gt;15&lt;/price&gt;
           &lt;/item&gt;
       &lt;/category&gt;
       &lt;category type=&quot;present&quot;&gt;
           &lt;item when=&quot;Aug 10&quot;&gt;
               &lt;name&gt;Kathryn&#39;s Birthday&lt;/name&gt;
               &lt;price&gt;200&lt;/price&gt;
           &lt;/item&gt;
     &lt;/category&gt;
 &lt;/shopping&gt;
 * </pre>
 *
 * Get the name of the first category item:
 * <pre>
 *     String name = with(XML).get("shopping.category.item[0].name");
 * </pre>
 *
 * To get the number of category items:
 * <pre>
 *     int items = with(XML).get("shopping.category.item.size()");
 * </pre>
 *
 * Get a specific category:
 * <pre>
 *     Node category = with(XML).get("shopping.category[0]");
 * </pre>
 *
 * To get the number of categories with type attribute equal to 'groceries':
 * <pre>
 *    int items = with(XML).get("shopping.category.findAll { it.@type == 'groceries' }.size()");
 * </pre>
 *
 * Get all items with price greater than or equal to 10 and less than or equal to 20:
 * <pre>
 * List<Node> itemsBetweenTenAndTwenty = with(XML).get("shopping.category.item.findAll { item -> def price = item.price.toFloat(); price >= 10 && price <= 20 }");
 * </pre>
 *
 *
 */
public class XmlPath {

    private final GPathResult input;

    private String rootPath = "";

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param text The text containing the XML document
     */
    public XmlPath(String text) {
        input = parseText(text);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param stream The stream containing the XML document
     */
    public XmlPath(InputStream stream) {
        input = parseInputStream(stream);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param source The source containing the XML document
     */
    public XmlPath(InputSource source) {
        input = parseInputSource(source);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param file The file containing the XML document
     */
    public XmlPath(File file) {
        input = parseFile(file);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param reader The reader containing the XML document
     */
    public XmlPath(Reader reader) {
        input = parseReader(reader);
    }

    /**
     * Instantiate a new XmlPath instance.
     *
     * @param uri The URI containing the XML document
     */
    public XmlPath(URI uri) {
        input = parseURI(uri);
    }

    /**
     * Get the result of an XML path expression. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @param <T> The type of the return value.
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> T get(String path) {
        notNull(path, "path");
        final XMLAssertion xmlAssertion = new XMLAssertion();
        final String root = rootPath.equals("") ? rootPath : rootPath.endsWith(".") ? rootPath : rootPath + ".";
        xmlAssertion.setKey(root + path);
        return (T) xmlAssertion.getResult(input);
    }

    /**
     * Get the result of an XML path expression as a list. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @param <T> The list type
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> List<T> getList(String path) {
        return get(path);
    }

    /**
     * Get the result of an XML path expression as a list. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @param genericType The generic list type
     * @param <T> The type
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> List<T> getList(String path, Class<T> genericType) {
        return get(path);
    }

    /**
     * Get the result of an XML path expression as a map. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @param <K> The type of the expected key
     * @param <V> The type of the expected value
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <K,V> Map<K, V> getMap(String path) {
        return get(path);
    }

    /**
     * Get the result of an XML path expression as a map. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @param keyType The type of the expected key
     * @param valueType The type of the expected value
     * @param <K> The type of the expected key
     * @param <V> The type of the expected value
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <K,V> Map<K, V> getMap(String path, Class<K> keyType, Class<V> valueType) {
        return get(path);
    }

    /**
     * Get the result of an XML path expression as an int. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public int getInt(String path) {
        return (Integer) get(path);
    }

    /**
     * Get the result of an XML path expression as a boolean. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public boolean getBoolean(String path) {
        return (Boolean) get(path);
    }

    /**
     * Get the result of an XML path expression as a char. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public char getChar(String path) {
        return (Character) get(path);
    }

    /**
     * Get the result of an XML path expression as a byte. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public byte getByte(String path) {
        return (Byte) get(path);
    }

    /**
     * Get the result of an XML path expression as a short. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public short getShort(String path) {
        return (Short) get(path);
    }

    /**
     * Get the result of an XML path expression as a float. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public float getFloat(String path) {
        return (Float) get(path);
    }

    /**
     * Get the result of an XML path expression as a double. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public double getDouble(String path) {
        return (Double) get(path);
    }

    /**
     * Get the result of an XML path expression as a long. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public long getLong(String path) {
        return (Long) get(path);
    }

    /**
     * Get the result of an XML path expression as a string. For syntax details please refer to
     * <a href="http://groovy.codehaus.org/Updating+XML+with+XmlSlurper">this</a> url.
     *
     * @param path The XML path.
     * @return The object matching the XML path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public String getString(String path) {
        return get(path);
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

    private GPathResult parseText(final String text)  {
        return new ExceptionCatcher() {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parseText(text);
            }
        }.invoke();
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
     */
    public XmlPath setRoot(String rootPath) {
        notNull(rootPath, "Root path");
        this.rootPath = rootPath;
        return this;
    }

    private GPathResult parseInputStream(final InputStream stream)  {
        return new ExceptionCatcher() {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parse(stream);
            }
        }.invoke();
    }

    private GPathResult parseReader(final Reader reader)  {
        return new ExceptionCatcher() {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parse(reader);
            }
        }.invoke();
    }

    private GPathResult parseFile(final File file)  {
        return new ExceptionCatcher() {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parse(file);
            }
        }.invoke();
    }

    private GPathResult parseURI(final URI uri)  {
        return new ExceptionCatcher() {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parse(uri.toString());
            }
        }.invoke();
    }

    private GPathResult parseInputSource(final InputSource source)  {
        return new ExceptionCatcher() {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parse(source);
            }
        }.invoke();
    }

    private abstract class ExceptionCatcher {

        protected abstract GPathResult method(XmlSlurper slurper) throws Exception;

        public GPathResult invoke() {
            try {
                return method(new XmlSlurper());
            } catch(Exception e) {
                throw new ParsePathException("Failed to parse the XML document", e);
            }
        }
    }
}