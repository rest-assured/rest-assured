/*
 * Copyright 2013 the original author or authors.
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
package com.jayway.restassured.examples.springmvc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
public class FileUploadController {

    @RequestMapping(value = "/fileUpload", method = POST, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String fileUpload(@RequestParam MultipartFile file) {
        return "{ \"size\" : "+file.getSize()+", \"name\" : \""+file.getName()+"\" }";
    }

    @RequestMapping(value = "/fileUpload2", method = POST, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String fileUpload2(@RequestParam(value = "controlName") MultipartFile file) {
        return "{ \"size\" : "+file.getSize()+", \"name\" : \""+file.getName()+"\", \"originalName\" : \""+file.getOriginalFilename()+"\", \"mimeType\" : \""+file.getContentType()+"\" }";
    }

    @RequestMapping(value = "/multiFileUpload", method = POST, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
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
}