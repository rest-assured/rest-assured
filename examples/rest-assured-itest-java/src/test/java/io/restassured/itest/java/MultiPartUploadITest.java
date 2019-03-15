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

import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.EncoderConfig;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.internal.mapping.Jackson2Mapper;
import io.restassured.itest.java.objects.Greeting;
import io.restassured.itest.java.objects.Message;
import io.restassured.itest.java.support.MyEnum;
import io.restassured.itest.java.support.WithJetty;
import io.restassured.mapper.ObjectMapperType;
import io.restassured.path.json.mapper.factory.DefaultJackson2ObjectMapperFactory;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static io.restassured.config.MultiPartConfig.multiPartConfig;
import static org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE;
import static org.hamcrest.Matchers.*;

public class MultiPartUploadITest extends WithJetty {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void multiPartUploadingWorksForByteArrays() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
        expect().
                statusCode(200).
                body(is(new String(bytes))).
        when().
                post("/multipart/file");
    }

    @Test
    public void multiPartUploadingWorksForStrings() throws Exception {
       // When
       given().
               multiPart("text", "Some text").
       expect().
               statusCode(200).
               body(is("Some text")).
       when().
               post("/multipart/text");
    }

    @Test
    public void multiPartUploadingSupportsOtherSubTypesThanFormData() throws Exception {
       // When
       given().
               contentType("multipart/mixed").
               multiPart("text", "Some text").
       expect().
               statusCode(200).
               body(is("Some text")).
       when().
               post("/multipart/text");
    }

    @Test
    public void multiPartUploadingSupportsSpecifyingDefaultSubtype() throws Exception {
       // When
       given().
               config(config().multiPartConfig(multiPartConfig().defaultSubtype("mixed"))).
               multiPart("text", "Some text").
       expect().
               statusCode(200).
               body(is("Some text")).
               header("X-Request-Header", startsWith("multipart/mixed")).
       when().
               post("/multipart/textAndReturnHeader");
    }

    @Test
    public void multiPartUploadingSupportsSpecifyingCharset() throws Exception {
       // When
       given().
               contentType("multipart/mixed; charset=US-ASCII").
               multiPart("text", "Some text").
       expect().
               statusCode(200).
               body(is("Some text")).
               header("X-Request-Header", allOf(startsWith("multipart/mixed"), containsString("charset=US-ASCII"))).
       when().
               post("/multipart/textAndReturnHeader");
    }

    @Test
    public void explicitMultipartContentTypeOverridesDefaultSubtype() throws Exception {
       // When
       given().
               contentType("multipart/form-data").
               config(config().multiPartConfig(multiPartConfig().defaultSubtype("mixed"))).
               multiPart("text", "Some text").
       expect().
               statusCode(200).
               body(is("Some text")).
               header("X-Request-Header", startsWith("multipart/form-data")).
       when().
               post("/multipart/textAndReturnHeader");
    }

    @Test
    public void multipartContentTypeSetBySpecificationOverridesDefaultSubtype() throws Exception {
       // When
       given().
               spec(new RequestSpecBuilder().setContentType("multipart/form-data").build()).
               config(config().multiPartConfig(multiPartConfig().defaultSubtype("mixed"))).
               multiPart("text", "Some text").
       expect().
               statusCode(200).
               body(is("Some text")).
               header("X-Request-Header", startsWith("multipart/form-data")).
       when().
               post("/multipart/textAndReturnHeader");
    }

    @Test
    public void multiPartUploadingWorksForJsonObjects() throws Exception {
        // Given
        final Message message = new Message();
        message.setMessage("Hello World");

        // When
       given().
               multiPart("text", message).
       expect().
               statusCode(200).
               body(is("{\"message\":\"Hello World\"}")).
       when().
               post("/multipart/text");
    }

    @Test
    public void multiPartUploadingWorksForJsonObjectsWhenMimeTypeIsSpecified() throws Exception {
       // Given
       final Message message = new Message();
       message.setMessage("Hello World");

       // When
       given().
               multiPart("text", message, "application/some+json").
       expect().
               statusCode(200).
               body(is("{\"message\":\"Hello World\"}")).
       when().
               post("/multipart/text");
    }

    @Test
    public void multiPartUploadingWorksForXmlObjectsWhenMimeTypeIsSpecified() throws Exception {
       // Given
       final Greeting greeting = new Greeting();
       greeting.setFirstName("John");
       greeting.setLastName("Doe");

       // When
       given().
               multiPart("text", greeting, "application/some+xml").
       expect().
               statusCode(200).
               body(endsWith("<greeting><firstName>John</firstName><lastName>Doe</lastName></greeting>")).
       when().
               post("/multipart/text");
    }

    @Test
    public void multiPartSupportsSpecifyingAnObjectMapperTypeToMultiPartSpecBuilder() throws Exception {
        // Given
        final Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");

        // When
        given().
                multiPart(new MultiPartSpecBuilder(greeting, ObjectMapperType.JACKSON_2)
                        .fileName("RoleBasedAccessFeaturePlan.csv")
                        .controlName("text")
                        .mimeType("application/vnd.ms-excel").build()).
        when().
                post("/multipart/text").
        then().
                statusCode(200).
                body(containsString("John"), containsString("Doe"), containsString("{"));
    }

    @Test
    public void multiPartSupportsSpecifyingAnObjectMapperToMultiPartSpecBuilder() throws Exception {
        // Given
        final Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");

        // When
        given().
                multiPart(new MultiPartSpecBuilder(greeting, new Jackson2Mapper(new DefaultJackson2ObjectMapperFactory()))
                        .fileName("RoleBasedAccessFeaturePlan.csv")
                        .controlName("text")
                        .mimeType("application/vnd.ms-excel").build()).
        when().
                post("/multipart/text").
        then().
                statusCode(200).
                body(containsString("John"), containsString("Doe"), containsString("{"));
    }

    @Test
    public void multiPartObjectMapperTypeHavePrecedenceOverMimeType() throws Exception {
        // Given
        final Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");

        // When
        given().
                multiPart(new MultiPartSpecBuilder(greeting, ObjectMapperType.JAXB)
                        .fileName("RoleBasedAccessFeaturePlan.csv")
                        .controlName("text")
                        .mimeType("application/json").build()).
        when().
                post("/multipart/text").
        then().
                statusCode(200).
                body(containsString("John"), containsString("Doe"), containsString("<"));
    }

    @Test
    public void multiPartUploadingUsesEncoderConfigToKnowHowToSerializeCustomMimeTypesToJson() throws Exception {
         // Given
       final Greeting greeting = new Greeting();
       greeting.setFirstName("John");
       greeting.setLastName("Doe");

       // When
       given().
               config(config().encoderConfig(EncoderConfig.encoderConfig().encodeContentTypeAs("application/vnd.ms-excel", ContentType.JSON))).
               multiPart(new MultiPartSpecBuilder(greeting)
                       .fileName("RoleBasedAccessFeaturePlan.csv")
                       .controlName("text")
                       .mimeType("application/vnd.ms-excel").build()).
       when().
               post("/multipart/text").
       then().
               statusCode(200).
               body(containsString("John"), containsString("Doe"), containsString("{"));
    }

    @Test
    public void multiPartUploadingUsesEncoderConfigToKnowHowToSerializeCustomMimeTypesToXml() throws Exception {
         // Given
       final Greeting greeting = new Greeting();
       greeting.setFirstName("John");
       greeting.setLastName("Doe");

       // When
       given().
               config(config().encoderConfig(EncoderConfig.encoderConfig().encodeContentTypeAs("application/vnd.ms-excel", ContentType.XML))).
               multiPart(new MultiPartSpecBuilder(greeting)
                       .fileName("RoleBasedAccessFeaturePlan.csv")
                       .controlName("text")
                       .mimeType("application/vnd.ms-excel").build()).
       when().
               post("/multipart/text").
       then().
               statusCode(200).
               body(containsString("John"), containsString("Doe"), containsString("<"));
    }

    @Test
    public void multiPartUploadingThrowsExceptionWhenUsingEncoderConfigToSpecifyNonSerializableContentType() throws Exception {
        // Given
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Cannot serialize because cannot determine how to serialize content-type application/vnd.ms-excel as HTML (no serializer supports this format)");

        final Greeting greeting = new Greeting();
        greeting.setFirstName("John");
        greeting.setLastName("Doe");

       // When
       given().
               config(config().encoderConfig(EncoderConfig.encoderConfig().encodeContentTypeAs("application/vnd.ms-excel", ContentType.HTML))).
               multiPart(new MultiPartSpecBuilder(greeting)
                       .fileName("RoleBasedAccessFeaturePlan.csv")
                       .controlName("text")
                       .mimeType("application/vnd.ms-excel").build()).
       when().
               post("/multipart/text");
    }

   @Test
   public void multiPartUploadingWorksForMultipleStrings() throws Exception {
       // When
       given().
               multiPart("text", "Some text").
               multiPart("text", "Some other text").
       expect().
               statusCode(200).
               body(is("Some text,Some other text")).
       when().
               post("/multipart/text");
    }

    @Test
    public void multiPartUploadingWorksForByteArrayAndStrings() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
                multiPart("text", "Some text").
        expect().
                statusCode(200).
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingWorksForByteArrayAndFormParams() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
                formParam("text", "Some text").
        expect().
                statusCode(200).
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingWorksForByteArrayAndNumberFormParams() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
                formParam("text", 2L).
        expect().
                statusCode(200).
                body(is(new String(bytes)+"2")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingWorksForByteArrayAndEnumFormParams() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
                formParam("text", MyEnum.ENUM_1).
        expect().
                statusCode(200).
                body(is(new String(bytes)+"ENUM_1")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingWorksForFormParamsAndByteArray() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                formParam("text", "Some text").
                multiPart("file", "myFile", bytes).
        expect().
                statusCode(200).
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingWorksForByteArrayAndParams() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
                param("text", "Some text").
        expect().
                statusCode(200).
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingWorksForParamsAndByteArray() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                param("text", "Some text").
                multiPart("file", "myFile", bytes).
        expect().
                statusCode(200).
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void bytesAndFormParamUploadingWorkUsingRequestBuilder() throws Exception {
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));
        final RequestSpecification spec = new RequestSpecBuilder().addMultiPart("file", "myFile", bytes).addFormParam("text", "Some text").build();

        // When
        given().
                spec(spec).
        expect().
                body(is(new String(bytes)+"Some text")).
        when().
                post("/multipart/fileAndText");
    }

    @Test
    public void multiPartUploadingDoesntWorkForDelete() throws Exception {
         given().
                 multiPart("text", "Some text").
         when().
                 delete("/multipart/text").
         then().
                 statusCode(500); // Scalatra doesn't seem to handle multipart delete requests?
    }

    @Test
    public void multiPartByteArrayUploadingWorksUsingPut() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
        expect().
                statusCode(200).
                body(is(new String(bytes))).
        when().
                put("/multipart/file");
    }

    @Test
    public void multiPartByteArrayUploadingWorksUsingGet() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
        when().
                get("/multipart/file").
        then().
                statusCode(200).
                body(is(new String(bytes)));
    }

    @Test
    public void multiPartByteArrayUploadingWorksUsingOptions() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                multiPart("file", "myFile", bytes).
        when().
                options("/multipart/file").
        then().
                statusCode(200).
                body(is(new String(bytes)));
    }

    @Test
    public void multiPartByteArrayUploadingWorksUsingForUtf8ControlNamesWhenCharsetIsSpecifiedInContentTypeAndMultipartModeIsNotStrict() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                contentType("multipart/xml; charset=UTF-8").
                config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig().httpMultipartMode(BROWSER_COMPATIBLE))).
                multiPart(new MultiPartSpecBuilder(bytes).controlName("Cédrìc").build()).
        when().
                post("/multipart/file-utf8").
        then().
                statusCode(200).
                body(is(new String(bytes)));
    }

    @Test
    public void multiPartByteArrayUploadingWorksUsingForUtf8ControlNamesWhenDefaultCharsetIsSpecifiedInMultiPartConfigAndMultipartModeIsNotStrict() throws Exception {
        // Given
        final byte[] bytes = IOUtils.toByteArray(getClass().getResourceAsStream("/car-records.xsd"));

        // When
        given().
                config(RestAssuredConfig.config().httpClient(HttpClientConfig.httpClientConfig().httpMultipartMode(BROWSER_COMPATIBLE)).multiPartConfig(multiPartConfig().defaultCharset("UTF-8"))).
                multiPart(new MultiPartSpecBuilder(bytes).controlName("Cédrìc").build()).
        when().
                post("/multipart/file-utf8").
        then().
                statusCode(200).
                body(is(new String(bytes)));
    }
}
