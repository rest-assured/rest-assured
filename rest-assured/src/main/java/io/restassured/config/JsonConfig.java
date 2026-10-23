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

import io.restassured.common.config.NumberReturnType;
import org.apache.commons.lang3.Validate;

/**
 * Allows you to configure properties of JSON parsing.
 */
public class JsonConfig implements Config {

    /**
     * The default maximum length (in characters) of a JSON number token, the same as Jackson's
     * {@code StreamReadConstraints} default.
     */
    public static final int DEFAULT_NUMBER_LENGTH_LIMIT = 1000;
    private final NumberReturnType numberReturnType;
    private final int numberLengthLimit;
    private final JsonPathEngine pathEngine;
    private final boolean isUserDefined;

    /**
     * Create a new instance of JsonConfig with the default settings.
     */
    public JsonConfig() {
        this(NumberReturnType.FLOAT_AND_DOUBLE, DEFAULT_NUMBER_LENGTH_LIMIT, false);
    }

    public JsonConfig(NumberReturnType numberReturnType) {
        this(numberReturnType, DEFAULT_NUMBER_LENGTH_LIMIT, true);
    }

    public JsonConfig(NumberReturnType numberReturnType, boolean isUserDefined) {
        this(numberReturnType, DEFAULT_NUMBER_LENGTH_LIMIT, isUserDefined);
    }

    private JsonConfig(NumberReturnType numberReturnType, int numberLengthLimit, boolean isUserDefined) {
        this(numberReturnType, numberLengthLimit, null, isUserDefined);
    }

    private JsonConfig(NumberReturnType numberReturnType, int numberLengthLimit, JsonPathEngine pathEngine, boolean isUserDefined) {
        Validate.notNull(numberReturnType, "numberReturnType cannot be null");
        this.numberReturnType = numberReturnType;
        this.numberLengthLimit = numberLengthLimit;
        this.pathEngine = pathEngine;
        this.isUserDefined = isUserDefined;
    }

    public NumberReturnType numberReturnType() {
        return numberReturnType;
    }

    public boolean shouldRepresentJsonNumbersAsBigDecimal() {
        return numberReturnType() == NumberReturnType.BIG_DECIMAL;
    }

    /**
     * @return The maximum allowed length (in characters) of a JSON number token, or a negative value if unlimited.
     */
    public int numberLengthLimit() {
        return numberLengthLimit;
    }

    /**
     * Specifies if JSON parsing should use floats and doubles or BigDecimals to represent Json numbers.
     *
     * @param numberReturnType The choice.
     * @return A new instance of JsonConfig with the given configuration
     */
    public JsonConfig numberReturnType(NumberReturnType numberReturnType) {
        return new JsonConfig(numberReturnType, numberLengthLimit, pathEngine, true);
    }

    /**
     * Specifies the maximum allowed length (in characters) of a JSON number token. Number tokens longer than this
     * are rejected before being converted to a number, guarding against CPU/heap exhaustion when constructing
     * arbitrarily large numbers (such as {@link java.math.BigInteger}) from untrusted JSON. A value of {@code 0}
     * rejects every number, and a negative value disables the check. Defaults to {@link #DEFAULT_NUMBER_LENGTH_LIMIT}.
     *
     * @param numberLengthLimit The maximum number token length, {@code 0} to reject all numbers, or a negative value to disable the check.
     * @return A new instance of JsonConfig with the given configuration
     */
    public JsonConfig numberLengthLimit(int numberLengthLimit) {
        return new JsonConfig(numberReturnType, numberLengthLimit, pathEngine, true);
    }

    /**
     * Specifies the engine that evaluates JSON paths, in {@code body(..)} expectations, {@code path(..)} and
     * {@code extract().path(..)}. It doesn't change {@code jsonPath()}, which always uses GPath. When no engine is
     * specified (the default), REST Assured uses GPath ({@code io.rest-assured:json-path-gpath}) if it's on the classpath,
     * and otherwise Jayway JsonPath ({@code io.rest-assured:json-path-jayway}). When both are on the classpath, it logs
     * once that it uses GPath.
     *
     * @param pathEngine The engine, or {@code null} to let REST Assured pick it.
     * @return A new instance of JsonConfig with the given configuration
     */
    public JsonConfig pathEngine(JsonPathEngine pathEngine) {
        return new JsonConfig(numberReturnType, numberLengthLimit, pathEngine, true);
    }

    /**
     * @return The engine that evaluates JSON paths, or {@code null} if REST Assured picks it.
     */
    public JsonPathEngine pathEngine() {
        return pathEngine;
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

