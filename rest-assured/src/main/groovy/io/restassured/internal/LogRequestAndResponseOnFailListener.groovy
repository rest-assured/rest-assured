package io.restassured.internal

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
