package com.jayway.restassured.itest.java;

import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;

public class GivenWhenThenRootPathITest extends WithJetty {

    @Test public void
    given_when_then_works_with_root_path() {
         get("/jsonStore").then().assertThat().
                 root("store.%s", withArgs("book")).
                 body("category.size()", equalTo(4)).
                 appendRoot("%s.%s", withArgs("author", "size()")).
                 body(withNoArgs(), equalTo(4));
    }
}
