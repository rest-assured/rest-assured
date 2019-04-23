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

package io.restassured.path.json;

import groovy.json.JsonBuilder;
import groovy.json.JsonOutput;
import io.restassured.common.mapper.TypeRef;
import io.restassured.internal.common.assertion.AssertParameter;
import io.restassured.internal.common.path.ObjectConverter;
import io.restassured.internal.path.json.ConfigurableJsonSlurper;
import io.restassured.internal.path.json.JSONAssertion;
import io.restassured.internal.path.json.JsonPrettifier;
import io.restassured.internal.path.json.mapping.JsonObjectDeserializer;
import io.restassured.path.json.config.JsonParserType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.exception.JsonPathException;
import io.restassured.path.json.mapper.factory.GsonObjectMapperFactory;
import io.restassured.path.json.mapper.factory.Jackson1ObjectMapperFactory;
import io.restassured.path.json.mapper.factory.Jackson2ObjectMapperFactory;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 * JsonPath is an alternative to using XPath for easily getting values from a Object document. It follows the
 * Groovy <a href="http://docs.groovy-lang.org/latest/html/documentation/#_gpath">GPath</a> syntax when getting an object from the document. You can regard it as an alternative to XPath for JSON.
 * E.g. given the following Object document:
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
 * List&lt;String&gt; categories = with(Object).get("store.book.category");
 * </pre>
 * <p/>
 * Get the first book category:
 * <pre>
 * String category = with(Object).get("store.book[0].category");
 * </pre>
 * <p/>
 * Get the last book category:
 * <pre>
 * String category = with(Object).get("store.book[-1].category");
 * </pre>
 * <p/>
 * Get all books with price between 5 and 15:
 * <pre>
 * List&lt;Map&gt; books = with(Object).get("store.book.findAll { book -> book.price >= 5 && book.price <= 15 }");
 * </pre>
 * <p/>
 * The JsonPath implementation of rest-assured uses a Groovy shell to evaluate expressions so be careful when injecting
 * user input into the expression. For example avoid doing this:
 * <pre>
 * String name = System.console().readLine();
 * List&lt;Map&gt; books = with(Object).get("store.book.findAll { book -> book.author == " + name + " }");
 * </pre>
 * Instead use the {@link #param(java.lang.String, java.lang.Object)} method like this:
 * <pre>
 * String name = System.console().readLine();
 * List&lt;Map&gt; books = with(Object).param("name", name).get("store.book.findAll { book -> book.author == name }");
 * </pre>
 */
public class JsonPath {

    public static JsonPathConfig config = null;

    private final JsonParser jsonParser;
    private JsonPathConfig jsonPathConfig = null;
    private String rootPath = "";
    /**
     * Parameters for groovy console (not initialized here to save memory for queries that don't use params)
     */
    private Map<String, Object> params;

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param text The text containing the Object document
     */
    public JsonPath(String text) {
        jsonParser = parseText(text);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param url The url containing the Object document
     */
    public JsonPath(URL url) {
        jsonParser = parseURL(url);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param stream The stream containing the Object document
     */
    public JsonPath(InputStream stream) {
        jsonParser = parseInputStream(stream);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param file The file containing the Object document
     */
    public JsonPath(File file) {
        jsonParser = parseFile(file);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param reader The reader containing the Object document
     */
    public JsonPath(Reader reader) {
        jsonParser = parseReader(reader);
    }

    private JsonPath(JsonPath jsonPath, JsonPathConfig jsonPathConfig) {
        this.jsonPathConfig = jsonPathConfig;
        this.jsonParser = jsonPath.jsonParser;
        this.rootPath = jsonPath.rootPath;
        if (jsonPath.params != null) {
            this.params = new HashMap<String, Object>(jsonPath.params);
        }
    }

    /**
     * Get a Object graph with no named root element as a Java object. This is just a short-cut for
     * <p/>
     * <pre>
     *     get("");
     * </pre>
     * or
     * <pre>
     *     get("$");
     * </pre>
     *
     * @return The object matching the Object graph. This may be any primitive type, a List or a Map.  A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> T get() {
        return get("");
    }

    /**
     * Get the result of an Object path expression as a boolean.
     *
     * @param path The Object path.
     * @return The object matching the Object path. This may be any primitive type, a List or a Map.  A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> T get(String path) {
        final JSONAssertion jsonAssertion = createJsonAssertion(path, params);
        final Object json = jsonParser.parseWith(createConfigurableJsonSlurper());
        return (T) jsonAssertion.getResult(json, null);
    }

    /**
     * Get the result of an Object path expression as a boolean
     *
     * @param path The Object path.
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public boolean getBoolean(String path) {
        return ObjectConverter.convertObjectTo(get(path), Boolean.class);
    }

    /**
     * Get the result of an Object path expression as a char.
     *
     * @param path The Object path.
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public char getChar(String path) {
        return ObjectConverter.convertObjectTo(get(path), Character.class);
    }

    /**
     * Get the result of an Object path expression as an int.
     *
     * @param path The Object path.
     * @return The int matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public int getInt(String path) {
        //The type returned from Groovy depends on the input, so we need to handle different numerical types.
        Object value = get(path);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Short) {
            return ((Short) value).intValue();
        } else if (value instanceof Long) {
            return ((Long) value).intValue();
        } else {
            return ObjectConverter.convertObjectTo(value, Integer.class);
        }
    }

    /**
     * Get the result of an Object path expression as a byte.
     *
     * @param path The Object path.
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public byte getByte(String path) {
        //The type returned from Groovy depends on the input, so we need to handle different numerical types.
        Object value = get(path);
        if (value instanceof Byte) {
            return (Byte) value;
        } else if (value instanceof Long) {
            return ((Long) value).byteValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).byteValue();
        } else {
            return ObjectConverter.convertObjectTo(value, Byte.class);
        }
    }

    /**
     * Get the result of an Object path expression as a short.
     *
     * @param path The Object path.
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public short getShort(String path) {
        //The type returned from Groovy depends on the input, so we need to handle different numerical types.
        Object value = get(path);
        if (value instanceof Short) {
            return (Short) value;
        } else if (value instanceof Long) {
            return ((Long) value).shortValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).shortValue();
        } else {
            return ObjectConverter.convertObjectTo(value, Short.class);
        }
    }

    /**
     * Get the result of an Object path expression as a float.
     *
     * @param path The Object path.
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public float getFloat(String path) {
        final Object value = get(path);
        //Groovy will always return a Double for floating point values.
        if (value instanceof Double) {
            return ((Double) value).floatValue();
        } else {
            return ObjectConverter.convertObjectTo(value, Float.class);
        }
    }

    /**
     * Get the result of an Object path expression as a double.
     *
     * @param path The Object path.
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public double getDouble(String path) {
        final Object value = get(path);
        if (value instanceof Double) {
            return (Double) value;
        }
        return ObjectConverter.convertObjectTo(value, Double.class);
    }

    /**
     * Get the result of an Object path expression as a long.
     *
     * @param path The Object path.
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public long getLong(String path) {
        //The type returned from Groovy depends on the input, so we need to handle different numerical types.
        Object value = get(path);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Short) {
            return ((Short) value).longValue();
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue();
        } else {
            return ObjectConverter.convertObjectTo(value, Long.class);
        }
    }

    /**
     * Get the result of an Object path expression as a string.
     *
     * @param path The Object path.
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public String getString(String path) {
        return ObjectConverter.convertObjectTo(get(path), String.class);
    }

    /**
     * Get the result of an Object path expression as a UUID.
     *
     * @param path The Object path.
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public UUID getUUID(String path) {
        return ObjectConverter.convertObjectTo(get(path), UUID.class);
    }

    /**
     * Get the result of an Object path expression as a list.
     *
     * @param path The Object path.
     * @param <T>  The list type
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> List<T> getList(String path) {
        return get(path);
    }

    /**
     * Get the result of an Object path expression as a list.
     *
     * @param path        The Object path.
     * @param genericType The generic list type
     * @param <T>         The type
     * @return The object matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <T> List<T> getList(String path, Class<T> genericType) {
        if (genericType == null) {
            throw new IllegalArgumentException("Generic type cannot be null");
        }
        final List<T> original = get(path);
        final List<T> newList = new LinkedList<T>();
        if (original != null) {
            for (T t : original) {
                T e;
                if (t instanceof Map && !genericType.isAssignableFrom(Map.class)) {
                    // TODO Avoid double parsing
                    String str = objectToString(t);
                    //noinspection unchecked
                    e = (T) jsonStringToObject(str, genericType);
                } else {
                    e = ObjectConverter.convertObjectTo(t, genericType);
                }
                newList.add(e);
            }
        }
        return Collections.unmodifiableList(newList);
    }

    /**
     * Get the result of an Object path expression as a map.
     *
     * @param path The Object path.
     * @param <K>  The type of the expected key
     * @param <V>  The type of the expected value
     * @return The map matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <K, V> Map<K, V> getMap(String path) {
        return get(path);
    }

    /**
     * Get the result of an Object path expression as a map.
     *
     * @param path      The Object path.
     * @param keyType   The type of the expected key
     * @param valueType The type of the expected value
     * @param <K>       The type of the expected key
     * @param <V>       The type of the expected value
     * @return The map matching the Object path. A {@link java.lang.ClassCastException} will be thrown if the object
     * cannot be casted to the expected type.
     */
    public <K, V> Map<K, V> getMap(String path, Class<K> keyType, Class<V> valueType) {
        final Map<K, V> originalMap = get(path);
        final Map<K, V> newMap = new HashMap<K, V>();
        for (Entry<K, V> entry : originalMap.entrySet()) {
            final K key = entry.getKey() == null ? null : ObjectConverter.convertObjectTo(entry.getKey(), keyType);
            final V value = entry.getValue() == null ? null : ObjectConverter.convertObjectTo(entry.getValue(), valueType);
            newMap.put(key, value);
        }
        return Collections.unmodifiableMap(newMap);
    }

    /**
     * Get the result of a Object path expression as a java Object.
     * E.g. given the following Object document:
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
     * And a Java object like this:
     * <p/>
     * <pre>
     * public class Book {
     *      private String category;
     *      private String author;
     *      private String title;
     *      private String isbn;
     *      private float price;
     *
     *      public String getCategory() {
     *         return category;
     *      }
     *
     *     public void setCategory(String category) {
     *         this.category = category;
     *     }
     *
     *    public String getAuthor() {
     *          return author;
     *     }
     *
     *    public void setAuthor(String author) {
     *         this.author = author;
     *    }
     *
     *    public String getTitle() {
     *         return title;
     *    }
     *
     *    public void setTitle(String title) {
     *        this.title = title;
     *    }
     *
     *    public String getIsbn() {
     *             return isbn;
     *    }
     *
     *    public void setIsbn(String isbn) {
     *          this.isbn = isbn;
     *    }
     *
     *    public float getPrice() {
     *        return price;
     *    }
     *
     *    public void setPrice(float price) {
     *             this.price = price;
     *   }
     * }
     * </pre>
     * <p/>
     * Then
     * <pre>
     * Book book = from(Object).getObject("store.book[2]", Book.class);
     * </pre>
     * <p/>
     * maps the second book to a Book instance.
     *
     * @param path       The path to the object to map
     * @param objectType The class type of the expected object
     * @param <T>        The type of the expected object
     * @return The object
     */
    public <T> T getObject(String path, Class<T> objectType) {
        Object object = getJsonObject(path);
        if (object == null) {
            return null;
        } else if (object instanceof List || object instanceof Map) {
            // TODO Avoid double parsing
            object = objectToString(object);
        } else {
            return ObjectConverter.convertObjectTo(object, objectType);
        }

        if (!(object instanceof String)) {
            throw new IllegalStateException("Internal error: Json object was not an instance of String, please report to the REST Assured mailing-list.");
        }

        return (T) jsonStringToObject((String) object, objectType);
    }

    /**
     * Get the result of a Object path expression as a java Object with generic type.
     * E.g. given the following Object document:
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
     * And you want to get a book as a <code>Map&lt;String, Object&gt;</code>:
     * <p/>
     * Then
     * <pre>
     * Map&lt;String, Object&gt; book = from(Object).getObject("store.book[2]", new TypeRef&lt;Map&lt;String, Object&gt;&gt;() {});
     * </pre>
     * <p/>
     * maps the second book to a Book instance.
     *
     * @param path       The path to the object to map
     * @param typeRef    The class type of the expected object
     * @param <T>        The type of the expected object
     * @return The object
     */
    public <T> T getObject(String path, TypeRef<T> typeRef) {
        AssertParameter.notNull("objectType", "Type ref");
        return getObject(path, typeRef.getTypeAsClass());
    }

    /**
     * Add a parameter for the expression. Example:
     * <pre>
     * String name = System.console().readLine();
     * List&lt;Map&gt; books = with(Object).param("name", name).get("store.book.findAll { book -> book.author == name }");
     * </pre>
     *
     * @param key   The name of the parameter. Just use this name in your expression as a variable
     * @param value The value of the parameter
     * @return New JsonPath instance with the parameter set
     */
    public JsonPath param(String key, Object value) {
        JsonPath newP = new JsonPath(this, config);
        if (newP.params == null) {
            newP.params = new HashMap<String, Object>();
        }
        newP.params.put(key, value);
        return newP;
    }

    /**
     * Peeks into the JSON that JsonPath will parse by printing it to the console. You can
     * continue working with JsonPath afterwards. This is mainly for debug purposes. If you want to return a prettified version of the content
     * see {@link #prettify()}. If you want to return a prettified version of the content and also print it to the console use {@link #prettyPrint()}.
     * <p/>
     * <p>
     * Note that the content is not guaranteed to be looking exactly like the it does at the source. This is because once you peek
     * the content has been downloaded and transformed into another data structure (used by JsonPath) and the JSON is rendered
     * from this data structure.
     * </p>
     *
     * @return The same JsonPath instance
     */
    public JsonPath peek() {
        System.out.println(toJsonString());
        return this;
    }

    /**
     * Peeks into the JSON that JsonPath will parse by printing it to the console in a prettified manner. You can
     * continue working with JsonPath afterwards. This is mainly for debug purposes. If you want to return a prettified version of the content
     * see {@link #prettify()}. If you want to return a prettified version of the content and also print it to the console use {@link #prettyPrint()}.
     * <p/>
     * <p>
     * Note that the content is not guaranteed to be looking exactly like the it does at the source. This is because once you peek
     * the content has been downloaded and transformed into another data structure (used by JsonPath) and the JSON is rendered
     * from this data structure.
     * </p>
     *
     * @return The same JsonPath instance
     */
    public JsonPath prettyPeek() {
        prettyPrint();
        return this;
    }

    /**
     * Get the JSON as a prettified string.
     * <p>
     * Note that the content is not guaranteed to be looking exactly like the it does at the source. This is because once you peek
     * the content has been downloaded and transformed into another data structure (used by JsonPath) and the JSON is rendered
     * from this data structure.
     * </p>
     *
     * @return The JSON as a prettified String.
     */
    public String prettify() {
        final String jsonString = toJsonString();
        return JsonPrettifier.prettifyJson(jsonString);
    }

    /**
     * Get and print the JSON as a prettified string.
     * <p>
     * Note that the content is not guaranteed to be looking exactly like the it does at the source. This is because once you peek
     * the content has been downloaded and transformed into another data structure (used by JsonPath) and the JSON is rendered
     * from this data structure.
     * </p>
     *
     * @return The JSON as a prettified String.
     */
    public String prettyPrint() {
        final String pretty = prettify();
        System.out.println(pretty);
        return pretty;
    }

    /**
     * Configure JsonPath to use a specific Gson object mapper factory
     *
     * @param factory The gson object mapper factory instance
     * @return a new JsonPath instance
     */
    public JsonPath using(GsonObjectMapperFactory factory) {
        return new JsonPath(this, jsonPathConfig.gsonObjectMapperFactory(factory));
    }

    /**
     * Configure JsonPath to use a specific Jackson object mapper factory
     *
     * @param factory The Jackson object mapper factory instance
     * @return a new JsonPath instance
     */
    public JsonPath using(Jackson1ObjectMapperFactory factory) {
        return new JsonPath(this, getJsonPathConfig().jackson1ObjectMapperFactory(factory));
    }

    /**
     * Configure JsonPath to use a specific Jackson 2 object mapper factory
     *
     * @param factory The Jackson 2 object mapper factory instance
     * @return a new JsonPath instance
     */
    public JsonPath using(Jackson2ObjectMapperFactory factory) {
        return new JsonPath(this, getJsonPathConfig().jackson2ObjectMapperFactory(factory));
    }

    /**
     * Configure JsonPath to with a specific JsonPathConfig.
     *
     * @param config The JsonPath config
     * @return a new JsonPath instance
     */
    public JsonPath using(JsonPathConfig config) {
        return new JsonPath(this, config);
    }

    /**
     * Syntactic sugar.
     *
     * @return The same JsonPath instance.
     */
    public JsonPath and() {
        return this;
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param text The text containing the Object document
     */
    public static JsonPath given(String text) {
        return new JsonPath(text);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param stream The stream containing the Object document
     */
    public static JsonPath given(InputStream stream) {
        return new JsonPath(stream);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param file The file containing the Object document
     */
    public static JsonPath given(File file) {
        return new JsonPath(file);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param reader The reader containing the Object document
     */
    public static JsonPath given(Reader reader) {
        return new JsonPath(reader);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param url The URL containing the Object document
     */
    public static JsonPath given(URL url) {
        return new JsonPath(url);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param stream The stream containing the Object document
     */
    public static JsonPath with(InputStream stream) {
        return new JsonPath(stream);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param text The text containing the Object document
     */
    public static JsonPath with(String text) {
        return new JsonPath(text);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param file The file containing the Object document
     */
    public static JsonPath with(File file) {
        return new JsonPath(file);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param reader The reader containing the Object document
     */
    public static JsonPath with(Reader reader) {
        return new JsonPath(reader);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param url The URI containing the Object document
     */
    public static JsonPath with(URL url) {
        return new JsonPath(url);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param stream The stream containing the Object document
     */
    public static JsonPath from(InputStream stream) {
        return new JsonPath(stream);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param text The text containing the Object document
     */
    public static JsonPath from(String text) {
        return new JsonPath(text);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param file The file containing the Object document
     */
    public static JsonPath from(File file) {
        return new JsonPath(file);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param reader The reader containing the Object document
     */
    public static JsonPath from(Reader reader) {
        return new JsonPath(reader);
    }

    /**
     * Instantiate a new JsonPath instance.
     *
     * @param url The URI containing the Object document
     */
    public static JsonPath from(URL url) {
        return new JsonPath(url);
    }

    /**
     * Set the root path of the document so that you don't need to write the entire path. E.g.
     * <pre>
     * final JsonPath jsonPath = new JsonPath(Object).setRoot("store.book");
     * assertThat(jsonPath.getInt("size()"), equalTo(4));
     * assertThat(jsonPath.getList("author", String.class), hasItem("J. R. R. Tolkien"));
     * </pre>
     *
     * @param rootPath The root path to use.
     * @deprecated Use {@link #setRootPath(String)} instead
     */
    @Deprecated
    public JsonPath setRoot(String rootPath) {
        return setRootPath(rootPath);
    }
    /**
     * Set the root path of the document so that you don't need to write the entire path. E.g.
     * <pre>
     * final JsonPath jsonPath = new JsonPath(Object).setRootPath("store.book");
     * assertThat(jsonPath.getInt("size()"), equalTo(4));
     * assertThat(jsonPath.getList("author", String.class), hasItem("J. R. R. Tolkien"));
     * </pre>
     *
     * @param rootPath The root path to use.
     */
    public JsonPath setRootPath(String rootPath) {
        AssertParameter.notNull(rootPath, "Root path");
        this.rootPath = rootPath;
        return this;
    }

    private JsonParser parseInputStream(final InputStream stream) {
        return new JsonParser() {
            @Override
            public Object doParseWith(final ConfigurableJsonSlurper slurper) {
                return new ExceptionCatcher() {
                    protected Object method() throws Exception {
                        return slurper.parse(toReader(stream));
                    }
                }.invoke();
            }
        };
    }

    private JsonParser parseReader(final Reader reader) {
        return new JsonParser() {
            @Override
            public Object doParseWith(final ConfigurableJsonSlurper slurper) {
                return new ExceptionCatcher() {
                    protected Object method() throws Exception {
                        return slurper.parse(reader);
                    }
                }.invoke();
            }
        };
    }

    private JsonParser parseFile(final File file) {
        return new JsonParser() {
            @Override
            public Object doParseWith(final ConfigurableJsonSlurper slurper) {
                return new ExceptionCatcher() {
                    protected Object method() throws Exception {
                        return slurper.parse(new FileReader(file));
                    }
                }.invoke();
            }
        };
    }

    private JsonParser parseText(final String text) {
        return new JsonParser() {
            @Override
            public Object doParseWith(final ConfigurableJsonSlurper slurper) {
                return new ExceptionCatcher() {
                    protected Object method() throws Exception {
                        return slurper.parseText(text);
                    }
                }.invoke();
            }
        };
    }

    private JsonParser parseURL(final URL url) {
        return new JsonParser() {
            @Override
            public Object doParseWith(final ConfigurableJsonSlurper slurper) {
                return new ExceptionCatcher() {
                    protected Object method() throws Exception {
                        return slurper.parse(toReader(url.openStream()));
                    }
                }.invoke();
            }
        };
    }

    private BufferedReader toReader(InputStream in) {
        final JsonPathConfig cfg = getJsonPathConfig();
        try {
            return new BufferedReader(new InputStreamReader(in, cfg.charset()));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Charset is invalid", e);
        }
    }

    private abstract class ExceptionCatcher {

        protected abstract Object method() throws Exception;

        public Object invoke() {
            try {
                return method();
            } catch (Exception e) {
                throw new JsonPathException("Failed to parse the JSON document", e);
            }
        }
    }

    public <T> T getJsonObject(String path) {
        final JSONAssertion jsonAssertion = createJsonAssertion(path, params);
        final Object json = jsonParser.parseWith(createConfigurableJsonSlurper());
        return (T) jsonAssertion.getAsJsonObject(json);
    }

    private JSONAssertion createJsonAssertion(String path, Map<String, Object> params) {
        AssertParameter.notNull(path, "path");
        final JSONAssertion jsonAssertion = new JSONAssertion();
        final boolean appendDot = !rootPath.equals("") && !rootPath.endsWith(".") && !path.startsWith("[");
        final String root = appendDot ? rootPath + "." : rootPath;
        jsonAssertion.setKey(root + path);
        if (params != null) {
            jsonAssertion.setParams(params);
        }
        return jsonAssertion;
    }

    private ConfigurableJsonSlurper createConfigurableJsonSlurper() {
        JsonPathConfig cfg = getJsonPathConfig();
        return new ConfigurableJsonSlurper(cfg.numberReturnType());
    }

    private JsonPathConfig getJsonPathConfig() {
        JsonPathConfig cfg;
        if (config == null && jsonPathConfig == null) {
            cfg = new JsonPathConfig();
        } else if (jsonPathConfig != null) {
            cfg = jsonPathConfig;
        } else {
            cfg = config;
        }
        return cfg;
    }

    private abstract class JsonParser {
        private Object json;

        public final Object parseWith(ConfigurableJsonSlurper slurper) {
            if (json == null) {
                json = doParseWith(slurper);
            }
            return json;
        }

        abstract Object doParseWith(ConfigurableJsonSlurper slurper);

    }

    /**
     * Resets static JsonPath configuration to default values
     */
    public static void reset() {
        JsonPath.config = null;
    }

    private String toJsonString() {
        final Object json = jsonParser.parseWith(createConfigurableJsonSlurper());
        final String jsonString;
        if (json instanceof Map) {
            jsonString = JsonOutput.toJson((Map) json);
        } else {
            jsonString = JsonOutput.toJson(json);
        }
        return jsonString;
    }

    private String objectToString(Object object) {
        return new JsonBuilder(object).toString();
    }

    private Object jsonStringToObject(String object, Class objectType) {
        JsonPathConfig cfg = new JsonPathConfig(getJsonPathConfig());
        if (cfg.hasCustomJackson10ObjectMapperFactory()) {
            cfg = cfg.defaultParserType(JsonParserType.JACKSON_1);
        } else if (cfg.hasCustomGsonObjectMapperFactory()) {
            cfg = cfg.defaultParserType(JsonParserType.GSON);
        } else if (cfg.hasCustomJackson20ObjectMapperFactory()) {
            cfg = cfg.defaultParserType(JsonParserType.JACKSON_2);
        } else if (cfg.hasCustomJohnzonObjectMapperFactory()) {
            cfg = cfg.defaultParserType(JsonParserType.JOHNZON);
        }

        //noinspection unchecked
        return JsonObjectDeserializer.deserialize(object, objectType, cfg);
    }
}
