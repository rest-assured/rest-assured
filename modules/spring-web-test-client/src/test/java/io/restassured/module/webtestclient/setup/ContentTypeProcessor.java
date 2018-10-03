package io.restassured.module.webtestclient.setup;

import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.BodyInserters.fromObject;

/**
 * @author Olga Maciaszek-Sharma
 */
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
