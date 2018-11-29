package io.restassured.specification;

import io.restassured.specification.Argument;
import org.junit.Assert;
import org.junit.Test;

public class ArgumentTest {

  @Test
  public void equalsInputNullOutputFalse() {
    // Arrange
    final Argument objectUnderTest = new Argument(null);
    final Object o = null;
    // Act
    final boolean retval = objectUnderTest.equals(o);
    // Assert result
    Assert.assertEquals(false, retval);
  }
}
