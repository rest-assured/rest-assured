package com.jayway.restassured


import org.junit.Test;
import static com.jayway.restassured.RestAssured.map
import static groovy.util.GroovyTestCase.assertEquals
import static org.junit.Assert.assertTrue

class MapBuilderTest {

  @Test(expected = IllegalArgumentException.class)
  def void mapThrowIAEWhenNoKeyValuePairsAreSupplied() throws Exception {
    map();
  }

  @Test(expected = IllegalArgumentException.class)
  def void mapThrowIAEWhenNoValuesAreSupplied() throws Exception {
    map("key");
  }

  @Test(expected = IllegalArgumentException.class)
  def void mapThrowIAEWhenOddNumberOfStringsAreSupplied() throws Exception {
    map("key1", "value1", "key2");
  }

  @Test
  def void mapBuildsAMapBasedOnTheSuppliedKeysAndValues() throws Exception {
    def map = map("key1", "value1", "key2", 3);

    assertEquals 2, map.size()
    assertEquals "value1", map.get("key1")
    assertEquals 3, map.get("key2")
  }
}
