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

package io.restassured.response;

/**
 * The response of a request made by REST Assured.
 * <p>
 * Usage example:
 * <pre>
 * Response response = get("/lotto");
 * String body = response.getBody().asString();
 * String headerValue = response.getHeader("headerName");
 * String cookieValue = response.getCookie("cookieName");
 * </pre>
 * <p>
 * You can also map the response body to a Java object automatically. REST Assured will use
 * Jackson, Gson and JAXB to accommodate this:
 * <pre>
 * Message message = get("/message").as(Message.class);
 * </pre>
 * </p>
 */
public interface Response extends ResponseBody<Response>, ResponseOptions<Response>, Validatable<ValidatableResponse, Response> {
}
