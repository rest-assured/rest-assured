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

package io.restassured.module.mockmvc.config;

import io.restassured.config.Config;
import io.restassured.config.ParamConfig;

import static io.restassured.config.ParamConfig.UpdateStrategy.MERGE;
import static io.restassured.config.ParamConfig.UpdateStrategy.REPLACE;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;

/**
 * Determines how different parameter types in REST Assured MockMvc should be updated when adding multiple parameters
 * of the same type with the same name.
 */
public class MockMvcParamConfig extends ParamConfig implements Config {

    private final boolean userConfigured;
    private final UpdateStrategy queryParamsUpdateStrategy;
    private final UpdateStrategy formParamsUpdateStrategy;
    private final UpdateStrategy requestParameterUpdateStrategy;
    private final UpdateStrategy attributeUpdateStrategy;
    private final UpdateStrategy sessionUpdateStrategy;

    /**
     * Create a new instance where all parameters are merged
     */
    public MockMvcParamConfig() {
        this(MERGE, MERGE, MERGE, MERGE, MERGE, false);
    }

    /**
     * Create a new instance and specify update strategies for all parameter types.
     *
     * @param queryParamsUpdateStrategy      The update strategy for query parameters
     * @param formParamsUpdateStrategy       The update strategy for form parameters
     * @param requestParameterUpdateStrategy The update strategy for request parameters
     * @param sessionUpdateStrategy          The update strategy for session parameters
     */
    public MockMvcParamConfig(UpdateStrategy queryParamsUpdateStrategy,
                              UpdateStrategy formParamsUpdateStrategy,
                              UpdateStrategy requestParameterUpdateStrategy,
                              UpdateStrategy attributeUpdateStrategy,
                              UpdateStrategy sessionUpdateStrategy) {
        this(queryParamsUpdateStrategy, formParamsUpdateStrategy, requestParameterUpdateStrategy, attributeUpdateStrategy, sessionUpdateStrategy, true);
    }

    private MockMvcParamConfig(UpdateStrategy queryParamsUpdateStrategy, UpdateStrategy formParamsUpdateStrategy,
                               UpdateStrategy requestParameterUpdateStrategy, UpdateStrategy attributeUpdateStrategy,
                               UpdateStrategy sessionUpdateStrategy, boolean userConfigured) {
        notNull(queryParamsUpdateStrategy, "Query param update strategy");
        notNull(requestParameterUpdateStrategy, "Request param update strategy");
        notNull(formParamsUpdateStrategy, "Form param update strategy");
        notNull(attributeUpdateStrategy, "Attribute update strategy");
        notNull(sessionUpdateStrategy, "Session update strategy");
        this.queryParamsUpdateStrategy = queryParamsUpdateStrategy;
        this.formParamsUpdateStrategy = formParamsUpdateStrategy;
        this.requestParameterUpdateStrategy = requestParameterUpdateStrategy;
        this.attributeUpdateStrategy = attributeUpdateStrategy;
        this.userConfigured = userConfigured;
        this.sessionUpdateStrategy = sessionUpdateStrategy;
    }

    /**
     * Merge all parameter types.
     *
     * @return A new instance of {@link MockMvcParamConfig}
     */
    public MockMvcParamConfig mergeAllParameters() {
        return new MockMvcParamConfig(MERGE, MERGE, MERGE, MERGE, MERGE, true);
    }

    /**
     * Replace parameter values for all kinds of parameter types.
     *
     * @return A new instance of {@link MockMvcParamConfig}
     */
    public MockMvcParamConfig replaceAllParameters() {
        return new MockMvcParamConfig(REPLACE, REPLACE, REPLACE, REPLACE, REPLACE, true);
    }

    /**
     * Set form parameter update strategy to the given value.
     *
     * @param updateStrategy The update strategy to use for form parameters
     * @return A new instance of {@link MockMvcParamConfig}
     */
    public MockMvcParamConfig formParamsUpdateStrategy(UpdateStrategy updateStrategy) {
        return new MockMvcParamConfig(queryParamsUpdateStrategy, updateStrategy, requestParameterUpdateStrategy, attributeUpdateStrategy, sessionUpdateStrategy, true);
    }

    /**
     * Set request parameter update strategy to the given value.
     * A "request parameter" is a parameter that will turn into a form or query parameter depending on the request. For example:
     * <p>
     * given().param("name", "value").when().get("/x"). ..
     * </p>
     *
     * @param updateStrategy The update strategy to use for request parameters
     * @return A new instance of {@link MockMvcParamConfig}
     */
    public MockMvcParamConfig requestParamsUpdateStrategy(UpdateStrategy updateStrategy) {
        return new MockMvcParamConfig(queryParamsUpdateStrategy, formParamsUpdateStrategy, updateStrategy, attributeUpdateStrategy, sessionUpdateStrategy, true);
    }

    /**
     * Set query parameter update strategy to the given value.
     *
     * @param updateStrategy The update strategy to use for query parameters
     * @return A new instance of {@link MockMvcParamConfig}
     */
    public MockMvcParamConfig queryParamsUpdateStrategy(UpdateStrategy updateStrategy) {
        return new MockMvcParamConfig(updateStrategy, formParamsUpdateStrategy, requestParameterUpdateStrategy, attributeUpdateStrategy, sessionUpdateStrategy, true);
    }

    /**
     * Set attribute update strategy to the given value.
     *
     * @param updateStrategy The update strategy to use for attribute parameters
     * @return A new instance of {@link MockMvcParamConfig}
     */
    public MockMvcParamConfig attributeUpdateStrategy(UpdateStrategy updateStrategy) {
        return new MockMvcParamConfig(queryParamsUpdateStrategy, formParamsUpdateStrategy, requestParameterUpdateStrategy, updateStrategy, sessionUpdateStrategy, true);
    }

    /**
     * Set session parameter update strategy to the given value.
     *
     * @param updateStrategy The update strategy to use for session parameters
     * @return A new instance of {@link MockMvcParamConfig}
     */
    public MockMvcParamConfig sessionAttributesUpdateStrategy(UpdateStrategy updateStrategy) {
        return new MockMvcParamConfig(queryParamsUpdateStrategy, formParamsUpdateStrategy, requestParameterUpdateStrategy, attributeUpdateStrategy, updateStrategy, true);
    }

    /**
     * @return The update strategy for form parameters
     */
    public UpdateStrategy attributeUpdateStrategy() {
        return attributeUpdateStrategy;
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
     * @return The update strategy for query parameters
     */
    public UpdateStrategy sessionAttributesUpdateStrategy() {
        return sessionUpdateStrategy;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUserConfigured() {
        return userConfigured;
    }

    /**
     * @return A static way to create a new MockMvcParamConfig instance without calling "new" explicitly. Mainly for syntactic sugar.
     */
    public static MockMvcParamConfig paramConfig() {
        return new MockMvcParamConfig();
    }

    /**
     * Syntactic sugar.
     *
     * @return The same MockMvcParamConfig instance.
     */
    public MockMvcParamConfig and() {
        return this;
    }

    /**
     * Syntactic sugar.
     *
     * @return The same MockMvcParamConfig instance.
     */
    public MockMvcParamConfig with() {
        return this;
    }
}
