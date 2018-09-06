package io.restassured.internal;

import io.restassured.module.webtestclient.internal.WebTestClientFactory;

import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WrapperWebTestClientFactory implements WebTestClientFactory {

	private final WebTestClient webTestClient;

	public WrapperWebTestClientFactory(WebTestClient webTestClient) {
		this.webTestClient = webTestClient;
	}

	@Override
	public synchronized WebTestClient build() {
		return webTestClient;
	}
}
