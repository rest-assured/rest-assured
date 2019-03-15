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

package io.restassured.http;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CookieTest {

    @Test public void
    can_use_negative_values_as_max_age() {
        // When
        Cookie cookie = new Cookie.Builder("hello", "world").setMaxAge(-3600).build();

        // Then
        assertThat(cookie.getMaxAge()).isEqualTo(-3600);
    }
}