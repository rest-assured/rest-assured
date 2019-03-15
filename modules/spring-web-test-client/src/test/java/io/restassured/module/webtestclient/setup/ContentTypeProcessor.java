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

import static org.springframework.web.reactive.function.BodyInserters.fromObject;

public class ContentTypeProcessor {

	public Mono<ServerResponse> processContentType(ServerRequest request) {
		return ServerResponse.ok()
				.body(fromObject(new RequestContentTypeWrapper(request.headers().contentType().orElse(MediaType.ALL)
						.toString())));
	}
}

class RequestContentTypeWrapper {

	public String requestContentType;

	public RequestContentTypeWrapper(String requestContentType) {
		this.requestContentType = requestContentType;
	}
}
