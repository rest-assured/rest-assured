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

package com.jayway.restassured.path;

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

import static com.jayway.restassured.assertion.AssertParameter.notNull;

/**
 * XmlPath is an alternative to using XPath for easily getting values in an XML file. It follows the Groovy syntax
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
 * </pre>*
 *
 * To get the number of category items:
 * <pre>
 *     int items = with(XML).get("shopping.category.item.size()");
 * </pre>
 *
 *
 */
public class XmlPath {

    private final GPathResult input;

    private String rootPath = "";

    public XmlPath(String text) {
        input = parseText(text);
    }

    public XmlPath(InputStream stream) {
        input = parseInputStream(stream);
    }

    public XmlPath(InputSource source) {
        input = parseInputSource(source);
    }

    public XmlPath(File file) {
        input = parseFile(file);
    }

    public XmlPath(Reader reader) {
        input = parseReader(reader);
    }

    public XmlPath(URI uri) {
        input = parseURI(uri);
    }

    public <T> T get(String path) {
        notNull(path, "path");
        final XMLAssertion xmlAssertion = new XMLAssertion();
        final String root = rootPath.equals("") ? rootPath : rootPath.endsWith(".") ? rootPath : rootPath + ".";
        xmlAssertion.setKey(root + path);
        return (T) xmlAssertion.getResult(input);
    }

    public <T> List<T> getList(String path) {
        return get(path);
    }

    public <T> List<T> getList(String path, Class<T> genericType) {
        return get(path);
    }

    public int getInt(String path) {
        return (Integer) get(path);
    }

    public float getFloat(String path) {
        return (Float) get(path);
    }

    public double getDouble(String path) {
        return (Double) get(path);
    }

    public long getLong(String path) {
        return (Long) get(path);
    }

    public String getString(String path) {
        return get(path);
    }

    private GPathResult parseText(final String text)  {
        return new ExceptionCatcher() {
            protected GPathResult method(XmlSlurper slurper) throws Exception {
                return slurper.parseText(text);
            }
        }.invoke();
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
                throw new ParsePathException("Failed to parse the xml", e);
            }
        }
    }

    public static XmlPath given(String text) {
        return new XmlPath(text);
    }

    public static XmlPath given(InputStream stream) {
        return new XmlPath(stream);
    }

    public static XmlPath given(InputSource source) {
        return new XmlPath(source);
    }

    public static XmlPath given(File file) {
        return new XmlPath(file);
    }

    public static XmlPath given(Reader reader) {
        return new XmlPath(reader);
    }

    public static XmlPath given(URI uri) {
        return new XmlPath(uri);
    }

    public static XmlPath with(String text) {
        return new XmlPath(text);
    }

    public static XmlPath with(InputStream stream) {
        return new XmlPath(stream);
    }

    public static XmlPath with(InputSource source) {
        return new XmlPath(source);
    }

    public static XmlPath with(File file) {
        return new XmlPath(file);
    }

    public static XmlPath with(Reader reader) {
        return new XmlPath(reader);
    }

    public static XmlPath with(URI uri) {
        return new XmlPath(uri);
    }

    public XmlPath setRoot(String rootPath) {
        notNull(rootPath, "Root path");
        this.rootPath = rootPath;
        return this;
    }
}