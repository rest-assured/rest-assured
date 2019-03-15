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

package io.restassured.builder;

import io.restassured.internal.ResponseSpecificationImpl;
import org.junit.Test;

import static io.restassured.RestAssured.withArgs;
import static org.junit.Assert.assertEquals;

public class ResponseSpecBuilderPathTest {
    @Test
    public void rootPathShouldBeSet() {
        assertEquals("lotto", (
                (ResponseSpecificationImpl) new ResponseSpecBuilder()
                .rootPath("lotto")
                .build()).getRootPath());
    }

    @Test
    public void rootPathShouldOverwritePreviousRootPath() {
        assertEquals("lotto", (
                (ResponseSpecificationImpl) new ResponseSpecBuilder()
                .rootPath("nonExistentPath").rootPath("lotto")
                .build()).getRootPath());
    }

    @Test
    public void rootPathWithArgumentsShouldBeEvaluatedAndSet() {
        assertEquals("lotto.winners[1]", (
                (ResponseSpecificationImpl) new ResponseSpecBuilder()
                .rootPath("lotto.winners[%d]", withArgs(1))
                .build()).getRootPath());
    }

    @Test
    public void rootPathToAppendShouldBeAppendedToPreviousRootPath() {
        assertEquals("lotto.winners[1]", (
                (ResponseSpecificationImpl) new ResponseSpecBuilder()
                .rootPath("lotto").appendRootPath("winners[1]")
                .build()).getRootPath());
    }

    @Test
    public void rootPathWithArgumentsToAppendShouldBeAppendedToPreviousRootPath() {
        assertEquals("lotto.winners[1]", (
                (ResponseSpecificationImpl) new ResponseSpecBuilder()
                .rootPath("lotto").appendRootPath("winners[%d]", withArgs(1))
                .build()).getRootPath());
    }

    @Test
    public void rootPathShouldBeReset() {
        assertEquals("", (
                (ResponseSpecificationImpl) new ResponseSpecBuilder()
                .rootPath("lotto.winners[1]").noRootPath()
                .build()).getRootPath());
    }

    @Test
    public void rootPathShouldBeDetached() {
        assertEquals("lotto", (
                (ResponseSpecificationImpl) new ResponseSpecBuilder()
                .rootPath("lotto.winners[1]").detachRootPath("winners[1]")
                .build()).getRootPath());
    }
}
