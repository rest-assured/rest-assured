/*
 * Copyright 2026 the original author or authors.
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

package io.restassured.specification;

import org.junit.jupiter.api.Test;

import static io.restassured.specification.ProxySpecification.auth;
import static org.assertj.core.api.Assertions.assertThat;

public class ProxySpecificationTest {

    @Test public void
    the_with_methods_keep_the_user_name_and_password() {
        ProxySpecification proxy = auth("admin", "secret").withHost("proxy").withPort(9090).withScheme("https");

        assertThat(proxy.getHost()).isEqualTo("proxy");
        assertThat(proxy.getPort()).isEqualTo(9090);
        assertThat(proxy.getScheme()).isEqualTo("https");
        assertThat(proxy.getUsername()).isEqualTo("admin");
        assertThat(proxy.getPassword()).isEqualTo("secret");
    }
}
