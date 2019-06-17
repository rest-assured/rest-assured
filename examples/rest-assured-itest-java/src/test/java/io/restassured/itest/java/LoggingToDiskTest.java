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
package io.restassured.itest.java;

import io.restassured.itest.java.support.WithJetty;
import io.restassured.itest.java.support.WriteLogsToDisk;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static io.restassured.itest.java.support.WithJetty.JettyOption.DONT_RESET_REST_ASSURED_BEFORE_TEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoggingToDiskTest extends WithJetty {

    public LoggingToDiskTest() {
        super(DONT_RESET_REST_ASSURED_BEFORE_TEST);
    }

    private static File directory;

    static {
        try {
            directory = Files.createTempDirectory("ra").toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Rule
    public TestName testName = new TestName();

    @Rule
    public WriteLogsToDisk writeLogsToDisk = new WriteLogsToDisk(directory);

    @Test
    public void example1() {
        given().
                queryParam("firstName", "John").
                queryParam("lastName", "Doe").
        when().
                get("/greet").
        then().
                log().all().
                body("greeting", equalTo("Greetings John Doe"));
    }

    @Test
    public void example2() {
        given().
                queryParam("firstName", "Jane").
                queryParam("lastName", "Doe").
        when().
                get("/greet").
        then().
                log().all().
                body("greeting", equalTo("Greetings Jane Doe"));
    }

    // This test needs to be executed last, thus @FixMethodOrder
    @Test
    public void make_sure_logging_to_disk_works() throws Exception {
        List<FileAndContents> files = Files.list(directory.toPath())
                .filter(file -> !file.endsWith(testName.getMethodName() + ".log"))
                .map(file -> {
                    try {
                        return FileAndContents.of(file.getFileName().toString(), Files.readAllLines(file));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .collect(Collectors.toList());

        FileAndContents example1Log = findByName(files, "example1.log");
        FileAndContents example2Log = findByName(files, "example2.log");

        assertThat(files.toString(), files, hasSize(2));
        assertThat(example1Log.content.getLast(), is("{\"greeting\":\"Greetings John Doe\"}"));
        assertThat(example2Log.content.getLast(), is("{\"greeting\":\"Greetings Jane Doe\"}"));
    }

    private FileAndContents findByName(List<FileAndContents> files, String name) {
        return files.stream().filter(f -> name.equals(f.fileName)).findFirst().orElse(null);
    }

    private static class FileAndContents {
        final String fileName;
        final LinkedList<String> content;

        FileAndContents(String fileName, List<String> content) {
            this.fileName = fileName;
            this.content = new LinkedList<>(content);
        }

        private static FileAndContents of(String fileName, List<String> content) {
            return new FileAndContents(fileName, content);
        }

        @Override
        public String toString() {
            return "FileAndContents{" +
                    "fileName='" + fileName + '\'' +
                    ", content='" + content + '\'' +
                    '}';
        }
    }
}
