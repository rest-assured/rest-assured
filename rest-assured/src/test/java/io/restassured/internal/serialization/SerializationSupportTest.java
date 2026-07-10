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

package io.restassured.internal.serialization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SerializationSupportTest {

    private enum PlainEnum {
        VALUE
    }

    private enum EnumWithBody {
        VALUE {
            @Override
            public String toString() {
                return "value";
            }
        }
    }

    private static class Pojo {
        @SuppressWarnings("unused")
        String name;
    }

    @Test
    @DisplayName("plain enum constant is not a serialization candidate")
    public void plainEnumConstantIsNotASerializationCandidate() {
        // Act
        boolean candidate = SerializationSupport.isSerializableCandidate(PlainEnum.VALUE);
        // Assert
        assertThat(candidate).isFalse();
    }

    @Test
    @DisplayName("enum constant with a body is not a serialization candidate")
    public void enumConstantWithBodyIsNotASerializationCandidate() {
        // Act
        boolean candidate = SerializationSupport.isSerializableCandidate(EnumWithBody.VALUE);
        // Assert
        assertThat(candidate).isFalse();
    }

    @Test
    @DisplayName("plain object is a serialization candidate")
    public void plainObjectIsASerializationCandidate() {
        // Act
        boolean candidate = SerializationSupport.isSerializableCandidate(new Pojo());
        // Assert
        assertThat(candidate).isTrue();
    }

    @Test
    @DisplayName("null is not a serialization candidate")
    public void nullIsNotASerializationCandidate() {
        // Act
        boolean candidate = SerializationSupport.isSerializableCandidate(null);
        // Assert
        assertThat(candidate).isFalse();
    }
}
