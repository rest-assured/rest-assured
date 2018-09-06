package io.restassured.module.webtestclient.internal;

import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Olga Maciaszek-Sharma
 */
public class BuilderBasedWebTestClientFactory implements WebTestClientFactory {

	private final WebTestClient.Builder builder;

	public BuilderBasedWebTestClientFactory(WebTestClient.Builder builder) {
		this.builder = builder;
	}

	@Override
	public synchronized WebTestClient build() {
		// TODO: setup configuration

		return builder.build();
	}
}
