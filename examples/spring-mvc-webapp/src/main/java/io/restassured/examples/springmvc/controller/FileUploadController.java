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
package io.restassured.examples.springmvc.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Controller
public class FileUploadController {

    @RequestMapping(value = "/fileUpload", method = { POST, PUT } , consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String fileUpload(@RequestParam MultipartFile file) {
        return "{ \"size\" : "+file.getSize()+", \"name\" : \""+file.getName()+"\" }";
    }

    @RequestMapping(value = "/fileUpload2", method = { POST, PUT }, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String fileUpload2(@RequestParam(value = "controlName") MultipartFile file) {
        return "{ \"size\" : "+file.getSize()+", \"name\" : \""+file.getName()+"\", \"originalName\" : \""+file.getOriginalFilename()+"\", \"mimeType\" : \""+file.getContentType()+"\" }";
    }

    @RequestMapping(value = "/multiFileUpload", method = { POST, PUT }, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody List<FileDescription> multiFileUpload(@RequestParam(value = "controlName1") MultipartFile file1, @RequestParam(value = "controlName2") MultipartFile file2) throws IOException {
        FileDescription fd1 = new FileDescription();
        fd1.setContent(new String(file1.getBytes()));
        fd1.setName(file1.getName());
        fd1.setMimeType(file1.getContentType());
        fd1.setOriginalName(file1.getOriginalFilename());
        fd1.setSize(file1.getSize());

        FileDescription fd2 = new FileDescription();
        fd2.setContent(new String(file2.getBytes()));
        fd2.setName(file2.getName());
        fd2.setMimeType(file2.getContentType());
        fd2.setOriginalName(file2.getOriginalFilename());
        fd2.setSize(file2.getSize());
        return asList(fd1, fd2);
    }

    @RequestMapping(value = "/fileUploadWithParam", method = { POST, PUT }, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody FileWithParam fileUploadWithParam(@RequestParam(value = "controlName") MultipartFile file, @RequestParam(value = "param", required = false) String param) throws IOException {
        FileDescription fd1 = new FileDescription();
        fd1.setContent(new String(file.getBytes()));
        fd1.setName(file.getName());
        fd1.setMimeType(file.getContentType());
        fd1.setOriginalName(file.getOriginalFilename());
        fd1.setSize(file.getSize());

        FileWithParam fileWithParam = new FileWithParam();
        fileWithParam.setFile(fd1);
        fileWithParam.setParam(param);

        return fileWithParam;
    }

    @RequestMapping(value = "/nonMultipartFileUpload", method = POST, consumes = APPLICATION_OCTET_STREAM_VALUE, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String nonMultipartFileUpload(@RequestBody String is) throws IOException {
        return "{ \"size\" : " + is.length() + ", \"content\":\"" + is + "\" }";
    }

    @RequestMapping(value = "/fileUploadWithControlNameEqualToSomething", method = { POST, PUT }, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String fileUploadWithControlNameEqualToSomething(@RequestParam(value = "something") MultipartFile file) {
        return "{ \"size\" : "+file.getSize()+", \"name\" : \""+file.getName()+"\", \"originalName\" : \""+file.getOriginalFilename() + "\", \"mimeType\" : \""+file.getContentType()+"\" }";
    }

    @RequestMapping(value = "/textAndReturnHeader", method = { POST, PUT }, consumes = "multipart/mixed", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> fileUploadWithControlNameEqualToSomething(
            @RequestHeader("Content-Type") String requestContentType,
            @RequestParam(value = "something") MultipartFile file) {
        return ResponseEntity.ok().header(APPLICATION_JSON_VALUE).header("X-Request-Header", requestContentType).body("{ \"size\" : " + file.getSize() + ", \"name\" : \"" + file.getName() + "\", \"originalName\" : \"" + file.getOriginalFilename() + "\", \"mimeType\" : \"" + file.getContentType() + "\" }");
    }

}