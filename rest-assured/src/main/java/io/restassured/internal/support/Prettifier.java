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
package io.restassured.internal.support;

import io.restassured.internal.ResponseParserRegistrar;
import io.restassured.internal.RestAssuredResponseOptionsImpl;
import io.restassured.internal.common.util.GroovyStyleToString;
import io.restassured.internal.path.json.JsonPrettifier;
import io.restassured.internal.path.xml.XmlPrettifier;
import io.restassured.internal.util.SafeExceptionRethrower;
import io.restassured.parsing.Parser;
import io.restassured.response.ResponseBody;
import io.restassured.response.ResponseOptions;
import io.restassured.specification.FilterableRequestSpecification;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class Prettifier {

    public String getPrettifiedBodyIfPossible(FilterableRequestSpecification request) {
        Object body = request.getBody();
        if (body == null) {
            return null;
        } else if (!(body instanceof String)) {
            return GroovyStyleToString.toString(body);
        }
        // RequestSpecificationImpl.getRequestContentType() returns getContentType()
        Parser parser = Parser.fromContentType(request.getContentType());
        return prettify(body, parser);
    }

    public String getPrettifiedBodyIfPossible(ResponseOptions<?> responseOptions, ResponseBody<?> responseBody) {
        String contentType = responseOptions.getContentType();
        String responseAsString = responseBody.asString();
        if (isBlank(contentType) || !(responseOptions instanceof RestAssuredResponseOptionsImpl)) {
            return responseAsString;
        }

        ResponseParserRegistrar rpr = ((RestAssuredResponseOptionsImpl<?>) responseOptions).getRpr();
        Parser parser = rpr.getParser(contentType);
        return prettify(responseAsString, parser);
    }

    public String prettify(Object content, Parser parser) {
        if (content == null) {
            return "";
        }

        if (content instanceof File) {
            return prettifyBy(new String(readBytes((File) content)), parser);
        }

        return prettifyBy(GroovyStyleToString.toString(content), parser);
    }

    private static byte[] readBytes(File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return inputStream.readAllBytes();
        } catch (IOException e) {
            return SafeExceptionRethrower.safeRethrow(e);
        }
    }

    private static String prettifyBy(String body, Parser parser) {
        if (parser == null) {
            return body;
        }
        try {
            switch (parser) {
                case JSON:
                    return JsonPrettifier.prettifyJson(body);
                case XML:
                    return XmlPrettifier.prettifyXml(body);
                case HTML:
                    return XmlPrettifier.prettifyHtml(body);
                default:
                    return body;
            }
        } catch (Exception e) {
            // Parsing failed, probably because the content was not of expected type.
            return body;
        }
    }
}
