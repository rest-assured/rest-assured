package com.jayway.restassured.module.mockmvc.http;

import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

public class CookieController {

    @RequestMapping(value = "/cookie", method = GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String cookie(@CookieValue("cookieName1") String cookieValue1, @CookieValue(value = "cookieName2", required = false) String cookieValue2) {
        return "{\"cookieValue1\" : \"" + cookieValue1 + "\", \"cookieValue2\" : \"" + cookieValue2 + "\"}";
    }
}
