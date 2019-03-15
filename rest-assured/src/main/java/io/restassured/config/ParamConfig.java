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

import io.restassured.internal.common.assertion.AssertParameter;

import static io.restassured.config.ParamConfig.UpdateStrategy.MERGE;
import static io.restassured.config.ParamConfig.UpdateStrategy.REPLACE;

/**
 * Param config determines how different parameter types in REST Assured should be updated when adding multiple parameters
 * of the same type with the same name.
 */
public class ParamConfig implements Config {

    private final boolean userConfigured;
    private final UpdateStrategy queryParamsUpdateStrategy;
    private final UpdateStrategy formParamsUpdateStrategy;
    private final UpdateStrategy requestParameterUpdateStrategy;

    /**
     * Create a new instance where all parameters are merged
     */
    public ParamConfig() {
        this(MERGE, MERGE, MERGE, false);
    }

    /**
     * Create a new instance and specify update strategies for all parameter types.
     *
     * @param queryParamsUpdateStrategy      The update strategy for query parameters
     * @param formParamsUpdateStrategy       The update strategy for form parameters
     * @param requestParameterUpdateStrategy The update strategy for request parameters
     */
    public ParamConfig(UpdateStrategy queryParamsUpdateStrategy,
                       UpdateStrategy formParamsUpdateStrategy,
                       UpdateStrategy requestParameterUpdateStrategy) {
        this(queryParamsUpdateStrategy, formParamsUpdateStrategy, requestParameterUpdateStrategy, true);
    }

    private ParamConfig(UpdateStrategy queryParamsUpdateStrategy, UpdateStrategy formParamsUpdateStrategy,
                        UpdateStrategy requestParameterUpdateStrategy, boolean userConfigured) {
        AssertParameter.notNull(queryParamsUpdateStrategy, "Query param update strategy");
        AssertParameter.notNull(requestParameterUpdateStrategy, "Request param update strategy");
        AssertParameter.notNull(formParamsUpdateStrategy, "Form param update strategy");
        this.queryParamsUpdateStrategy = queryParamsUpdateStrategy;
        this.formParamsUpdateStrategy = formParamsUpdateStrategy;
        this.requestParameterUpdateStrategy = requestParameterUpdateStrategy;
        this.userConfigured = userConfigured;
    }

    /**
     * Merge all parameter types.
     *
     * @return A new instance of {@link ParamConfig}
     */
    public ParamConfig mergeAllParameters() {
        return new ParamConfig(MERGE, MERGE, MERGE, true);
    }

    /**
     * Replace parameter values for all kinds of parameter types.
     *
     * @return A new instance of {@link ParamConfig}
     */
    public ParamConfig replaceAllParameters() {
        return new ParamConfig(REPLACE, REPLACE, REPLACE, true);
    }

    /**
     * Set form parameter update strategy to the given value.
     *
     * @param updateStrategy The update strategy to use for form parameters
     * @return A new instance of {@link ParamConfig}
     */
    public ParamConfig formParamsUpdateStrategy(UpdateStrategy updateStrategy) {
        return new ParamConfig(queryParamsUpdateStrategy, updateStrategy, requestParameterUpdateStrategy, true);
    }

    /**
     * Set request parameter update strategy to the given value.
     * A "request parameter" is a parameter that will turn into a form or query parameter depending on the request. For example:
     * <p>
     * given().param("name", "value").when().get("/x"). ..
     * </p>
     *
     * @param updateStrategy The update strategy to use for request parameters
     * @return A new instance of {@link ParamConfig}
     */
    public ParamConfig requestParamsUpdateStrategy(UpdateStrategy updateStrategy) {
        return new ParamConfig(queryParamsUpdateStrategy, formParamsUpdateStrategy, updateStrategy, true);
    }

    /**
     * Set query parameter update strategy to the given value.
     *
     * @param updateStrategy The update strategy to use for query parameters
     * @return A new instance of {@link ParamConfig}
     */
    public ParamConfig queryParamsUpdateStrategy(UpdateStrategy updateStrategy) {
        return new ParamConfig(updateStrategy, formParamsUpdateStrategy, requestParameterUpdateStrategy, true);
    }

    /**
     * @return The update strategy for form parameters
     */
    public UpdateStrategy formParamsUpdateStrategy() {
        return formParamsUpdateStrategy;
    }

    /**
     * @return The update strategy for request parameters
     */
    public UpdateStrategy requestParamsUpdateStrategy() {
        return requestParameterUpdateStrategy;
    }

    /**
     * @return The update strategy for query parameters
     */
    public UpdateStrategy queryParamsUpdateStrategy() {
        return queryParamsUpdateStrategy;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUserConfigured() {
        return userConfigured;
    }

    /**
     * The update strategy to use for a parameter type
     */
    public enum UpdateStrategy {
        /**
         * Parameters with the same name is merged.
         */
        MERGE,
        /**
         * Parameters with the same name is replaced with the latest applied value.
         */
        REPLACE
    }


    /**
     * @return A static way to create a new ParamConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static ParamConfig paramConfig() {
        return new ParamConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same ParamConfig instance.
     */
    public ParamConfig and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same ParamConfig instance.
     */
    public ParamConfig with() {
        return this;
    }
}
