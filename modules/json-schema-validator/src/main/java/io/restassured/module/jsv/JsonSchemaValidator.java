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

import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A Hamcrest matcher that validates that a JSON document matches a <a href="https://json-schema.org/">JSON schema</a>, for example:
 * <pre>
 * get("/products").then().assertThat().body(matchesJsonSchemaInClasspath("products-schema.json"));
 * </pre>
 * The schema is validated according to the draft that its <code>$schema</code> declares, or the version configured with
 * {@link #using(JsonSchemaVersion)} for schemas without <code>$schema</code>:
 * <ul>
 * <li>Draft-06, draft-07, 2019-09 and 2020-12 are validated by the networknt json-schema-validator.</li>
 * <li>Draft-04, the default for schemas without <code>$schema</code>, is validated by the java-json-tools json-schema-validator if
 * it's on the classpath, as in REST Assured 6, and otherwise by the networknt json-schema-validator.</li>
 * <li>Draft-03 requires the java-json-tools json-schema-validator.</li>
 * </ul>
 * See {@link JsonSchemaVersion} for details. With java-json-tools on the classpath you can also validate with your own
 * {@link JsonSchemaFactory}:
 * <pre>
 * get("/products").then().assertThat().body(matchesJsonSchemaInClasspath("products-schema.json").using(jsonSchemaFactory));
 * </pre>
 * Other settings are given with {@link #using(JsonSchemaValidatorSettings)}, or for all matchers with the static {@link #settings}:
 * <pre>
 * JsonSchemaValidator.settings = settings().with().schemaVersion(JsonSchemaVersion.DRAFT_2020_12);
 * </pre>
 * {@link #reset()} sets them back to the defaults.
 */
public class JsonSchemaValidator extends TypeSafeMatcher<String> {

    /**
     * The settings of each matcher that has none of its own, shared by all threads. It's a standalone setting, never a
     * setting of a request: neither the config of a request nor a <code>RestAssuredClient</code> changes it. To use
     * other settings for one matcher, use {@link #using(JsonSchemaValidatorSettings)}, for example
     * <code>matchesJsonSchemaInClasspath("products-schema.json").using(settings().with().checkedValidation(false))</code>.
     */
    public static volatile JsonSchemaValidatorSettings settings;

    static final boolean JAVA_JSON_TOOLS_AVAILABLE = isJavaJsonToolsAvailable();

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).build();

    /**
     * The parsed schema (a {@link JsonNode}) or the {@link URL} to load it from.
     */
    private final Object schema;
    /**
     * The location of a schema that has been loaded into a JsonNode (e.g. a File), used to resolve relative $ref's. May be null.
     */
    private final String schemaLocation;
    private final JsonSchemaValidatorSettings instanceSettings;

    private List<String> validationMessages;

    private JsonSchemaValidator(Object schema, String schemaLocation, JsonSchemaValidatorSettings jsonSchemaValidatorSettings) {
        // Without it, TypeSafeMatcher finds the type by reflection, which loads the java-json-tools classes of using(JsonSchemaFactory)
        super(String.class);
        if (jsonSchemaValidatorSettings == null) {
            throw new IllegalArgumentException(JsonSchemaValidatorSettings.class.getSimpleName() + " cannot be null.");
        }
        this.schema = schema;
        this.schemaLocation = schemaLocation;
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
            JsonNode createSchemaInstance(String input) {
                return JSON_MAPPER.readTree(input);
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
            JsonNode createSchemaInstance(Reader input) {
                return JSON_MAPPER.readTree(input);
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
            JsonNode createSchemaInstance(File input) {
                return JSON_MAPPER.readTree(input);
            }

            @Override
            String schemaLocation(File input) {
                return input.getAbsoluteFile().toURI().toString();
            }
        }.create(file);
    }

    public static JsonSchemaValidator matchesJsonSchema(URL url) {
        return new JsonSchemaValidatorFactory<URL>() {
            @Override
            Object createSchemaInstance(URL input) {
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
     * Validate draft-03 and draft-04 schemas, and schemas without <code>$schema</code>, using the supplied <code>jsonSchemaFactory</code>
     * instance. Requires the java-json-tools json-schema-validator.
     *
     * @param jsonSchemaFactory The json schema factory instance to use.
     * @return A Hamcrest matcher
     */
    public Matcher<?> using(JsonSchemaFactory jsonSchemaFactory) {
        return new JsonSchemaValidator(schema, schemaLocation, instanceSettings.jsonSchemaFactory(jsonSchemaFactory));
    }

    /**
     * Validate the JSON document against the supplied JSON Schema version (draft) if the schema doesn't declare <code>$schema</code>.
     * A <code>$schema</code> declared by the schema always takes precedence. For example:
     * <pre>
     * get("/products").then().body(matchesJsonSchemaInClasspath("products-schema.json").using(JsonSchemaVersion.DRAFT_7));
     * </pre>
     * See {@link JsonSchemaValidatorSettings#schemaVersion(JsonSchemaVersion)} for details.
     *
     * @param schemaVersion The JSON Schema version to use.
     * @return A Hamcrest matcher
     */
    public Matcher<?> using(JsonSchemaVersion schemaVersion) {
        return new JsonSchemaValidator(schema, schemaLocation, instanceSettings.schemaVersion(schemaVersion));
    }

    /**
     * Validate the JSON document using the supplied <code>jsonSchemaValidatorSettings</code> instance.
     *
     * @param jsonSchemaValidatorSettings The json schema validator settings instance to use.
     * @return A Hamcrest matcher
     */
    public Matcher<?> using(JsonSchemaValidatorSettings jsonSchemaValidatorSettings) {
        return new JsonSchemaValidator(schema, schemaLocation, jsonSchemaValidatorSettings);
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
        validationMessages = null;
        try {
            JsonSchemaVersion configuredVersion = instanceSettings.schemaVersion();
            final JsonNode schemaNode;
            if (schema instanceof URL) {
                // Don't fetch remote schemas an extra time just to detect the version. They're only inspected if they're parsed anyway.
                URL url = (URL) schema;
                boolean parsedAnyway = !JAVA_JSON_TOOLS_AVAILABLE || instanceSettings.shouldParseUriAndUrlsAsJsonNode()
                        || (configuredVersion != null && configuredVersion != JsonSchemaVersion.DRAFT_4);
                schemaNode = parsedAnyway || isLocal(url) ? parseUrlSchemaQuietly(url) : null;
            } else {
                schemaNode = (JsonNode) schema;
            }

            // A $schema declared by the schema always wins, the configured version only applies to schemas without (a known) $schema
            String declaredSchema = declaredSchema(schemaNode);
            boolean draft3 = JsonSchemaVersion.isDraft3MetaSchemaUri(declaredSchema);
            JsonSchemaVersion declaredVersion = JsonSchemaVersion.fromMetaSchemaUri(declaredSchema);
            JsonSchemaVersion schemaVersion = draft3 ? null : declaredVersion == null ? configuredVersion : declaredVersion;

            if (JAVA_JSON_TOOLS_AVAILABLE && (schemaVersion == null || schemaVersion == JsonSchemaVersion.DRAFT_4)) {
                List<String> messages = new ArrayList<>();
                String schemaJson = schemaNode == null ? null : schemaNode.toString();
                boolean valid = JavaJsonToolsSchemaValidation.validate(instanceSettings, schema, schemaJson, content, messages);
                validationMessages = messages;
                return valid;
            } else if (draft3) {
                throw javaJsonToolsRequired("Validating a draft-03 schema");
            } else {
                return matchesUsingNetworknt(schemaVersion == null ? JsonSchemaVersion.DRAFT_4 : schemaVersion, schemaNode, content);
            }
        } catch (JsonSchemaValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonSchemaValidationException(e);
        }
    }

    private boolean matchesUsingNetworknt(JsonSchemaVersion schemaVersion, JsonNode schemaNode, String content) throws IOException {
        JsonNode contentAsJsonNode = JSON_MAPPER.readTree(content);
        if (contentAsJsonNode.isMissingNode()) {
            // As with java-json-tools, an empty body isn't a JSON document, so it neither matches nor fails to match a schema
            throw new JsonSchemaValidationException("The content to validate is empty, so it isn't a JSON document", null);
        }
        String location = schema instanceof URL ? schema.toString() : schemaLocation;
        JsonNode loadedSchema = schemaNode == null ? parseUrlSchema((URL) schema) : schemaNode;
        validationMessages = NetworkntSchemaValidation.validate(schemaVersion, loadedSchema, location, contentAsJsonNode);
        return validationMessages.isEmpty();
    }

    private static String declaredSchema(JsonNode schemaNode) {
        if (schemaNode == null || !schemaNode.isObject()) {
            return null;
        }
        JsonNode schemaKeyword = schemaNode.get("$schema");
        return schemaKeyword != null && schemaKeyword.isString() ? schemaKeyword.stringValue() : null;
    }

    private static boolean isLocal(URL url) {
        // Treat everything except network protocols as local, e.g. file:, jar:, wsjar:, vfs: and bundleresource:
        String protocol = url.getProtocol().toLowerCase(Locale.ROOT);
        return !(protocol.equals("http") || protocol.equals("https") || protocol.equals("ftp"));
    }

    private static JsonNode parseUrlSchemaQuietly(URL url) {
        try {
            return parseUrlSchema(url);
        } catch (Exception e) {
            // Let the validator load (and report problems with) the schema, as before
            return null;
        }
    }

    private static JsonNode parseUrlSchema(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return JSON_MAPPER.readTree(in);
        }
    }

    public void describeTo(Description description) {
        if (validationMessages != null) {
            description.appendText("The content to match the given JSON schema.\n");
            for (String validationMessage : validationMessages) {
                description.appendText(validationMessage);
            }
        }
    }

    private static boolean isJavaJsonToolsAvailable() {
        try {
            Class.forName("com.github.fge.jsonschema.main.JsonSchemaFactory", false, JsonSchemaValidator.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    static JsonSchemaValidationException javaJsonToolsRequired(String what) {
        return new JsonSchemaValidationException(what + " requires the java-json-tools json-schema-validator. Add it to the classpath, with Maven:\n" +
                "<dependency>\n" +
                "    <groupId>com.github.java-json-tools</groupId>\n" +
                "    <artifactId>json-schema-validator</artifactId>\n" +
                "    <version>2.2.14</version>\n" +
                "    <scope>test</scope>\n" +
                "</dependency>\n" +
                "or with Gradle: testImplementation(\"com.github.java-json-tools:json-schema-validator:2.2.14\")", null);
    }

    private static void validateSchemaIsNotNull(Object schema) {
        if (schema == null) {
            throw new IllegalArgumentException("Schema to use cannot be null");
        }
    }

    private static abstract class JsonSchemaValidatorFactory<T> {

        private JsonSchemaValidatorSettings createSettings() {
            JsonSchemaValidatorSettings currentSettings = settings;
            return currentSettings == null ? new JsonSchemaValidatorSettings() : currentSettings;
        }

        public JsonSchemaValidator create(T schema) {
            validateSchemaIsNotNull(schema);
            Object loadedSchema;
            try {
                loadedSchema = createSchemaInstance(schema);
            } catch (JacksonException e) {
                throw new JsonSchemaValidationException(e);
            }

            return new JsonSchemaValidator(loadedSchema, schemaLocation(schema), createSettings());
        }

        abstract Object createSchemaInstance(T input);

        String schemaLocation(T input) {
            return null;
        }
    }

    /**
     * Reset the static {@link #settings} to <code>null</code>.
     */
    public static void reset() {
        settings = null;
    }
}
