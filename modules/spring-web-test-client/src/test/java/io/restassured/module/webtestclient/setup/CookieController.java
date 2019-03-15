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

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class CookieController {

    @RequestMapping(value = "/cookie", produces = APPLICATION_JSON_VALUE)
    public Mono<String> cookie(@CookieValue("cookieName1") String cookieValue1, @CookieValue(value = "cookieName2", required = false) String cookieValue2) {
        return Mono.just("{\"cookieValue1\" : \"" + cookieValue1 + "\", \"cookieValue2\" : \"" + cookieValue2 + "\"}");
    }
}
