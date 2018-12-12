package io.restassured;

import io.restassured.RestAssured;
import io.restassured.authentication.FormAuthConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class RestAssuredTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void formInputNotNullNullNullOutputIllegalArgumentException() {
    // Arrange
    final String userName = "    ";
    final String password = null;
    final FormAuthConfig config = null;
    // Act
    thrown.expect(IllegalArgumentException.class);
    RestAssured.form(userName, password, config);
    // Method is not expected to return due to exception thrown
  }

  @Test
  public void formInputNullNotNullNullOutputIllegalArgumentException() {
    // Arrange
    final String userName = null;
    final String password = "";
    final FormAuthConfig config = null;
    // Act
    thrown.expect(IllegalArgumentException.class);
    RestAssured.form(userName, password, config);
    // Method is not expected to return due to exception thrown
  }
}
