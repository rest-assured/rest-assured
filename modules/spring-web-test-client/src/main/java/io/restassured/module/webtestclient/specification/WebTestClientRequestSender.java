/*
 * Copyright 2018 the original author or authors.
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

package io.restassured.module.webtestclient.specification;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import io.restassured.http.Method;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.specification.RequestSenderOptions;

import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.web.util.UriBuilder;

/**
 * @author Olga Maciaszek-Sharma
 */
public interface WebTestClientRequestSender extends RequestSenderOptions<WebTestClientResponse> {

	WebTestClientResponse get(Function<UriBuilder, URI> uriFunction);

	WebTestClientResponse post(Function<UriBuilder, URI> uriFunction);

	WebTestClientResponse put(Function<UriBuilder, URI> uriFunction);

	WebTestClientResponse delete(Function<UriBuilder, URI> uriFunction);

	WebTestClientResponse patch(Function<UriBuilder, URI> uriFunction);

	WebTestClientResponse head(Function<UriBuilder, URI> uriFunction);

	WebTestClientResponse options(Function<UriBuilder, URI> uriFunction);

	WebTestClientResponse request(Method method, Function<UriBuilder, URI> uriFunction);

	WebTestClientResponse request(String method, Function<UriBuilder, URI> uriFunction);

	// TODO: add usage example
	WebTestClientRequestSender consumeWith(Consumer<EntityExchangeResult<byte[]>> consumer);
}
