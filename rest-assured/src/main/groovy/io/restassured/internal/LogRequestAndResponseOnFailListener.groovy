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

package io.restassured.internal

import io.restassured.listener.ResponseValidationFailureListener
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification
import io.restassured.specification.ResponseSpecification
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.SystemUtils

/**
 * Default listener fired when validation fails in ResponseSpecificationImpl.
 * The listener inspect fields requestLog and responseLog in logRepository. If these fields are not empty,
 * it means that "log on validation" was enabled and these fields (also streams) were used to store logs temporarily.
 * Since the execution reached the listener's code, it also means that it is time to log request and response
 * to actual output stream - which is logRepository.defaultStream()
 */
class LogRequestAndResponseOnFailListener implements ResponseValidationFailureListener {

    @Override
    void onFailure(RequestSpecification requestSpecification,
                   ResponseSpecification responseSpecification,
                   Response response) {
        if (!responseSpecification instanceof ResponseSpecificationImpl) {
            return
        }
        ResponseSpecificationImpl respSpecImpl = (ResponseSpecificationImpl) responseSpecification
        def logRepository = respSpecImpl.getLogRepository()
        if (logRepository != null) {
            def stream = responseSpecification.getConfig().getLogConfig().defaultStream()
            def requestLog = logRepository.requestLog
            def responseLog = logRepository.responseLog
            def requestLogHasText = StringUtils.isNotEmpty(requestLog)
            if (requestLogHasText) {
                stream.print(requestLog)
            }
            if (StringUtils.isNotEmpty(responseLog)) {
                if (requestLogHasText) {
                    stream.print(SystemUtils.LINE_SEPARATOR);
                }
                stream.print(responseLog)
            }
        }
    }
}
