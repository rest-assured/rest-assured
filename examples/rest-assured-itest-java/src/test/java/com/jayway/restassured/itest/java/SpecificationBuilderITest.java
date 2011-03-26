/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.itest.java;

import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.itest.java.support.WithJetty;
import com.jayway.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionContaining.hasItems;

public class SpecificationBuilderITest extends WithJetty {

    @Test
    public void expectingSpecificationMergesTheCurrentSpecificationWithTheSuppliedOne() throws Exception {
        final ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectBody("store.book.size()", is(4)).expectStatusCode(200);
        final ResponseSpecification responseSpecification = builder.build();

        expect().
                specification(responseSpecification).
                body("store.book[0].author", equalTo("Nigel Rees")).
        when().
                get("/jsonStore");
    }

    @Test
    public void expectingSpecMergesTheCurrentSpecificationWithTheSuppliedOne() throws Exception {
        final ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectBody("store.book.size()", is(4)).expectStatusCode(200);
        final ResponseSpecification responseSpecification = builder.build();

        expect().
                spec(responseSpecification).
                body("store.book[0].author", equalTo("Nigel Rees")).
        when().
                get("/jsonStore");
    }

    @Test
    public void bodyExpectationsAreNotOverwritten() throws Exception {
        final ResponseSpecBuilder builder = new ResponseSpecBuilder();
        builder.expectBody("store.book.size()", is(4)).expectStatusCode(200);
        final ResponseSpecification responseSpecification = builder.build();

        expect().
                body("store.book.author", hasItems("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")).
                spec(responseSpecification).
                body("store.book[0].author", equalTo("Nigel Rees")).
        when().
                get("/jsonStore");
    }

    @Test
    public void responseSpecificationSupportsMergingWithAnotherResponseSpecification() throws Exception {
        final ResponseSpecification specification = expect().body("store.book.size()", equalTo(4));
        final ResponseSpecification built = new ResponseSpecBuilder().expectStatusCode(200).addResponseSpecification(specification).build();

        expect().
                body("store.book.author", hasItems("Nigel Rees", "Evelyn Waugh", "Herman Melville", "J. R. R. Tolkien")).
                spec(built).
                body("store.book[0].author", equalTo("Nigel Rees")).
        when().
                get("/jsonStore");
    }
}
