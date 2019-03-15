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
package io.restassured.module.webtestclient.internal;

import io.restassured.filter.time.TimingFilter;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.log.LogRepository;
import io.restassured.module.spring.commons.config.ConfigConverter;
import io.restassured.module.webtestclient.config.RestAssuredWebTestClientConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

class ExchangeResultConverter {

    WebTestClientRestAssuredResponseImpl toRestAssuredResponse(FluxExchangeResult<byte[]> result,
                                                               WebTestClient.ResponseSpec responseSpec,
                                                               long responseTime, LogRepository logRepository,
                                                               RestAssuredWebTestClientConfig config,
                                                               Consumer<EntityExchangeResult<byte[]>> consumer,
                                                               ResponseParserRegistrar responseParserRegistrar) {
        WebTestClientRestAssuredResponseImpl restAssuredResponse = new WebTestClientRestAssuredResponseImpl(responseSpec,
                logRepository);
        restAssuredResponse.setConfig(ConfigConverter.convertToRestAssuredConfig(config));
        byte[] responseBodyContent = ofNullable(consumer).map(theConsumer ->
                responseSpec.expectBody().consumeWith(theConsumer).returnResult().getResponseBodyContent())
                .orElseGet(() -> responseSpec.expectBody().returnResult().getResponseBodyContent());
        restAssuredResponse.setContent(responseBodyContent);
        MediaType contentType = result.getResponseHeaders().getContentType();
        restAssuredResponse.setContentType(ofNullable(contentType).map(MimeType::toString).orElse(null));
        restAssuredResponse.setHasExpectations(false);
        restAssuredResponse.setStatusCode(result.getStatus().value());
        List<Header> responseHeaders = assembleHeaders(result.getResponseHeaders());
        restAssuredResponse.setResponseHeaders(new Headers(responseHeaders));
        restAssuredResponse.setRpr(responseParserRegistrar);
        restAssuredResponse.setStatusLine(buildResultString(result.getStatus()));
        restAssuredResponse.setFilterContextProperties(new HashMap<Object, Object>() {{
            put(TimingFilter.RESPONSE_TIME_MILLISECONDS, responseTime);
        }});
        restAssuredResponse.setCookies(convertCookies(result.getResponseCookies()));
        return restAssuredResponse;
    }

    private List<Header> assembleHeaders(HttpHeaders headers) {
        return headers.keySet().stream()
                .map(headerName -> headers.get(headerName).stream()
                        .map(headerValue -> new Header(headerName, headerValue))
                        .collect(Collectors.toList())).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private String buildResultString(HttpStatus status) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(status.value());
        if (status.isError() && isNotBlank(status.getReasonPhrase())) {
            stringBuilder.append(": ");
            stringBuilder.append(status.getReasonPhrase());
        }
        return stringBuilder.toString();
    }

    private Cookies convertCookies(MultiValueMap<String, ResponseCookie> responseCookies) {
        List<Cookie> cookies = responseCookies.keySet().stream().map(cookie -> responseCookies.get(cookie).stream()
                .map(responseCookie -> new Cookie.Builder(responseCookie.getName(), responseCookie.getValue()).build())
                .collect(Collectors.toList())
        ).flatMap(Collection::stream).collect(Collectors.toList());
        return new Cookies(cookies);
    }

}
