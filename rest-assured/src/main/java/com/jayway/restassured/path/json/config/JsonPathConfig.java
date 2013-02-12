/*
 * Copyright 2012 the original author or authors.
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
package com.jayway.restassured.path.json.config;

import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;
import static com.jayway.restassured.path.json.config.JsonPathConfig.NumberReturnType.FLOAT_AND_DOUBLE;

/**
 * Allows you to configure how the JsonPath will handle JSON numbers. By default JsonPath and body expectations
 * in REST Assured will return floats for numbers that are less than or equal to {@link Float#MAX_VALUE} or doubles
 * for larger numbers. In Groovy 1.8.5 they changed so that all numbers are now BigDecimals. If you prefer that
 * you can configure the {@link NumberReturnType} to be {@link NumberReturnType#BIG_DECIMAL}.
 */
public class JsonPathConfig {

    private final NumberReturnType numberReturnType;

    /**
     * Creates a new JsonPathConfig that is configured to return floats and doubles.
     */
    public JsonPathConfig() {
        this(FLOAT_AND_DOUBLE);
    }

    public JsonPathConfig(NumberReturnType numberReturnType) {
        if(numberReturnType == null) throw new IllegalArgumentException("numberReturnType cannot be null");
        this.numberReturnType = numberReturnType;
    }

    public NumberReturnType numberReturnType() {
        return numberReturnType;
    }

    public JsonPathConfig numberReturnType(NumberReturnType numberReturnType) {
        return new JsonPathConfig(numberReturnType);
    }

    public boolean shouldRepresentJsonNumbersAsBigDecimal() {
        return numberReturnType() == BIG_DECIMAL;
    }

    /**
     * @return A static way to create a new JsonPathConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static JsonPathConfig jsonPathConfig() {
        return new JsonPathConfig();
    }

    public static enum NumberReturnType {
        FLOAT_AND_DOUBLE, BIG_DECIMAL
    }
}
