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

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
public class SecuredController {

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping(value = "/springSecurityGreeting", method = GET)
    public @ResponseBody Greeting greeting(@RequestParam(value = "name", required = false, defaultValue = "World") String name) {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user == null || !user.getUsername().equals("authorized_user")) {
            throw new IllegalArgumentException("Not authorized");
        }

        return new Greeting(counter.incrementAndGet(), String.format(template, name));
    }

    @RequestMapping(value = "/principalGreeting", method = GET)
    public @ResponseBody Greeting greeting(@RequestParam(value = "name", required = false, defaultValue = "World") String name, Principal principal) {
        if (principal == null || !principal.getName().equals("authorized_user")) {
            throw new IllegalArgumentException("Not authorized");
        }

        return new Greeting(counter.incrementAndGet(), String.format(template, name));
    }

    @RequestMapping(value = "/setAuthenticationSetBoth", method = GET)
    public @ResponseBody Greeting setAuthenticationSetBoth(@RequestParam(value = "name", required = false, defaultValue = "World") String name, Principal principal) {
        if (!SecurityContextHolder.getContext().getAuthentication().equals(principal)) {
            throw new IllegalArgumentException("Authentication not equal principal!");
        }

        return new Greeting(counter.incrementAndGet(), String.format(template, name));
    }
}