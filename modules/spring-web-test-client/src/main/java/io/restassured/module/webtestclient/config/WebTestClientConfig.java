package io.restassured.module.webtestclient.config;

import io.restassured.config.Config;
import io.restassured.module.spring.commons.config.ClientConfig;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebTestClientConfig implements ClientConfig, Config {

	private final boolean userConfigured;

	public WebTestClientConfig() {
		this(false, true);
	}

	public WebTestClientConfig(boolean userConfigured, boolean automaticallyApplySpringSecurityFilters) {
		this.userConfigured = userConfigured;
	}

	@Override
	public boolean isUserConfigured() {
		return userConfigured;
	}

}
