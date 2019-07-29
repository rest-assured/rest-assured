package com.oneandone.iocunit.restassuredtest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpRequestPreprocessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.oneandone.iocunit.IocUnitRunner;
import com.oneandone.iocunit.analyzer.annotations.SutClasses;
import com.oneandone.iocunit.restassuredtest.http.GreetingResource;

import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.http.ContentType;

/**
 * @author aschoerk
 */
@RunWith(IocUnitRunner.class)
@SutClasses(GreetingResource.class)
public class AcceptTest {
    @Inject
    Dispatcher dispatcher;

    final List<String> accept = new ArrayList<String>();

    @Before
    public void beforeTest() {
        dispatcher.addHttpPreprocessor(new HttpRequestPreprocessor() {
            @Override
            public void preProcess(final HttpRequest request) {
                for (Object header : request.getHttpHeaders().getRequestHeader("Accept")) {
                    accept.add(String.valueOf(header));
                }
            }
        });
    }


    @Test
    public void
    adds_accept_by_content_type() {

        RestAssured.given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                accept(ContentType.JSON).
                when().
                get("/greeting").
                then().
                statusCode(200);

        assertEquals(accept.get(0),"application/json, application/javascript, text/javascript, text/json");
    }

    @Test
    public void
    adds_accept_by_string_value() {

        RestAssured.given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                accept("application/json, application/javascript").
                when().
                get("/greeting").
                then().
                statusCode(200);

        assertEquals(accept.get(0),"application/json, application/javascript");
    }

    @Test
    public void
    adds_accept_by_media_type() {

        RestAssured.given().
                config(RestAssured.config().encoderConfig(EncoderConfig.encoderConfig().appendDefaultContentCharsetToContentTypeIfUndefined(false))).
                // accept(MediaType.APPLICATION_JSON,MediaType.APPLICATION_FORM_URLENCODED).
                accept("application/json, application/x-www-form-urlencoded").
                when().
                get("/greeting").
                then().
                statusCode(200);

        assertEquals(accept.get(0),"application/json, application/x-www-form-urlencoded");
    }
}
