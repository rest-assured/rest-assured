package io.restassured.module.webtestclient.setup;

import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author Olga Maciaszek-Sharma
 */
public class QueryParamsProcessor {

	private static final String template = "Hello, %s!";

	public Mono<ServerResponse> processQueryParams(ServerRequest serverRequest) {
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).
				syncBody("{ \"name\" : \"" + String.format(template,
						serverRequest.queryParams().getFirst("name")) + "\"," +
						" \"message\" : \"" + serverRequest.queryParams().getFirst("message") + "\", " +
						" \"_link\" : \"" + serverRequest.uri() + "\" }");
	}
}
