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

public class JsonSchemaValidator extends TypeSafeMatcher<String> {

    /**
     * Default json schema factory instance
     */
    public static JsonSchemaFactory jsonSchemaFactory;

    private final JsonNode schema;

    private ProcessingReport report;

    private JsonSchemaValidator(JsonNode schema) {
        this.schema = schema;
    }

    /**
     * Creates a Hamcrest matcher that validates that a JSON document conforms to the JSON schema provided to this method.
     *
     * @param schema The string defining the JSON schema
     * @return A Hamcrest matcher
     */
    public static Matcher<?> matchesJsonSchema(String schema) {
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
    public static Matcher<?> matchesJsonSchemaInClasspath(String pathToSchemaInClasspath) {
        return matchesJsonSchema(Thread.currentThread().getContextClassLoader().getResource(pathToSchemaInClasspath));
    }

    /**
     * Creates a Hamcrest matcher that validates that a JSON document conforms to the JSON schema provided to this method.
     *
     * @param schema The input stream that points to a JSON schema
     * @return A Hamcrest matcher
     */
    public static Matcher<?> matchesJsonSchema(InputStream schema) {
        return matchesJsonSchema(new InputStreamReader(schema));
    }

    /**
     * Creates a Hamcrest matcher that validates that a JSON document conforms to the JSON schema provided to this method.
     *
     * @param schema The reader that points to a JSON schema
     * @return A Hamcrest matcher
     */
    public static Matcher<?> matchesJsonSchema(Reader schema) {
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
    public static Matcher<?> matchesJsonSchema(File file) {
        return new JsonSchemaValidatorFactory<File>() {
            @Override
            JsonNode createJsonNodeInstance(File schema) throws IOException {
                return JsonLoader.fromFile(schema);
            }
        }.create(file);
    }

    public static Matcher<?> matchesJsonSchema(URL url) {
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
    public static Matcher<?> matchesJsonSchema(URI uri) {
        return matchesJsonSchema(toURL(uri));
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
            JsonSchema jsonSchema = jsonSchemaFactory().getJsonSchema(schema);
            report = jsonSchema.validate(contentAsJsonNode);
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

    private JsonSchemaFactory jsonSchemaFactory() {
        return jsonSchemaFactory == null ? JsonSchemaFactory.byDefault() : jsonSchemaFactory;
    }

    private static void validateSchemaIsNotNull(Object schema) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema to use cannot be null");
        }
    }


    private static abstract class JsonSchemaValidatorFactory<T> {

        public JsonSchemaValidator create(T schema) {
            validateSchemaIsNotNull(schema);
            JsonNode schemaNode;
            try {
                schemaNode = createJsonNodeInstance(schema);
            } catch (IOException e) {
                throw new JsonSchemaValidationException(e);
            }

            return new JsonSchemaValidator(schemaNode);
        }

        abstract JsonNode createJsonNodeInstance(T schema) throws IOException;

    }

}
