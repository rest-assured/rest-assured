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
package io.restassured.module.spring.commons;

import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.internal.RequestSpecificationImpl;
import io.restassured.internal.http.CharsetExtractor;
import io.restassured.module.spring.commons.config.SpecificationConfig;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static org.apache.commons.lang3.SystemUtils.LINE_SEPARATOR;

public class RequestLogger {

    public static void logParamsAndHeaders(final RequestSpecificationImpl reqSpec, String method, String uri,
                                           Object[] unnamedPathParams, Map<String, Object> params, Map<String,
            Object> queryParams, Map<String, Object> formParams,
                                           Headers headers, Cookies cookies) {
        reqSpec.setMethod(method);
        reqSpec.path(uri);
        reqSpec.buildUnnamedPathParameterTuples(unnamedPathParams);
        if (params != null) {
            new ParamLogger(params) {
                protected void logParam(String paramName, Object paramValue) {
                    reqSpec.param(paramName, paramValue);
                }
            }.logParams();
        }

        if (queryParams != null) {
            new ParamLogger(queryParams) {
                protected void logParam(String paramName, Object paramValue) {
                    reqSpec.queryParam(paramName, paramValue);
                }
            }.logParams();
        }

        if (formParams != null) {
            new ParamLogger(formParams) {
                protected void logParam(String paramName, Object paramValue) {
                    reqSpec.formParam(paramName, paramValue);
                }
            }.logParams();
        }

        if (headers != null) {
            for (Header header : headers) {
                reqSpec.header(header);
            }
        }

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                reqSpec.cookie(cookie);
            }
        }
    }

    public static void logRequestBody(RequestSpecificationImpl reqSpec, Object requestBody, Headers headers,
                                      List<Object> multiParts, SpecificationConfig config) {
        if (requestBody != null) {
            if (requestBody instanceof byte[]) {
                reqSpec.body((byte[]) requestBody);
            } else if (requestBody instanceof File) {
                String contentType = HeaderHelper.findContentType(headers, multiParts, config);
                RequestLogger.logFileRequestBody(reqSpec, requestBody, contentType);
            } else {
                reqSpec.body(requestBody);
            }
        }
    }

    public static void logFileRequestBody(RequestSpecificationImpl reqSpec, Object requestBody, String contentType) {
        String charset = null;
        if (StringUtils.isNotBlank(contentType)) {
            charset = CharsetExtractor.getCharsetFromContentType(contentType);
        }

        if (charset == null) {
            charset = Charset.defaultCharset().toString();
        }

        String string = fileToString((File) requestBody, charset);
        reqSpec.body(string);
    }

    private static String fileToString(File file, String charset) {
        StringBuilder fileContents = new StringBuilder((int) file.length());
        Scanner scanner;
        try {
            scanner = new Scanner(file, charset);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        String lineSeparator = System.getProperty(LINE_SEPARATOR);

        try {
            while (scanner.hasNextLine()) {
                fileContents.append(scanner.nextLine()).append(lineSeparator);
            }
            return fileContents.toString();
        } finally {
            scanner.close();
        }
    }
}
