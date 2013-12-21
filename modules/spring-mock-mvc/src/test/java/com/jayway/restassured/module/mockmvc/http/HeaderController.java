package com.jayway.restassured.module.mockmvc.http;

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

public class HeaderController {

    @RequestMapping(value = "/header", method = GET, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String header(@RequestHeader("headerName") String headerValue,
                                       @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        return "{\"headerName\" : \""+headerValue+"\", \"user-agent\" : \""+userAgent+"\"}";
    }
}
