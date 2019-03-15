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

package io.restassured.module.webtestclient;

import io.restassured.module.webtestclient.matcher.RestAssuredWebTestClientMatchers;
import io.restassured.module.webtestclient.setup.ResponseAwareMatcherController;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

public class WebTestClientResponseAwareMatcherTest {

	@Test
	public void
	can_use_predefined_matcher_for_response_aware_matching() {
		RestAssuredWebTestClient.given()
				.standaloneSetup(new ResponseAwareMatcherController())
				.when()
				.get("/responseAware")
				.then()
				.statusCode(200)
				.body("_links.self.href", RestAssuredWebTestClientMatchers.endsWithPath("id"))
				.body("status", equalTo("ongoing"));
	}
}
