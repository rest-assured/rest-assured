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
package io.restassured.module.webtestclient.config;

import io.restassured.config.ParamConfig;

import static io.restassured.config.ParamConfig.UpdateStrategy.MERGE;
import static io.restassured.config.ParamConfig.UpdateStrategy.REPLACE;
import static io.restassured.internal.common.assertion.AssertParameter.notNull;

public class WebTestClientParamConfig extends ParamConfig {

	private final boolean userConfigured;
	private final UpdateStrategy queryParamsUpdateStrategy;
	private final UpdateStrategy formParamsUpdateStrategy;
	private final UpdateStrategy requestParameterUpdateStrategy;
	private final UpdateStrategy attributeUpdateStrategy;


	public WebTestClientParamConfig() {
		this(MERGE, MERGE, MERGE, MERGE, false);
	}

	public WebTestClientParamConfig(UpdateStrategy queryParamsUpdateStrategy, UpdateStrategy formParamsUpdateStrategy,
	                                UpdateStrategy requestParameterUpdateStrategy, UpdateStrategy attributeUpdateStrategy,
	                                boolean userConfigured) {
		notNull(queryParamsUpdateStrategy, "Query param update strategy");
		notNull(requestParameterUpdateStrategy, "Request param update strategy");
		notNull(formParamsUpdateStrategy, "Form param update strategy");
		notNull(attributeUpdateStrategy, "Attribute update strategy");
		this.queryParamsUpdateStrategy = queryParamsUpdateStrategy;
		this.formParamsUpdateStrategy = formParamsUpdateStrategy;
		this.requestParameterUpdateStrategy = requestParameterUpdateStrategy;
		this.attributeUpdateStrategy = attributeUpdateStrategy;
		this.userConfigured = userConfigured;
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
	 * {@inheritDoc}
	 */
	public boolean isUserConfigured() {
		return userConfigured;
	}

	/**
	 * @return A static way to create a new WebTestClientParamConfig instance without calling "new" explicitly.
	 * Mainly for syntactic sugar.
	 */
	public static WebTestClientParamConfig paramConfig() {
		return new WebTestClientParamConfig();
	}

	/**
	 * Merge all parameter types.
	 *
	 * @return A new instance of {@link WebTestClientParamConfig}
	 */
	public WebTestClientParamConfig mergeAllParameters() {
		return new WebTestClientParamConfig(MERGE, MERGE, MERGE, MERGE, true);
	}

	/**
	 * Replace parameter values for all kinds of parameter types.
	 *
	 * @return A new instance of {@link WebTestClientParamConfig}
	 */
	public WebTestClientParamConfig replaceAllParameters() {
		return new WebTestClientParamConfig(REPLACE, REPLACE, REPLACE, REPLACE, true);
	}

	/**
	 * Set form parameter update strategy to the given value.
	 *
	 * @param updateStrategy The update strategy to use for form parameters
	 * @return A new instance of {@link WebTestClientParamConfig}
	 */
	public WebTestClientParamConfig formParamsUpdateStrategy(UpdateStrategy updateStrategy) {
		return new WebTestClientParamConfig(queryParamsUpdateStrategy, updateStrategy,
				requestParameterUpdateStrategy, attributeUpdateStrategy, true);
	}

	/**
	 * Set request parameter update strategy to the given value.
	 * A "request parameter" is a parameter that will turn into a form or query parameter depending on the request. For example:
	 * <p>
	 * given().param("name", "value").when().get("/x"). ..
	 * </p>
	 *
	 * @param updateStrategy The update strategy to use for request parameters
	 * @return A new instance of {@link WebTestClientParamConfig}
	 */
	public WebTestClientParamConfig requestParamsUpdateStrategy(UpdateStrategy updateStrategy) {
		return new WebTestClientParamConfig(queryParamsUpdateStrategy, formParamsUpdateStrategy,
				updateStrategy, attributeUpdateStrategy, true);
	}


	/**
	 * Set query parameter update strategy to the given value.
	 *
	 * @param updateStrategy The update strategy to use for query parameters
	 * @return A new instance of {@link WebTestClientParamConfig}
	 */
	public WebTestClientParamConfig queryParamsUpdateStrategy(UpdateStrategy updateStrategy) {
		return new WebTestClientParamConfig(updateStrategy, formParamsUpdateStrategy,
				requestParameterUpdateStrategy, attributeUpdateStrategy, true);
	}

	/**
	 * Syntactic sugar.
	 *
	 * @return The same WebTestClientParamConfig instance.
	 */
	public WebTestClientParamConfig and() {
		return this;
	}

	/**
	 * Syntactic sugar.
	 *
	 * @return The same WebTestClientParamConfig instance.
	 */
	public WebTestClientParamConfig with() {
		return this;
	}

	/**
	 * Set attribute update strategy to the given value.
	 *
	 * @param updateStrategy The update strategy to use for attribute parameters
	 * @return A new instance of {@link WebTestClientParamConfig}
	 */
	public WebTestClientParamConfig attributeUpdateStrategy(UpdateStrategy updateStrategy) {
		return new WebTestClientParamConfig(queryParamsUpdateStrategy, formParamsUpdateStrategy,
				requestParameterUpdateStrategy, updateStrategy, true);
	}

}
