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

package io.restassured.config;

import io.restassured.path.json.config.JsonPathConfig;
import org.apache.commons.lang3.Validate;

/**
 * Allows you to configure properties of JSON parsing.
 */
public class JsonConfig implements Config {
    private final JsonPathConfig.NumberReturnType numberReturnType;
    private final boolean isUserDefined;

    /**
     * Create a new instance of XmlConfig without any features and that is namespace unaware.
     */
    public JsonConfig() {
        this(JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE, false);
    }

    public JsonConfig(JsonPathConfig.NumberReturnType numberReturnType) {
        this(numberReturnType, true);
    }

    public JsonConfig(JsonPathConfig.NumberReturnType numberReturnType, boolean isUserDefined) {
        Validate.notNull(numberReturnType, "numberReturnType cannot be null");
        this.numberReturnType = numberReturnType;
        this.isUserDefined = isUserDefined;
    }

    public JsonPathConfig.NumberReturnType numberReturnType() {
        return numberReturnType;
    }

    public boolean shouldRepresentJsonNumbersAsBigDecimal() {
        return numberReturnType() == JsonPathConfig.NumberReturnType.BIG_DECIMAL;
    }

    /**
     * Specifies if JSON parsing should use floats and doubles or BigDecimals to represent Json numbers.
     *
     * @param numberReturnType The choice.
     * @return A new instance of JsonConfig with the given configuration
     */
    public JsonConfig numberReturnType(JsonPathConfig.NumberReturnType numberReturnType) {
        return new JsonConfig(numberReturnType, true);
    }

    /**
     * For syntactic sugar.
     *
     * @return The same JsonConfig instance
     */
    public JsonConfig with() {
        return this;
    }

    /**
     * @return A static way to create a new JsonConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static JsonConfig jsonConfig() {
        return new JsonConfig();
    }

    public boolean isUserConfigured() {
        return isUserDefined;
    }
}

