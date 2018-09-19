package io.restassured.module.webtestclient.config;

import io.restassured.config.ParamConfig;

import static io.restassured.config.ParamConfig.UpdateStrategy.MERGE;
import static io.restassured.internal.assertion.AssertParameter.notNull;

/**
 * @author Olga Maciaszek-Sharma
 */
// TODO: extract duplicates
public class WebTestClientParamConfig extends ParamConfig {

	private final boolean userConfigured;
	private final UpdateStrategy queryParamsUpdateStrategy;
	private final UpdateStrategy formParamsUpdateStrategy;
	private final UpdateStrategy requestParameterUpdateStrategy;
	private final UpdateStrategy attributeUpdateStrategy;
	private final UpdateStrategy sessionUpdateStrategy;


	public WebTestClientParamConfig() {
		this(MERGE, MERGE, MERGE, MERGE, MERGE, false);
	}

	public WebTestClientParamConfig(UpdateStrategy queryParamsUpdateStrategy, UpdateStrategy formParamsUpdateStrategy,
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
		this.sessionUpdateStrategy = sessionUpdateStrategy;
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
	 * @return The update strategy for query parameters
	 */
	public UpdateStrategy sessionAttributesUpdateStrategy() {
		return sessionUpdateStrategy;
	}

}
