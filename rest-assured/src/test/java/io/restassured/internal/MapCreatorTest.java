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

package io.restassured.internal;

import io.restassured.internal.MapCreator.CollisionStrategy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.withArgs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class MapCreatorTest {

    @Test public void
    can_merge_map_keys_with_parameters() {
        Map<String, Object> map = MapCreator.createMapFromObjects(CollisionStrategy.MERGE, "key1.%s", withArgs("hello1"), equalTo("value1"), "key2.%s", withArgs("hello2"), equalTo("value2"));

        assertThat(map).isNotEmpty();
    }

    // Regression guard for #1867: Integer values used to trigger a Groovy operator-dispatch
    // failure on the `parameters.length % 2` check under Groovy 4.0.29.
    @Test public void
    can_create_map_from_integer_values() {
        Map<String, Object> map = MapCreator.createMapFromObjects(CollisionStrategy.MERGE, "a", 1, "b", 2);

        assertThat(map).containsEntry("a", 1).containsEntry("b", 2);
    }

}