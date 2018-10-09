package io.restassured.module.webtestclient.config;

import io.restassured.config.Config;
import io.restassured.module.spring.commons.config.ClientConfig;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebTestClientConfig implements ClientConfig, Config {

	private final boolean userConfigured;

	public WebTestClientConfig() {
		this(true);
	}

	public WebTestClientConfig(boolean userConfigured) {
		this.userConfigured = userConfigured;
	}

	@Override
	public boolean isUserConfigured() {
		return userConfigured;
	}

	/**
	 * Just syntactic sugar.
	 *
	 * @return A new instance of {@link WebTestClientConfig}.
	 */
	public static WebTestClientConfig webTestClientConfig() {
		return new WebTestClientConfig();
	}

	/**
	 * Just syntactic sugar to make the DSL more English-like.
	 */
	public WebTestClientConfig with() {
		return this;
	}
}
