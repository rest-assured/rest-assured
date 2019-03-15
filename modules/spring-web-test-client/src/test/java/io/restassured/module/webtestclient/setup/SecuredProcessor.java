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

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class SecuredProcessor {

	public Mono<ServerResponse> processSecuredRequest(ServerRequest serverRequest) {
		String auth = serverRequest.headers().header("Authorization").get(0);
		return ServerResponse.ok().syncBody(new AuthWrapper(auth));
	}
}

class AuthWrapper {

	public String auth;

	public AuthWrapper(String auth) {
		this.auth = auth;
	}
}
