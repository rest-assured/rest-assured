package io.restassured.module.mockmvc.http;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;

@Controller
public class PatchController {
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @RequestMapping(value = "/greetingPatch", method = PATCH, consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public @ResponseBody Greeting greeting(@RequestParam("name") String name) {
        return new Greeting(counter.incrementAndGet(), String.format(template, name));
    }

    @RequestMapping(value = "/stringBody", method = PATCH)
    public @ResponseBody String stringBody(@RequestBody String body) {
        return body;
    }

    @RequestMapping(value = "/jsonReflect", method = PATCH, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String jsonReflect(@RequestBody String body) {
        return body;
    }
}
