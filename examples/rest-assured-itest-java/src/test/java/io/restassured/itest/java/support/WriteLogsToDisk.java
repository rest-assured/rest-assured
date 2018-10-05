/*
 * Copyright 2018 the original author or authors.
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
package io.restassured.itest.java.support;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import org.apache.commons.io.output.WriterOutputStream;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.io.*;

public class WriteLogsToDisk extends TestWatcher {

    private final String logFolder;
    private PrintStream printStream;
    private LogConfig originalLogConfig;

    public WriteLogsToDisk(String logFolder) {
        File dir = new File(logFolder);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        if (logFolder.endsWith("/")) {
            this.logFolder = logFolder.substring(0, logFolder.length() - 1);
        } else {
            this.logFolder = logFolder;
        }
    }

    @Override
    protected void starting(Description description) {
        originalLogConfig = RestAssured.config().getLogConfig();
        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(logFolder + "/" + description.getMethodName() + ".log");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        printStream = new PrintStream(new WriterOutputStream(fileWriter), true);
        RestAssured.config = RestAssured.config().logConfig(LogConfig.logConfig().defaultStream(printStream).enablePrettyPrinting(false));
    }


    @Override
    protected void finished(Description description) {
        if (printStream != null) {
            printStream.close();
        }

        if (originalLogConfig != null) {
            RestAssured.config = RestAssuredConfig.config().logConfig(originalLogConfig);
        }
    }
}