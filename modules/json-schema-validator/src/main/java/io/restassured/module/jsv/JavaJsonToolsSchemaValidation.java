/*
 * Copyright 2026 the original author or authors.
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
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Validates JSON documents using the java-json-tools json-schema-validator, which is optional.
 * <p>
 * All references to java-json-tools classes that are used when validating are kept in this class, so that nothing loads them
 * when java-json-tools isn't on the classpath.
 * </p>
 */
class JavaJsonToolsSchemaValidation {

    private JavaJsonToolsSchemaValidation() {
    }

    static JsonSchemaFactory defaultJsonSchemaFactory() {
        return JsonSchemaFactory.byDefault();
    }

    /**
     * @param settings   The settings, with the {@link JsonSchemaFactory} to use
     * @param schema     The schema as given to the matcher, a parsed schema or a {@link URL}
     * @param schemaJson The schema as JSON, or <code>null</code> if a {@link URL} schema hasn't been loaded
     * @param content    The JSON document to validate
     * @param messages   The list that the messages of the validation report are added to, each ending with a line break
     * @return <code>true</code> if the document is valid
     */
    static boolean validate(JsonSchemaValidatorSettings settings, Object schema, String schemaJson, String content, List<String> messages) throws IOException, ProcessingException {
        JsonNode contentAsJsonNode = JsonLoader.fromString(content);
        JsonSchemaFactory jsonSchemaFactory = settings.jsonSchemaFactory();
        final JsonSchema jsonSchema;
        if (schema instanceof URL && !settings.shouldParseUriAndUrlsAsJsonNode()) {
            // Lets the validator resolve relative $ref's against the URL
            jsonSchema = jsonSchemaFactory.getJsonSchema(schema.toString());
        } else if (schemaJson != null) {
            jsonSchema = jsonSchemaFactory.getJsonSchema(JsonLoader.fromString(schemaJson));
        } else {
            jsonSchema = jsonSchemaFactory.getJsonSchema(JsonLoader.fromURL((URL) schema));
        }

        ProcessingReport report = settings.shouldUseCheckedValidation() ? jsonSchema.validate(contentAsJsonNode) : jsonSchema.validateUnchecked(contentAsJsonNode);
        for (ProcessingMessage message : report) {
            messages.add(message.toString());
        }
        return report.isSuccess();
    }
}
