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

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

public class CookieController {

    @RequestMapping(value = "/cookie", method = GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String cookie(@CookieValue("cookieName1") String cookieValue1, @CookieValue(value = "cookieName2", required = false) String cookieValue2) {
        return "{\"cookieValue1\" : \"" + cookieValue1 + "\", \"cookieValue2\" : \"" + cookieValue2 + "\"}";
    }

    @RequestMapping(value = "/setCookies", method = GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String setCookies(HttpServletResponse response,
                     @RequestParam("cookieName1") String cookieName1, @RequestParam("cookieValue1") String cookieValue1,
                     @RequestParam("cookieName2") String cookieName2, @RequestParam("cookieValue2") String cookieValue2) {
        response.addCookie(new Cookie(cookieName1, cookieValue1));
        response.addCookie(new Cookie(cookieName2, cookieValue2));
        return "{}";
    }
}
