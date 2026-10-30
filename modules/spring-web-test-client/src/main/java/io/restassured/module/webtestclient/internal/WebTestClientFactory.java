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
package io.restassured.module.webtestclient.internal;

import io.restassured.module.webtestclient.config.WebTestClientConfig;
import org.springframework.test.web.reactive.server.WebTestClient;

public interface WebTestClientFactory {

	String NOT_CONFIGURED = "You haven't configured a WebTestClient instance. You can do this statically\n\nRestAssuredWebTestClient.webTestClient(..)\n" +
			"RestAssuredWebTestClient.standaloneSetup(..);\nRestAssuredWebTestClient.webAppContextSetup(..);\n\nor using the DSL:\n\n" +
			"given().\n\t\twebTestClient(..). ..\n\nor, for a specification built by WebTestClientRequestSpecBuilder that is sent on its own, " +
			"on the builder:\n\nnew WebTestClientRequestSpecBuilder().\n\t\tsetWebTestClient(..). ..\n";

	/**
	 * Builds and returns a {@link WebTestClient} instance based on provided configuration
	 * @param webTestClientConfig configuration to be used during WebTestClient instantiation
	 * @return a WebTestClient instance
	 */
	WebTestClient build(WebTestClientConfig webTestClientConfig);

	/**
	 * Checks if underlying {@link WebTestClient} or {@link org.springframework.test.web.reactive.server.WebTestClient.Builder}
	 * is not null
	 * @return verification result
	 */
	boolean isAssigned();
}
