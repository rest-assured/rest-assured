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

package io.restassured.internal.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class CommonVersionCheckTest {

    @Test
    void passes_when_common_has_the_required_type() {
        assertThatNoException().isThrownBy(CommonVersionCheck::verify);
    }

    @Test
    void fails_with_a_message_when_common_is_older() {
        // A class loader that sees only the JDK, like a classpath with an older rest-assured-common
        ClassLoader jdkOnly = new ClassLoader(null) {
        };

        assertThatThrownBy(() -> CommonVersionCheck.verify(jdkOnly))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("An older rest-assured-common is on the classpath but REST Assured is newer. If you use Spring Boot 3, set the rest-assured.version property instead of a version on the dependency. Otherwise import io.rest-assured:rest-assured-bom.");
    }

    @Test
    void message_names_both_versions_when_known() {
        assertThat(CommonVersionCheck.message("5.5.6", "6.3.0"))
                .isEqualTo("rest-assured-common 5.5.6 is on the classpath but REST Assured is 6.3.0. If you use Spring Boot 3, set the rest-assured.version property instead of a version on the dependency. Otherwise import io.rest-assured:rest-assured-bom.");
    }
}
