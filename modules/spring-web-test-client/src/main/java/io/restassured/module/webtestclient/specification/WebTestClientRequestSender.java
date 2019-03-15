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

package io.restassured.module.webtestclient.specification;

import io.restassured.http.Method;
import io.restassured.module.webtestclient.response.WebTestClientResponse;
import io.restassured.specification.RequestSenderOptions;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

/*
As the result of evaluating the {@link Function<UriBuilder, URI>} will not be known while sending the request, path and
parameters information will not be logged for the methods that take uriFunctions and not path and parameters
as arguments.
 */
public interface WebTestClientRequestSender extends RequestSenderOptions<WebTestClientResponse> {

	/**
	 * Perform a GET request to a uri obtained from a {@link Function<>} that uses {@link UriBuilder}
	 * to generate {@link URI}.
	 *
	 * @param uriFunction The function that will be used for evaluating the URI.
	 * @return The response of the request.
	 */
	WebTestClientResponse get(Function<UriBuilder, URI> uriFunction);

	/**
	 * Perform a POST request to a uri obtained from a {@link Function<>} that uses {@link UriBuilder}
	 * to generate {@link URI}.
	 *
	 * @param uriFunction The function that will be used for evaluating the URI.
	 * @return The response of the request.
	 */
	WebTestClientResponse post(Function<UriBuilder, URI> uriFunction);

	/**
	 * * Perform a PUT request to a uri obtained from a {@link Function<>} that uses {@link UriBuilder}
	 * 	 to generate {@link URI}.
	 *
	 * @param uriFunction The function that will be used for evaluating the URI.
	 * @return The response of the request.
	 */
	WebTestClientResponse put(Function<UriBuilder, URI> uriFunction);

	/**
	 * * Perform a DELETE request to a uri obtained from a {@link Function<>} that uses {@link UriBuilder}
	 * 	 to generate {@link URI}.
	 *
	 * @param uriFunction The function that will be used for evaluating the URI.
	 * @return The response of the request.
	 */
	WebTestClientResponse delete(Function<UriBuilder, URI> uriFunction);

	/**
	 * * Perform a PATCH request to a uri obtained from a {@link Function<>} that uses {@link UriBuilder}
	 * 	 to generate {@link URI}.
	 *
	 * @param uriFunction The function that will be used for evaluating the URI.
	 * @return The response of the request.
	 */
	WebTestClientResponse patch(Function<UriBuilder, URI> uriFunction);

	/**
	 * * Perform a HEAD request to a uri obtained from a {@link Function<>} that uses {@link UriBuilder}
	 * 	 to generate {@link URI}.
	 *
	 * @param uriFunction The function that will be used for evaluating the URI.
	 * @return The response of the request.
	 */
	WebTestClientResponse head(Function<UriBuilder, URI> uriFunction);

	/**
	 * * Perform a OPTIONS request to a uri obtained from a {@link Function<>} that uses {@link UriBuilder}
	 * 	 to generate {@link URI}.
	 *
	 * @param uriFunction The function that will be used for evaluating the URI.
	 * @return The response of the request.
	 */
	WebTestClientResponse options(Function<UriBuilder, URI> uriFunction);

	/**
	 * * Perform a request to a uri obtained from a {@link Function<>} that uses {@link UriBuilder}
	 * 	 to generate {@link URI}.
	 *
	 * @param method The HTTP method to use while sending the request expressed as {@link Method}
	 * @param uriFunction The function that will be used for evaluating the URI.
	 * @return The response of the request.
	 */
	WebTestClientResponse request(Method method, Function<UriBuilder, URI> uriFunction);

	/**
	 * * Perform a request to a uri obtained from a {@link Function<>} that uses {@link UriBuilder}
	 * 	 to generate {@link URI}.
	 *
	 * @param method The HTTP method to use while sending the request expressed as {@link String}
	 * @param uriFunction The function that will be used for evaluating the URI.
	 * @return The response of the request.
	 */
	WebTestClientResponse request(String method, Function<UriBuilder, URI> uriFunction);

	/**
	 * Specify a {@link Consumer} to process the request result. The consumer will be applied before processing the result
	 * into a {@link WebTestClientResponse} and before extracting the response body content as byte array, which closes
	 * the stream.
	 *
	 * This is very useful, for example for extracting Spring Rest Docs.
	 *
	 * Usage example:
	 * <p/>
	 * <pre>
	 *RestAssuredWebTestClient.given()
	 * 				.standaloneSetup(new GreetingController(), documentationConfiguration(restDocumentation))
	 * 				.queryParam("name", "John")
	 * 				.when()
	 * 				.consumeWith(document("greeting",
	 * 						pathParameters(
	 * 								parameterWithName("path").description("The path to greeting")),
	 * 						responseFields(
	 * 								fieldWithPath("id").description("The ID of the greeting"),
	 * 								fieldWithPath("content").description("The content of the greeting"))
	 * 				))
	 * 				.get("/{path}", "greeting")
	 * </pre>
	 *
	 * @param consumer to be applied on the exchange result.
	 * @return a {@link WebTestClientRequestSender} instance.
	 */
	WebTestClientRequestSender consumeWith(Consumer<EntityExchangeResult<byte[]>> consumer);
}
