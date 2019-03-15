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
package io.restassured.module.webtestclient.setup;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class QueryParamsProcessor {

	private static final String template = "Hello, %s!";

	public Mono<ServerResponse> processQueryParams(ServerRequest serverRequest) {
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.header("test", "test")
				.syncBody("{ \"name\" : \"" + String.format(template,
						serverRequest.queryParams().getFirst("name")) + "\"," +
						" \"message\" : \"" + serverRequest.queryParams().getFirst("message") + "\", " +
						" \"_link\" : \"" + serverRequest.uri() + "\" }");
	}
}
