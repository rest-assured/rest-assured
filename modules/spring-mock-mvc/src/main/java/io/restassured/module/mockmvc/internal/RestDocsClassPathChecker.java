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

package io.restassured.module.mockmvc.internal;

import static io.restassured.internal.common.classpath.ClassPathResolver.existInCP;

/**
 * Checks whether or not the spring-restdocs-mockmvc module is in classpath
 */
class RestDocsClassPathChecker {
    private static final String REST_DOCUMENTATION_REQUEST_BUILDERS_CLASS_NAME = "org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders";

    public static boolean isSpringRestDocsInClasspath() {
        return existInCP(REST_DOCUMENTATION_REQUEST_BUILDERS_CLASS_NAME);
    }
}
