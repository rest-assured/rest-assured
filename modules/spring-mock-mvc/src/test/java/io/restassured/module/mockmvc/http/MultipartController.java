package io.restassured.module.mockmvc.http;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
public class MultipartController {

    @PostMapping(value = "/files/{type}", consumes = {"multipart/form-data"}, produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public String saveFile(
            @PathVariable String type,
            @RequestPart("file") MultipartFile file) {
        return String.format("{\"type\": \"%s\", \"name\": \"%s\"}", type, file.getName());
    }
}
