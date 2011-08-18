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

package com.jayway.restassured.path.json;

import com.jayway.restassured.assertion.JSONAssertion;
import com.jayway.restassured.exception.ParsePathException;
import net.sf.json.JSON;
import net.sf.json.groovy.JsonSlurper;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.assertion.AssertParameter.notNull;

/**
 * JsonPath is an alternative to using XPath for easily getting values from a JSON document. It follows the
 * Groovy dot notation syntax when getting an object from the document. You can regard it as an alternative to XPath for XML.
 * E.g. given the following JSON document:
 * <pre>
 * { "store": {
 *   "book": [
 *    { "category": "reference",
 *      "author": "Nigel Rees",
 *      "title": "Sayings of the Century",
 *      "price": 8.95
 *    },
 *    { "category": "fiction",
 *      "author": "Evelyn Waugh",
 *      "title": "Sword of Honour",
 *      "price": 12.99
 *    },
 *    { "category": "fiction",
 *      "author": "Herman Melville",
 *      "title": "Moby Dick",
 *      "isbn": "0-553-21311-3",
 *      "price": 8.99
 *    },
 *    { "category": "fiction",
 *      "author": "J. R. R. Tolkien",
 *      "title": "The Lord of the Rings",
 *      "isbn": "0-395-19395-8",
 *      "price": 22.99
 *    }
 *  ],
 *    "bicycle": {
 *      "color": "red",
 *      "price": 19.95
 *    }
 *  }
 * }
 * </pre>
 * To get a list of all book categories:
 * <pre>
 * List&lt;String&gt; categories = with(JSON).get("store.book.category");
 * </pre>
 *
 * Get the first book category:
 * <pre>
 * String category = with(JSON).get("store.book[0].category");
 * </pre>
 *
 * Get the last book category:
 * <pre>
 * String category = with(JSON).get("store.book[-1].category");
 * </pre>
 *
 * Get all books with price between 5 and 15:
 * <pre>
 * List&lt;Map&gt; books = with(JSON).get("store.book.findAll { book -> book.price >= 5 && book.price <= 15 }");
 * </pre>
 *
 */
public class JsonPath {

