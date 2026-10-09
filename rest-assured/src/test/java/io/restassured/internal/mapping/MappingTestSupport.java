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

package io.restassured.internal.mapping;

import io.restassured.response.ResponseBodyData;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Shared fixtures for the object mapping tests.
 */
final class MappingTestSupport {

    private MappingTestSupport() {
    }

    static ResponseBodyData body(String body) {
        return new ResponseBodyData() {
            @Override
            public String asString() {
                return body;
            }

            @Override
            public String asPrettyString() {
                return body;
            }

            @Override
            public byte[] asByteArray() {
                return body.getBytes(StandardCharsets.UTF_8);
            }

            @Override
            public InputStream asInputStream() {
                return new ByteArrayInputStream(asByteArray());
            }
        };
    }

    /**
     * A bean that every JSON mapper (fields or getters/setters) and JAXB (annotated root element) can handle.
     */
    @jakarta.xml.bind.annotation.XmlRootElement(name = "greeting")
    @javax.xml.bind.annotation.XmlRootElement(name = "greeting")
    public static class Greeting {
        private String firstName;
        private String lastName;

        public Greeting() {
        }

        public Greeting(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }

    /**
     * Has no properties and no XmlRootElement, so Jackson and JAXB refuse to serialize it.
     */
    public static class Unserializable {
    }
}
