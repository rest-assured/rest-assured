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

package io.restassured.internal;

import io.restassured.config.FailureConfig;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.internal.log.LogRepository;
import io.restassured.listener.ResponseValidationFailureListener;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Initial state for all checks here is response spec with response body loaded into its logRepository. This
 * structure means that ResponseLoggingFilter saved response for later to be printed only if validation fails.
 */
class ResponseSpecificationImplTest {

    private static final String EXPECTED_BODY = "goodTestBody";
    private static final String UNEXPECTED_BODY = "badTestBody";

    @Test
    void should_call_default_failure_listener_to_print_response_if_log_repository_has_response_and_validation_failed() {
        // given
        PrintStream printStreamMock = mock(PrintStream.class);
        ResponseSpecificationImpl respSpecImpl = createRespSpec(UNEXPECTED_BODY, printStreamMock, null);
        Response unexpectedResponse = responseWithBody(UNEXPECTED_BODY);

        // when
        try {
            respSpecImpl.validate(unexpectedResponse);
        } catch (AssertionError ignored) {
        }

        // then
        verify(printStreamMock, times(1)).print(UNEXPECTED_BODY);
    }

    @Test
    void should_not_call_default_failure_listener_to_print_response_if_validation_passed() {
        // given
        PrintStream printStreamMock = mock(PrintStream.class);
        ResponseSpecificationImpl respSpecImpl = createRespSpec(EXPECTED_BODY, printStreamMock, null);
        Response expectedResponse = responseWithBody(EXPECTED_BODY);

        // when
        respSpecImpl.validate(expectedResponse);

        // then
        verify(printStreamMock, never()).print(EXPECTED_BODY);
    }

    @Test
    void should_call_custom_failure_listener_when_validation_fails() {
        // given
        ResponseValidationFailureListener customListener = mock(ResponseValidationFailureListener.class);
        ResponseSpecificationImpl respSpecImpl = createRespSpec(UNEXPECTED_BODY, System.out, customListener);
        Response matchingResponse = responseWithBody(UNEXPECTED_BODY);

        // when
        assertThatThrownBy(() -> respSpecImpl.validate(matchingResponse)).isInstanceOf(AssertionError.class);

        // then
        verify(customListener, times(1)).onFailure(nullable(RequestSpecification.class), same(respSpecImpl), same(matchingResponse));
    }

    @Test
    void should_not_call_custom_failure_listener_when_validation_passes() {
        // given
        ResponseValidationFailureListener customListener = mock(ResponseValidationFailureListener.class);
        ResponseSpecificationImpl respSpecImpl = createRespSpec(EXPECTED_BODY, System.out, customListener);
        Response nonmatchingResponse = responseWithBody(EXPECTED_BODY);

        // when
        respSpecImpl.validate(nonmatchingResponse);

        // then
        verify(customListener, never()).onFailure(any(RequestSpecification.class), same(respSpecImpl), same(nonmatchingResponse));
    }

    private static Response responseWithBody(String body) {
        Response response = mock(Response.class);
        when(response.asString()).thenReturn(body);
        return response;
    }

    private static ResponseSpecificationImpl createRespSpec(String responseContentInLogRepository, PrintStream logOutputStream,
                                                            ResponseValidationFailureListener failureListener) {
        List<ResponseValidationFailureListener> customFailureListeners = failureListener == null ? Collections.emptyList() : Collections.singletonList(failureListener);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.writeBytes(responseContentInLogRepository.getBytes(Charset.defaultCharset()));
        LogRepository logRepository = new LogRepository();
        logRepository.registerResponseLog(baos);

        ResponseSpecificationImpl respSpecImpl = new ResponseSpecificationImpl("/", null, null,
                new RestAssuredConfig()
                        .logConfig(new LogConfig(logOutputStream, true))
                        .failureConfig(new FailureConfig(customFailureListeners)),
                logRepository);

        respSpecImpl.body(equalTo(EXPECTED_BODY));

        return respSpecImpl;
    }
}
