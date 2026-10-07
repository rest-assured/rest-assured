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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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
    @DisplayName("LocalDate is not a serialization candidate")
    public void localDateIsNotASerializationCandidate() {
        // Act
        boolean candidate = SerializationSupport.isSerializableCandidate(LocalDate.of(2024, 4, 10));
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

    @Test
    @DisplayName("java.time values are not serialization candidates")
    public void javaTimeValuesAreNotSerializationCandidates() {
        assertThat(SerializationSupport.isSerializableCandidate(Instant.parse("2024-04-10T10:15:30Z"))).isFalse();
        assertThat(SerializationSupport.isSerializableCandidate(OffsetDateTime.parse("2024-04-10T10:15:30+02:00"))).isFalse();
        assertThat(SerializationSupport.isSerializableCandidate(Duration.ofMinutes(5))).isFalse();
        assertThat(SerializationSupport.isSerializableCandidate(ZoneId.of("Europe/Stockholm"))).isFalse();
    }

    @Test
    @DisplayName("a serialized JSON string literal is unwrapped and unescaped")
    public void jsonStringLiteralIsUnwrapped() {
        assertThat(SerializationSupport.unwrapJsonStringLiteral("\"c2a5a7c4-587a-4f13-94f9-084ebfac5302\""))
                .isEqualTo("c2a5a7c4-587a-4f13-94f9-084ebfac5302");
        assertThat(SerializationSupport.unwrapJsonStringLiteral("\"a \\\"quoted\\\" \\\\ value\\u00e9\""))
                .isEqualTo("a \"quoted\" \\ value\u00e9");
        assertThat(SerializationSupport.unwrapJsonStringLiteral("\"\"")).isEmpty();
    }

    @Test
    @DisplayName("serialized values that are not a single JSON string literal are left as is")
    public void nonStringLiteralsAreLeftAsIs() {
        assertThat(SerializationSupport.unwrapJsonStringLiteral("{\"name\":\"value\"}")).isEqualTo("{\"name\":\"value\"}");
        assertThat(SerializationSupport.unwrapJsonStringLiteral("[\"a\",\"b\"]")).isEqualTo("[\"a\",\"b\"]");
        assertThat(SerializationSupport.unwrapJsonStringLiteral("\"a\",\"b\"")).isEqualTo("\"a\",\"b\"");
        assertThat(SerializationSupport.unwrapJsonStringLiteral("\"")).isEqualTo("\"");
        assertThat(SerializationSupport.unwrapJsonStringLiteral("42")).isEqualTo("42");
        assertThat(SerializationSupport.unwrapJsonStringLiteral(null)).isNull();
    }
}
