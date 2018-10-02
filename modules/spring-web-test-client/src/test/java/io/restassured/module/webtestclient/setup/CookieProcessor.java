package io.restassured.module.webtestclient.setup;

import java.util.Collection;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

public class CookieProcessor {

	public Mono<ServerResponse> processCookies(ServerRequest request) {
		MultiValueMap processedResponseCookies = new LinkedMultiValueMap();
		request.queryParams().keySet()
				.stream()
				.map(queryParamName -> request.queryParams().get(queryParamName).stream()
						.map(queryParam -> ResponseCookie.from(queryParamName, queryParam).build())
						.collect(Collectors.toList()))
				.flatMap(Collection::stream)
				.forEach(processedCookie -> processedResponseCookies.add(processedCookie.getName(), processedCookie));

		return ServerResponse.ok().cookies(
				cookies -> cookies.addAll(processedResponseCookies)).contentType(MediaType.APPLICATION_JSON).build();

	}
}
