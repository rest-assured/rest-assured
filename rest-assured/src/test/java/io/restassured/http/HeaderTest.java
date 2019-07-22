package io.restassured.http;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HeaderTest {

  @Test
  public void header_has_same_name_as_expected() {
    final Header header1 = new Header("foo", "bar");
    final Header header2 = new Header("Foo", "baz");

    assertThat(header2.hasSameNameAs(header1)).isTrue();
  }

  @Test
  public void header_does_not_have_same_name_as_expected() {
    final Header header1 = new Header("foo", "bar");
    final Header header2 = new Header("bar", "baz");

    assertThat(header2.hasSameNameAs(header1)).isFalse();
  }
}
