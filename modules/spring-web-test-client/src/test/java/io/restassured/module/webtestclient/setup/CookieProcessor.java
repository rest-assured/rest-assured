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
import org.springframework.http.ResponseCookie;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
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
