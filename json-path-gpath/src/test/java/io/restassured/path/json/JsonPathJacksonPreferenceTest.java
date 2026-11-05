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

package io.restassured.path.json;

import io.restassured.path.json.config.JsonParserType;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.support.MapperProbe;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Jackson 2, Jackson 3 and Gson are all on this module's test classpath.
 */
class JsonPathJacksonPreferenceTest {
    private static final String JSON = "{ \"probe\" : { \"mapper\" : \"none\" } }";

    @Test
    void get_object_prefers_jackson_3_when_both_jacksons_are_in_classpath() {
        MapperProbe probe = new JsonPath(JSON).getObject("probe", MapperProbe.class);

        assertThat(probe.getMapper(), equalTo("jackson3"));
    }

    @Test
    void get_object_uses_jackson_2_when_it_is_the_default_parser_type() {
        MapperProbe probe = new JsonPath(JSON).using(new JsonPathConfig().defaultParserType(JsonParserType.JACKSON_2))
                .getObject("probe", MapperProbe.class);

        assertThat(probe.getMapper(), equalTo("jackson2"));
    }

    @Test
    void get_object_uses_gson_when_it_is_the_default_parser_type() {
        MapperProbe probe = new JsonPath(JSON).using(new JsonPathConfig().defaultParserType(JsonParserType.GSON))
                .getObject("probe", MapperProbe.class);

        assertThat(probe.getMapper(), equalTo("none"));
    }
}
