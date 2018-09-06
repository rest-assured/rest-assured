package io.restassured.module.webtestclient.internal;

import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * @author Olga Maciaszek-Sharma
 */
public interface WebTestClientFactory {

	WebTestClient build();

}
