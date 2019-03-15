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

package io.restassured.module.mockmvc.http;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * This controller has been copied from <a href="http://spring.io/guides/gs/rest-service/">Spring Guides</a>.
 */
@Controller
public class BasePathController {

    private static final String template = "Hello, %s!";

    @RequestMapping(value = "/my-path/greetingPath", method = GET)
    public @ResponseBody Greeting greeting(
            @RequestParam(value="name", required=false, defaultValue="World") String name) {
        return new Greeting(0, String.format(template, name));
    }

    @RequestMapping(value = "/", method = GET)
    public @ResponseBody Greeting greeting2(
            @RequestParam(value="name", required=false, defaultValue="World") String name) {
        return new Greeting(1, String.format(template, name));
    }
}