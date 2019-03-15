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

package io.restassured.internal.log;

import java.io.ByteArrayOutputStream;

public class LogRepository {

    private static final String EMPTY = "";
    private ByteArrayOutputStream requestLog;
    private ByteArrayOutputStream responseLog;

    public void registerRequestLog(ByteArrayOutputStream baos) {
        this.requestLog = baos;
    }

    public void registerResponseLog(ByteArrayOutputStream baos) {
        this.responseLog = baos;
    }

    public String getRequestLog() {
        if (requestLog == null) {
            return EMPTY;
        }
        return requestLog.toString();
    }

    public String getResponseLog() {
        if (responseLog == null) {
            return EMPTY;
        }
        return responseLog.toString();
    }
}
