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

package io.restassured;

import io.restassured.authentication.FormAuthConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class RestAssuredTest {

  @Test
  @DisplayName("formInputNotNullNullNullOutputIllegalArgumentException")
  public void formInputNotNullNullNullOutputIllegalArgumentException() {
    // Arrange
    final String userName = "    ";
    final String password = null;
    final FormAuthConfig config = null;
    // Act
    Throwable thrown = catchThrowable(() -> RestAssured.form(userName, password, config));
    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("formInputNullNotNullNullOutputIllegalArgumentException")
  public void formInputNullNotNullNullOutputIllegalArgumentException() {
    // Arrange
    final String userName = null;
    final String password = "";
    final FormAuthConfig config = null;
    // Act
    Throwable thrown = catchThrowable(() -> RestAssured.form(userName, password, config));
    // Assert
    assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
  }
}
