package io.restassured.module.webtestclient.setup;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.http.MediaType.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class PatchController {
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @PatchMapping(value = "/greetingPatch", consumes = APPLICATION_FORM_URLENCODED_VALUE)
    public Mono<Greeting> greeting(@RequestParam("name") String name) {
        return Mono.just(new Greeting(counter.incrementAndGet(), String.format(template, name)));
    }

    @PatchMapping("/stringBody")
    public Mono<String> stringBody(@RequestBody String body) {
        return Mono.just(body);
    }

    @PatchMapping(value = "/jsonReflect", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public Mono<String> jsonReflect(@RequestBody String body) {
        return Mono.just(body);
    }

    @PatchMapping(value = "/multipartFileUpload", consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public Mono<String> multipartFileUpload(@RequestPart("file") Mono<FilePart> file) {
        return Mono.just(file.toString());
    }
}
