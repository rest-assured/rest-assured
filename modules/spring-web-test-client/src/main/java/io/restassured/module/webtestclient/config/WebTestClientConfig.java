package io.restassured.module.webtestclient.config;

import io.restassured.config.Config;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebTestClientConfig implements Config {

	private final boolean userConfigured;
	private final boolean automaticallyApplySpringSecurityFilters;

	public WebTestClientConfig() {
		this(false, true);
	}

	public WebTestClientConfig(boolean userConfigured, boolean automaticallyApplySpringSecurityFilters) {
		this.userConfigured = userConfigured;
		this.automaticallyApplySpringSecurityFilters = automaticallyApplySpringSecurityFilters;
	}

	@Override
	public boolean isUserConfigured() {
		return userConfigured;
	}

	public boolean shouldAutomaticallyApplySpringSecurityFilters() {
		return automaticallyApplySpringSecurityFilters;
	}
}
