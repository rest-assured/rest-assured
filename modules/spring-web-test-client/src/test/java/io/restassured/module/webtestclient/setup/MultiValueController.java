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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class MultiValueController {

    @RequestMapping(value = "/multiValueParam", method = {POST, GET}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> multiValueParam(@RequestParam("list") List<String> listValues) {
        return Mono.just("{ \"list\" : \"" + String.join(",", listValues) + "\" }");
    }

    @PostMapping(value = "/threeMultiValueParam", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> threeMultiValueParam(@RequestParam("list") List<String> list1Values,
                                             @RequestParam("list2") List<String> list2Values,
                                             @RequestParam("list3") List<String> list3Values) {
        return Mono.just("{ \"list\" : \"" + String.join(",", list1Values) + "\"," +
                " \"list2\" : \"" + String.join(",", list2Values) + "\", " +
                " \"list3\" : \"" + String.join(",", list3Values) + "\" }");
    }
}