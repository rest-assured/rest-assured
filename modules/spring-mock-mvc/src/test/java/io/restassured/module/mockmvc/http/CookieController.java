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

import org.springframework.mock.web.MockCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
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

    @RequestMapping(value = "/setDetailedCookies", method = GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String setDetailedCookies(HttpServletResponse response,
                                                   @RequestParam("cookieName1") String cookieName1, @RequestParam("cookieValue1") String cookieValue1,
                                                   @RequestParam("cookieName2") String cookieName2, @RequestParam("cookieValue2") String cookieValue2) {
        MockCookie cookie1 = new MockCookie(cookieName1, cookieValue1);
        cookie1.setHttpOnly(true);
        cookie1.setSameSite("None");
        cookie1.setSecure(true);
        cookie1.setExpires(ZonedDateTime.of(2023, 1, 1, 12, 30, 0, 0, ZoneId.of("Z")));
        response.addCookie(cookie1);

        MockCookie cookie2 = new MockCookie(cookieName2, cookieValue2);
        cookie2.setHttpOnly(false);
        cookie2.setSameSite("Lax");
        cookie2.setSecure(false);
        cookie2.setExpires(ZonedDateTime.of(2023, 1, 1, 12, 30, 0, 0, ZoneId.of("Z")));
        response.addCookie(cookie2);
        return "{}";
    }
}
