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
import java.net.URL;
import java.util.List;

// TODO Null checks!
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

    public static Matcher<?> matchesJsonSchema(String schema) {
        JsonNode schemaNode;
        try {
            schemaNode = JsonLoader.fromString(schema);
        } catch (IOException e) {
            throw new JsonSchemaValidationException(e);
        }

        return new JsonSchemaValidator(schemaNode);
    }

    public static Matcher<?> matchesJsonSchemaInClasspath(String pathToSchemaInClasspath) {
        return matchesJsonSchema(Thread.currentThread().getContextClassLoader().getResource(pathToSchemaInClasspath));
    }

    public static Matcher<?> matchesJsonSchema(InputStream schema) {
        JsonNode schemaNode;
        try {
            schemaNode = JsonLoader.fromReader(new InputStreamReader(schema));
        } catch (IOException e) {
            throw new JsonSchemaValidationException(e);
        }

        return new JsonSchemaValidator(schemaNode);
    }

    public static Matcher<?> matchesJsonSchema(Reader schema) {
        JsonNode schemaNode;
        try {
            schemaNode = JsonLoader.fromReader(schema);
        } catch (IOException e) {
            throw new JsonSchemaValidationException(e);
        }

        return new JsonSchemaValidator(schemaNode);
    }

    public static Matcher<?> matchesJsonSchema(File file) {
        JsonNode schemaNode;
        try {
            schemaNode = JsonLoader.fromFile(file);
        } catch (IOException e) {
            throw new JsonSchemaValidationException(e);
        }

        return new JsonSchemaValidator(schemaNode);
    }

    public static Matcher<?> matchesJsonSchema(URL url) {
        JsonNode schemaNode;
        try {
            schemaNode = JsonLoader.fromURL(url);
        } catch (IOException e) {
            throw new JsonSchemaValidationException(e);
        }

        return new JsonSchemaValidator(schemaNode);
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
}
