package io.restassured.module.webtestclient.setup;

import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * @author Olga Maciaszek-Sharma
 */
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
