/*
 * Copyright 2016 the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class HeaderController {

    @GetMapping(value = "/header", produces = APPLICATION_JSON_VALUE)
    public Mono<String> header(@RequestHeader("headerName") String headerValue,
                               @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        return Mono.just("{\"headerName\" : \"" + headerValue + "\", \"user-agent\" : \"" + userAgent + "\"}");
    }
}
