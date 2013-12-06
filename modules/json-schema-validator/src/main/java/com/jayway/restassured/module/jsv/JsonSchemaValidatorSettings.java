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

import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class JsonSchemaValidatorSettings {
    private JsonSchemaFactory jsonSchemaFactory;
    private boolean checkedValidation;

    public JsonSchemaValidatorSettings(JsonSchemaFactory jsonSchemaFactory, boolean checkedValidation) {
        if (jsonSchemaFactory == null) {
            throw new IllegalArgumentException(JsonSchemaFactory.class.getSimpleName() + " cannot be null");
        }
        this.jsonSchemaFactory = jsonSchemaFactory;
        this.checkedValidation = checkedValidation;
    }

    public JsonSchemaValidatorSettings(JsonSchemaFactory jsonSchemaFactory) {
        this(jsonSchemaFactory, true);
    }

    public JsonSchemaValidatorSettings() {
        this(JsonSchemaFactory.byDefault(), true);
    }

    public JsonSchemaFactory jsonSchemaFactory() {
        return jsonSchemaFactory;
    }

    public boolean shouldUseCheckedValidation() {
        return checkedValidation;
    }

    public JsonSchemaValidatorSettings checkedValidation(boolean shouldUseCheckedValidation) {
        return new JsonSchemaValidatorSettings(jsonSchemaFactory, shouldUseCheckedValidation);
    }

    public JsonSchemaValidatorSettings jsonSchemaFactory(JsonSchemaFactory jsonSchemaFactory) {
        return new JsonSchemaValidatorSettings(jsonSchemaFactory, shouldUseCheckedValidation());
    }

    /**
     * Syntactic sugar.
     *
     * @return The same settings instance.
     */
    public JsonSchemaValidatorSettings and() {
        return this;
    }

    public static JsonSchemaValidatorSettings settings() {
        return new JsonSchemaValidatorSettings();
    }
}
