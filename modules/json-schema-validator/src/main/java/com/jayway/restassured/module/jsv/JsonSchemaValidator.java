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

package com.jayway.restassured.module.jsv;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ProcessingMessage;
import com.github.fge.jsonschema.report.ProcessingReport;
import com.google.common.collect.Lists;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * A Hamcrest matcher that can be used to validate that a JSON document matches a given <a href="http://json-schema.org/">JSON schema</a>.
 * Typical use-case in REST Assured:
 * <pre>
 * get("/products").then().assertThat().body(matchesJsonSchemaInClasspath("products-schema.json"));
 * </pre>
 * <p/>
 * The {@link #matchesJsonSchemaInClasspath(String)} is defined in this class and validates that the response body of the request to "<tt>/products</tt>"
 * matches the <tt>products-schema.json</tt> schema located in classpath. It's also possible to supply some settings, for example the {@link com.github.fge.jsonschema.main.JsonSchemaFactory}
 * that the matcher will use when validating the schema:
 * <pre>
 * JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.newBuilder().setValidationConfiguration(ValidationConfiguration.newBuilder().setDefaultVersion(DRAFTV4).freeze()).freeze();
 * get("/products").then().assertThat().body(matchesJsonSchemaInClasspath("products-schema.json").using(jsonSchemaFactory));
 * </pre>
 * or:
 * <pre>
 * get("/products").then().assertThat().body(matchesJsonSchemaInClasspath("products-schema.json").using(settings().with().checkedValidation(false)))
 * </pre>
 * where "settings" is found in {@link JsonSchemaValidatorSettings#settings()}.
 * <p>
 * It's also possible to specify static configuration that is reused for all matcher invocations. For example if you never wish to use checked validation you can configure
 * that {@link com.jayway.restassured.module.jsv.JsonSchemaValidator} like this:
 * <p/>
 * <pre>
 * JsonSchemaValidator.settings = settings().with().checkedValidation(false);
 * </pre>
 * This means that
 * <pre>
 * get("/products").then().assertThat().body(matchesJsonSchemaInClasspath("products-schema.json"));
 * </pre>
 * will use unchecked validation (since it was configured statically).
 * <p>
 * To reset the {@link com.jayway.restassured.module.jsv.JsonSchemaValidator} to its default state you can call {@link #reset()}:
 * </p>
 * <pre>
 * JsonSchemaValidator.reset();
 * </pre>
 * </p>
 */
public class JsonSchemaValidator extends TypeSafeMatcher<String> {

    /**
     * Default json schema factory instance
     */
    public static JsonSchemaValidatorSettings settings;

    private final JsonNode schema;
    private final JsonSchemaValidatorSettings instanceSettings;

    private ProcessingReport report;

    private JsonSchemaValidator(JsonNode schema, JsonSchemaValidatorSettings jsonSchemaValidatorSettings) {
        if (jsonSchemaValidatorSettings == null) {
            throw new IllegalArgumentException(JsonSchemaValidatorSettings.class.getSimpleName() + " cannot be null.");
        }
        this.schema = schema;
        this.instanceSettings = jsonSchemaValidatorSettings;
    }

    /**
     * Creates a Hamcrest matcher that validates that a JSON document conforms to the JSON schema provided to this method.
     *
     * @param schema The string defining the JSON schema
     * @return A Hamcrest matcher
     */
    public static JsonSchemaValidator matchesJsonSchema(String schema) {
        return new JsonSchemaValidatorFactory<String>() {
            @Override
            JsonNode createJsonNodeInstance(String schema) throws IOException {
                return JsonLoader.fromString(schema);
            }
        }.create(schema);
    }

    /**
     * Creates a Hamcrest matcher that validates that a JSON document conforms to the JSON schema provided to this method.
     *
     * @param pathToSchemaInClasspath The string that points to a JSON schema in classpath.
     * @return A Hamcrest matcher
     */
    public static JsonSchemaValidator matchesJsonSchemaInClasspath(String pathToSchemaInClasspath) {
        return matchesJsonSchema(Thread.currentThread().getContextClassLoader().getResource(pathToSchemaInClasspath));
    }

    /**
     * Creates a Hamcrest matcher that validates that a JSON document conforms to the JSON schema provided to this method.
     *
     * @param schema The input stream that points to a JSON schema
     * @return A Hamcrest matcher
     */
    public static JsonSchemaValidator matchesJsonSchema(InputStream schema) {
        return matchesJsonSchema(new InputStreamReader(schema));
    }

    /**
     * Creates a Hamcrest matcher that validates that a JSON document conforms to the JSON schema provided to this method.
     *
     * @param schema The reader that points to a JSON schema
     * @return A Hamcrest matcher
     */
    public static JsonSchemaValidator matchesJsonSchema(Reader schema) {
        return new JsonSchemaValidatorFactory<Reader>() {
            @Override
            JsonNode createJsonNodeInstance(Reader schema) throws IOException {
                return JsonLoader.fromReader(schema);
            }
        }.create(schema);
    }

    /**
     * Creates a Hamcrest matcher that validates that a JSON document conforms to the JSON schema provided to this method.
     *
     * @param file The file that points to a JSON schema
     * @return A Hamcrest matcher
     */
    public static JsonSchemaValidator matchesJsonSchema(File file) {
        return new JsonSchemaValidatorFactory<File>() {
            @Override
            JsonNode createJsonNodeInstance(File schema) throws IOException {
                return JsonLoader.fromFile(schema);
            }
        }.create(file);
    }

    public static JsonSchemaValidator matchesJsonSchema(URL url) {
        return new JsonSchemaValidatorFactory<URL>() {
            @Override
            JsonNode createJsonNodeInstance(URL schema) throws IOException {
                return JsonLoader.fromURL(schema);
            }
        }.create(url);
    }

    /**
     * Creates a Hamcrest matcher that validates that a JSON document conforms to the JSON schema loaded by the supplied URI.
     * <p>
     * Note: Converts the URI to a URL and loads this URL.
     * </p>
     *
     * @param uri The URI that points to a JSON schema
     * @return A Hamcrest matcher
     */
    public static JsonSchemaValidator matchesJsonSchema(URI uri) {
        return matchesJsonSchema(toURL(uri));
    }

    /**
     * Validate the JSON document using the supplied <code>jsonSchemaFactory</code> instance.
     *
     * @param jsonSchemaFactory The json schema factory instance to use.
     * @return A Hamcrest matcher
     */
    public Matcher<?> using(JsonSchemaFactory jsonSchemaFactory) {
        return new JsonSchemaValidator(schema, instanceSettings.jsonSchemaFactory(jsonSchemaFactory));
    }

    /**
     * Validate the JSON document using the supplied <code>jsonSchemaValidatorSettings</code> instance.
     *
     * @param jsonSchemaValidatorSettings The json schema validator settings instance to use.
     * @return A Hamcrest matcher
     */
    public Matcher<?> using(JsonSchemaValidatorSettings jsonSchemaValidatorSettings) {
        return new JsonSchemaValidator(schema, jsonSchemaValidatorSettings);
    }

    private static URL toURL(URI uri) {
        validateSchemaIsNotNull(uri);
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Can't convert the supplied URI to a URL", e);
        }
    }

    @Override
    protected boolean matchesSafely(String content) {
        try {
            JsonNode contentAsJsonNode = JsonLoader.fromString(content);
            JsonSchema jsonSchema = instanceSettings.jsonSchemaFactory().getJsonSchema(schema);
            if (instanceSettings.shouldUseCheckedValidation()) {
                report = jsonSchema.validate(contentAsJsonNode);
            } else {
                report = jsonSchema.validateUnchecked(contentAsJsonNode);
            }
            return report.isSuccess();
        } catch (Exception e) {
            throw new JsonSchemaValidationException(e);
        }
    }

    public void describeTo(Description description) {
        if (report != null) {
            description.appendText("The content to match the given JSON schema.\n");
            List<ProcessingMessage> messages = Lists.newArrayList(report);
            if (!messages.isEmpty()) {
                for (final ProcessingMessage message : messages)
                    description.appendText(message.toString());
            }
        }
    }

    private static void validateSchemaIsNotNull(Object schema) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema to use cannot be null");
        }
    }

    private static abstract class JsonSchemaValidatorFactory<T> {

        private JsonSchemaValidatorSettings createSettings() {
            return settings == null ? new JsonSchemaValidatorSettings() : settings;
        }

        public JsonSchemaValidator create(T schema) {
            validateSchemaIsNotNull(schema);
            JsonNode schemaNode;
            try {
                schemaNode = createJsonNodeInstance(schema);
            } catch (IOException e) {
                throw new JsonSchemaValidationException(e);
            }

            return new JsonSchemaValidator(schemaNode, createSettings());
        }

        abstract JsonNode createJsonNodeInstance(T schema) throws IOException;
    }

    /**
     * Reset the static {@link #settings} to <code>null</code>.
     */
    public static void reset() {
        settings = null;
    }
}
