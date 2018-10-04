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

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class PutController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @PutMapping(value = "/greetingPut", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public Greeting greeting(@RequestParam("name") String name) {
        return new Greeting(counter.incrementAndGet(), String.format(template, name));
    }

    @PutMapping("/stringBody")
    public String stringBody(@RequestBody String body) {
        return body;
    }

    @PutMapping(value = "/jsonReflect", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String jsonReflect(@RequestBody String body) {
        return body;
    }
}