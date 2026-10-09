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
package io.restassured.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoParameterValueTest {

    @Test
    void uses_identity_equality_and_the_default_to_string() {
        NoParameterValue value = new NoParameterValue();

        assertThat(value).isEqualTo(value).isNotEqualTo(new NoParameterValue());
        assertThat(value.hashCode()).isEqualTo(System.identityHashCode(value));
        assertThat(value.toString()).isEqualTo(NoParameterValue.class.getName() + "@" + Integer.toHexString(value.hashCode()));
    }
}
