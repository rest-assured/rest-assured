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

package io.restassured.itest.java.objects;

/**
 * Tells which JSON library deserialized it: {@link #getMapper()} is "jackson2" or "jackson3" when Jackson 2 or Jackson 3
 * did (each reads only its own annotation), and the JSON value otherwise.
 */
public class MapperProbe {
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = Jackson2Name.class)
    @tools.jackson.databind.annotation.JsonDeserialize(using = Jackson3Name.class)
    private String mapper;

    public String getMapper() {
        return mapper;
    }

    public void setMapper(String mapper) {
        this.mapper = mapper;
    }

    public static class Jackson2Name extends com.fasterxml.jackson.databind.JsonDeserializer<String> {
        @Override
        public String deserialize(com.fasterxml.jackson.core.JsonParser parser, com.fasterxml.jackson.databind.DeserializationContext ctx) {
            return "jackson2";
        }
    }

    public static class Jackson3Name extends tools.jackson.databind.ValueDeserializer<String> {
        @Override
        public String deserialize(tools.jackson.core.JsonParser parser, tools.jackson.databind.DeserializationContext ctx) {
            return "jackson3";
        }
    }
}
