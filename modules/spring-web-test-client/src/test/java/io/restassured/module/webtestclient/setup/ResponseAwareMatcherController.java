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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class ResponseAwareMatcherController {

    @GetMapping(value = "/responseAware", produces = APPLICATION_JSON_VALUE)
    public Mono<String> parserWithUnknownContentType() {
        return Mono.just("{\n" +
                "        \"playerOneId\":\"a084a81a-6bc9-418d-b107-5cb5ce249b77\",\n" +
                "            \"playerTwoId\":\"88867e23-0b38-4c43-ad8e-161ba5062c7d\",\n" +
                "            \"status\":\"ongoing\",\n" +
                "            \"rounds\":[\n" +
                "\n" +
                "        ],\n" +
                "        \"_links\":{\n" +
                "        \"self\":{\n" +
                "            \"href\":\"http://localhost:8080/2dd68f2d-37df-4eed-9fce-5d9ce23a6745\"\n" +
                "        },\n" +
                "        \"make-move\":{\n" +
                "            \"href\":\"http://localhost:8080/2dd68f2d-37df-4eed-9fce-5d9ce23a6745/make-move\"\n" +
                "        }\n" +
                "    },\n" +
                "        \"id\":\"2dd68f2d-37df-4eed-9fce-5d9ce23a6745\"\n" +
                "    }");
    }
}
