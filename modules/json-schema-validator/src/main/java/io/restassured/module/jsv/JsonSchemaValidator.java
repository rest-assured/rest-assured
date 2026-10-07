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

package io.restassured.module.jsv;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
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
 * JSON Schema draft-03, draft-04, draft-06, draft-07, 2019-09 and 2020-12 are supported. The version is detected from the <code>$schema</code>
 * keyword of the schema (schemas without <code>$schema</code> are validated as draft-04, as before), but it can also be specified explicitly:
 * <pre>
 * get("/products").then().assertThat().body(matchesJsonSchemaInClasspath("products-schema.json").using(JsonSchemaVersion.DRAFT_2020_12));
 * </pre>
 * See {@link JsonSchemaVersion} for details.
 * </p>
 * <p>
 * It's also possible to specify static configuration that is reused for all matcher invocations. For example if you never wish to use checked validation you can configure
 * that {@link JsonSchemaValidator} like this:
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
 * To reset the {@link JsonSchemaValidator} to its default state you can call {@link #reset()}:
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

    private final Object schema;
    private final JsonSchemaValidatorSettings instanceSettings;

    private ProcessingReport report;
    private List<String> validationErrors;

    private JsonSchemaValidator(Object schema, JsonSchemaValidatorSettings jsonSchemaValidatorSettings) {
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
            JsonNode createSchemaInstance(String input) throws IOException {
                return JsonLoader.fromString(input);
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
            JsonNode createSchemaInstance(Reader input) throws IOException {
                return JsonLoader.fromReader(input);
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
            JsonNode createSchemaInstance(File input) throws IOException {
                return JsonLoader.fromFile(input);
            }
        }.create(file);
    }

    public static JsonSchemaValidator matchesJsonSchema(URL url) {
        return new JsonSchemaValidatorFactory<URL>() {
            @Override
            Object createSchemaInstance(URL input) throws IOException {
                return input;
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
     * Validate the JSON document against the supplied JSON Schema version (draft) instead of detecting it from the <code>$schema</code> keyword.
     * For example:
     * <pre>
     * get("/products").then().body(matchesJsonSchemaInClasspath("products-schema.json").using(JsonSchemaVersion.DRAFT_7));
     * </pre>
     * See {@link JsonSchemaValidatorSettings#schemaVersion(JsonSchemaVersion)} for details.
     *
     * @param schemaVersion The JSON Schema version to use.
     * @return A Hamcrest matcher
     */
    public Matcher<?> using(JsonSchemaVersion schemaVersion) {
        return new JsonSchemaValidator(schema, instanceSettings.schemaVersion(schemaVersion));
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
        report = null;
        validationErrors = null;
        try {
            JsonNode contentAsJsonNode = JsonLoader.fromString(content);
            JsonNode parsedUrlSchema = null;
            JsonSchemaVersion schemaVersion = instanceSettings.schemaVersion();
            if (schemaVersion == null) {
                // Auto-detect the version from the $schema keyword
                final JsonNode schemaNode;
                if (schema instanceof URL) {
                    // Don't fetch remote schemas an extra time just to detect the version. They're only inspected if they're parsed anyway.
                    URL url = (URL) schema;
                    if (instanceSettings.shouldParseUriAndUrlsAsJsonNode() || isLocal(url)) {
                        parsedUrlSchema = parseUrlSchemaQuietly(url);
                    }
                    schemaNode = parsedUrlSchema;
                } else {
                    schemaNode = schema instanceof JsonNode ? (JsonNode) schema : null;
                }
                schemaVersion = detectSchemaVersion(schemaNode);
            }

            if (schemaVersion == null || schemaVersion.isLegacy()) {
                return matchesUsingJsonSchemaFactory(contentAsJsonNode, parsedUrlSchema);
            } else {
                return matchesUsingNetworknt(schemaVersion, contentAsJsonNode, parsedUrlSchema);
            }
        } catch (JsonSchemaValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonSchemaValidationException(e);
        }
    }

    private boolean matchesUsingJsonSchemaFactory(JsonNode contentAsJsonNode, JsonNode parsedUrlSchema) throws Exception {
        JsonSchemaFactory jsonSchemaFactory = instanceSettings.jsonSchemaFactory();
        Schema loadedSchema = parsedUrlSchema != null && instanceSettings.shouldParseUriAndUrlsAsJsonNode() ? new Schema(parsedUrlSchema) : loadSchema(schema, instanceSettings);
        final JsonSchema jsonSchema;
        if (loadedSchema.hasType(JsonNode.class)) {
            jsonSchema = jsonSchemaFactory.getJsonSchema(JsonNode.class.cast(loadedSchema.schema));
        } else if (loadedSchema.hasType(String.class)) {
            jsonSchema = jsonSchemaFactory.getJsonSchema(String.class.cast(loadedSchema.schema));
        } else {
            throw new RuntimeException("Internal error when loading schema from factory. Type was " + loadedSchema.schema.getClass().getName());
        }

        if (instanceSettings.shouldUseCheckedValidation()) {
            report = jsonSchema.validate(contentAsJsonNode);
        } else {
            report = jsonSchema.validateUnchecked(contentAsJsonNode);
        }
        return report.isSuccess();
    }

    private boolean matchesUsingNetworknt(JsonSchemaVersion schemaVersion, JsonNode contentAsJsonNode, JsonNode parsedUrlSchema) throws IOException {
        final JsonNode schemaNode;
        final URL schemaLocation;
        if (schema instanceof URL) {
            schemaLocation = (URL) schema;
            schemaNode = parsedUrlSchema == null ? JsonLoader.fromURL(schemaLocation) : parsedUrlSchema;
        } else if (schema instanceof JsonNode) {
            schemaLocation = null;
            schemaNode = (JsonNode) schema;
        } else {
            throw new RuntimeException("Internal error when loading schema: Input was instance of " + schema.getClass().getName());
        }

        try {
            validationErrors = NetworkntSchemaValidation.validate(schemaVersion, schemaNode, schemaLocation, contentAsJsonNode);
        } catch (NoClassDefFoundError e) {
            throw new JsonSchemaValidationException("Validating JSON Schema " + schemaVersion + " requires com.networknt:json-schema-validator to be available in the classpath.", e);
        }
        return validationErrors.isEmpty();
    }

    private static JsonSchemaVersion detectSchemaVersion(JsonNode schemaNode) {
        if (schemaNode == null || !schemaNode.isObject()) {
            return null;
        }
        JsonNode schemaKeyword = schemaNode.get("$schema");
        if (schemaKeyword == null || !schemaKeyword.isTextual()) {
            return null;
        }
        return JsonSchemaVersion.fromMetaSchemaUri(schemaKeyword.textValue());
    }

    private static boolean isLocal(URL url) {
        String protocol = url.getProtocol();
        return "file".equalsIgnoreCase(protocol) || "jar".equalsIgnoreCase(protocol);
    }

    private static JsonNode parseUrlSchemaQuietly(URL url) {
        try {
            return JsonLoader.fromURL(url);
        } catch (Exception e) {
            // Let the java-json-tools validator load (and report problems with) the schema, as before
            return null;
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
        } else if (validationErrors != null) {
            description.appendText("The content to match the given JSON schema.\n");
            for (String validationError : validationErrors) {
                description.appendText(validationError).appendText("\n");
            }
        }
    }

    private Schema loadSchema(Object input, JsonSchemaValidatorSettings instanceSettings) {
        if (input instanceof JsonNode) {
            return new Schema(input);
        } else if (input instanceof URL) {
            final Object loadedSchema;
            if (instanceSettings.shouldParseUriAndUrlsAsJsonNode()) {
                try {
                    loadedSchema = JsonLoader.fromURL((URL) input);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                loadedSchema = input.toString();
            }
            return new Schema(loadedSchema);
        } else {
            throw new RuntimeException("Internal error when loading schema: Input was instance of " + input.getClass().getName());
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
            Object loadedSchema;
            try {
                loadedSchema = createSchemaInstance(schema);
            } catch (IOException e) {
                throw new JsonSchemaValidationException(e);
            }

            return new JsonSchemaValidator(loadedSchema, createSettings());
        }

        abstract Object createSchemaInstance(T input) throws IOException;
    }

    private static class Schema {
        private final Object schema;

        private Schema(Object result) {
            this.schema = result;
        }

        public boolean hasType(Class<?> type) {
            return type.isAssignableFrom(schema.getClass());
        }

        public <T> T as(Class<T> type) {
            return type.cast(schema);
        }
    }

    /**
     * Reset the static {@link #settings} to <code>null</code>.
     */
    public static void reset() {
        settings = null;
    }
}