    private final JSON json;
    private String rootPath = "";

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param text The text containing the JSON document
     */
    public JsonPath(String text) {
        json = new JsonSlurper().parseText(text);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param url The url containing the JSON document
     */
    public JsonPath(URL url) {
        json = parseURL(url);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param stream The stream containing the JSON document
     */
    public JsonPath(InputStream stream) {
        json = parseInputStream(stream);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param file The file containing the JSON document
     */
    public JsonPath(File file) {
        json = parseFile(file);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param reader The reader containing the JSON document
     */
    public JsonPath(Reader reader) {
        json = parseReader(reader);
    }

    /**
     * Get a JSON graph with no named root element as a Java object. This is just a short-cut for
     *
     * <pre>
     *     get("");
     * </pre>
     * or
     * <pre>
     *     get("$");
     * </pre>
     *
     * @return The object matching the JSON graph. This may be any primitive type, a List or a Map.  A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> T get() {
        return get("");
    }

    /**
     * Get the result of an JSON path expression as a boolean.
     *
     * @param path The JSON path.
     * @return The object matching the JSON path. This may be any primitive type, a List or a Map.  A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> T get(String path) {
        notNull(path, "path");
        final JSONAssertion jsonAssertion = new JSONAssertion();
        final String root = rootPath.equals("") ? rootPath : rootPath.endsWith(".") ? rootPath : rootPath + ".";
        jsonAssertion.setKey(root + path);
        return (T) jsonAssertion.getResult(json);
    }

    /**
     * Get the result of an JSON path expression as a boolean
     *
     * @param path The JSON path.
     * @return The object matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public boolean getBoolean(String path) {
        return (Boolean) get(path);
    }

    /**
     * Get the result of an JSON path expression as a char.
     *
     * @param path The JSON path.
     * @return The object matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public char getChar(String path) {
        return (Character) get(path);
    }

    /**
     * Get the result of an JSON path expression as an int.
     *
     * @param path The JSON path.
     * @return The int matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public int getInt(String path) {
        //The type returned from Groovy depends on the input, so we need to handle different numerical types.
        Object value = get(path);
        if (value instanceof Short) {
            return ((Short)value).intValue();
        } else if (value instanceof Long) {
            return ((Long)value).intValue();
        } else {
            return (Integer) value;
        }
    }

    /**
     * Get the result of an JSON path expression as a byte. 
     *
     *
     * @param path The JSON path.
     * @return The object matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public byte getByte(String path) {
        //The type returned from Groovy depends on the input, so we need to handle different numerical types.
        Object value = get(path);
        if (value instanceof Long) {
            return ((Long)value).byteValue();
        } else if (value instanceof Integer) {
            return ((Integer)value).byteValue();
        } else {
            return (Byte) value;
        }
    }

    /**
     * Get the result of an JSON path expression as a short. 
     *
     *
     * @param path The JSON path.
     * @return The object matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public short getShort(String path) {
        //The type returned from Groovy depends on the input, so we need to handle different numerical types.
        Object value = get(path);
        if (value instanceof Long) {
            return ((Long)value).shortValue();
        } else if (value instanceof Integer) {
            return ((Integer)value).shortValue();
        } else {
            return (Short) value;
        }
    }

    /**
     * Get the result of an JSON path expression as a float. 
     *
     * @param path The JSON path.
     * @return The object matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public float getFloat(String path) {
        //Groovy will always return a Double for floating point values.
        return ((Double)get(path)).floatValue();
    }

    /**
     * Get the result of an JSON path expression as a double. 
     *
     * @param path The JSON path.
     * @return The object matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public double getDouble(String path) {
        return (Double) get(path);
    }

    /**
     * Get the result of an JSON path expression as a long. 
     *
     * @param path The JSON path.
     * @return The object matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public long getLong(String path) {
        //The type returned from Groovy depends on the input, so we need to handle different numerical types.
        Object value = get(path);
        if (value instanceof Short) {
            return ((Short)value).longValue();
        } else if (value instanceof Integer) {
            return ((Integer)value).longValue();
        } else {
            return (Long) value;
        }
    }

    /**
     * Get the result of an JSON path expression as a string. 
     *
     * @param path The JSON path.
     * @return The object matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public String getString(String path) {
        return get(path);
    }
    
    /**
     * Get the result of an JSON path expression as a list. 
     *
     * @param path The JSON path.
     * @param <T> The list type
     * @return The object matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> List<T> getList(String path) {
        return get(path);
    }

    /**
     * Get the result of an JSON path expression as a list.
     *
     * @param path The JSON path.
     * @param genericType The generic list type
     * @param <T> The type
     * @return The object matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> List<T> getList(String path, Class<T> genericType) {
        return get(path);
    }

    /**
     * Get the result of an JSON path expression as a map.
     *
     * @param path The JSON path.
     * @param <K> The type of the expected key
     * @param <V> The type of the expected value
     * @return The map matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <K,V> Map<K, V> getMap(String path) {
        return get(path);
    }

    /**
     * Get the result of an JSON path expression as a map.
     *
     * @param path The JSON path.
     * @param keyType The type of the expected key
     * @param valueType The type of the expected value
     * @param <K> The type of the expected key
     * @param <V> The type of the expected value
     * @return The map matching the JSON path. A {@java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <K,V> Map<K, V> getMap(String path, Class<K> keyType, Class<V> valueType) {
        return get(path);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param text The text containing the JSON document
     */
    public static JsonPath given(String text) {
        return new JsonPath(text);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param stream The stream containing the JSON document
     */
    public static JsonPath given(InputStream stream) {
        return new JsonPath(stream);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param file The file containing the JSON document
     */
    public static JsonPath given(File file) {
        return new JsonPath(file);
    }
    /**
     * Instantiate a new JsonPath instance.
     *
     * @param reader The reader containing the JSON document
     */
    public static JsonPath given(Reader reader) {
        return new JsonPath(reader);
    }
    /**
     * Instantiate a new JsonPath instance.
     *
     * @param url The URL containing the JSON document
     */
    public static JsonPath given(URL url) {
        return new JsonPath(url);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param stream The stream containing the JSON document
     */
    public static JsonPath with(InputStream stream) {
        return new JsonPath(stream);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param text The text containing the JSON document
     */
    public static JsonPath with(String text) {
        return new JsonPath(text);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param file The file containing the JSON document
     */
    public static JsonPath with(File file) {
        return new JsonPath(file);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param reader The reader containing the JSON document
     */
    public static JsonPath with(Reader reader) {
        return new JsonPath(reader);
    }
    /**
     * Instantiate a new JsonPath instance.
     *
     * @param url The URI containing the JSON document
     */
    public static JsonPath with(URL url) {
        return new JsonPath(url);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param stream The stream containing the JSON document
     */
    public static JsonPath from(InputStream stream) {
        return new JsonPath(stream);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param text The text containing the JSON document
     */
    public static JsonPath from(String text) {
        return new JsonPath(text);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param file The file containing the JSON document
     */
    public static JsonPath from(File file) {
        return new JsonPath(file);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param reader The reader containing the JSON document
     */
    public static JsonPath from(Reader reader) {
        return new JsonPath(reader);
    }
    /**
     * Instantiate a new JsonPath instance.
     *
     * @param url The URI containing the JSON document
     */
    public static JsonPath from(URL url) {
        return new JsonPath(url);
    }

    /**
     * Set the root path of the document so that you don't need to write the entire path. E.g.
     * <pre>
     * final JsonPath jsonPath = new JsonPath(JSON).setRoot("store.book");
     * assertThat(jsonPath.getInt("size()"), equalTo(4));
     * assertThat(jsonPath.getList("author", String.class), hasItem("J. R. R. Tolkien"));
     * </pre>
     *
     * @param rootPath The root path to use.
     */
    public JsonPath setRoot(String rootPath) {
        notNull(rootPath, "Root path");
        this.rootPath = rootPath;
        return this;
    }

    private JSON parseInputStream(final InputStream stream)  {
        return new ExceptionCatcher() {
            protected JSON method(JsonSlurper slurper) throws Exception {
                return slurper.parse(stream);
            }
        }.invoke();
    }

    private JSON parseReader(final Reader reader)  {
        return new ExceptionCatcher() {
            protected JSON method(JsonSlurper slurper) throws Exception {
                return slurper.parse(reader);
            }
        }.invoke();
    }

    private JSON parseFile(final File file)  {
        return new ExceptionCatcher() {
            protected JSON method(JsonSlurper slurper) throws Exception {
                return slurper.parse(file);
            }
        }.invoke();
    }

    private JSON parseURL(final URL url)  {
        return new ExceptionCatcher() {
            protected JSON method(JsonSlurper slurper) throws Exception {
                return slurper.parse(url.toString());
            }
        }.invoke();
    }

    private abstract class ExceptionCatcher {

        protected abstract JSON method(JsonSlurper slurper) throws Exception;

        public JSON invoke() {
            try {
                return method(new JsonSlurper());
            } catch(Exception e) {
                throw new ParsePathException("Failed to parse the JSON document", e);
            }
        }
    }

}
