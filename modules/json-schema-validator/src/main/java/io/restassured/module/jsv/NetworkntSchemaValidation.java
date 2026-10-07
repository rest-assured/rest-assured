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
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates JSON documents against draft-06, draft-07, 2019-09 and 2020-12 schemas using the networknt json-schema-validator.
 * <p>
 * All references to networknt classes are kept in this class so that the java-json-tools based (draft-03/draft-04)
 * validation keeps working even if the networknt library is excluded from the classpath.
 * </p>
 */
class NetworkntSchemaValidation {

    private NetworkntSchemaValidation() {
    }

    /**
     * @param version        The version to use for schemas that don't declare <code>$schema</code>. A <code>$schema</code> declared by the schema takes precedence.
     * @param schemaNode     The schema
     * @param schemaLocation The location the schema was loaded from (used to resolve relative <code>$ref</code>'s) or <code>null</code> if unknown.
     * @param content        The JSON document to validate
     * @return The validation error messages, empty if the document is valid.
     */
    static List<String> validate(JsonSchemaVersion version, JsonNode schemaNode, URL schemaLocation, JsonNode content) {
        // Allow $ref's to be resolved from file:, jar: and http(s): locations, like the java-json-tools validator does
        SchemaRegistry schemaRegistry = SchemaRegistry.withDefaultDialect(SpecificationVersion.valueOf(version.name()),
                builder -> builder.schemaLoader(schemaLoader -> schemaLoader.fetchRemoteResources()));
        Schema schema = schemaLocation == null ? schemaRegistry.getSchema(schemaNode) : schemaRegistry.getSchema(SchemaLocation.of(schemaLocation.toString()), schemaNode);
        List<Error> errors = schema.validate(content);
        List<String> messages = new ArrayList<>(errors.size());
        for (Error error : errors) {
            messages.add(error.toString());
        }
        return messages;
    }
}
