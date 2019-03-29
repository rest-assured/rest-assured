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

package io.restassured.itest.java;

import io.restassured.RestAssured;
import io.restassured.itest.java.support.WithJetty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class RootPathITest extends WithJetty {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void specifyingRootPathInExpectationAddsTheRootPathForEachSubsequentBodyExpectation() {
        expect().
                rootPath("store.book").
                 body("category.size()", equalTo(4)).
                 body("author.size()", equalTo(4)).
        when().
                 get("/jsonStore");
    }

    @Test
    public void specifyingRootPathThatEndsWithDotAndBodyThatEndsWithDotWorks() {
        expect().
                rootPath("store.book.").
                 body(".category.size()", equalTo(4)).
                 body(".author.size()", equalTo(4)).
        when().
                 get("/jsonStore");
    }

    @Test
    public void specifyingRootPathThatEndsWithDotAndBodyThatDoesntEndWithDotWorks() {
        expect().
                rootPath("store.book.").
                 body("category.size()", equalTo(4)).
                 body("author.size()", equalTo(4)).
        when().
                 get("/jsonStore");
    }

    @Test
    public void specifyingRootPathThatDoesntEndWithDotAndBodyThatEndsWithDotWorks() {
        expect().
                rootPath("store.book").
                 body(".category.size()", equalTo(4)).
                 body(".author.size()", equalTo(4)).
        when().
                 get("/jsonStore");
    }

    @Test
    public void specifyingRootPathAndBodyThatStartsWithArrayIndexingWorks() {
        expect().
                rootPath("store.book").
                 body("[0].category", either(equalTo("reference")).or(equalTo("fiction"))).
        when().
                get("/jsonStore");
    }

    @Test
    public void specifyingRootPathThatAndEmptyPathWorks() {
        expect().
                rootPath("store.book.category.size()").
                 body("", equalTo(4)).
        when().
                 get("/jsonStore");
    }

    @Test
    public void specifyingEmptyRootPathResetsToDefaultRootObject() {
        expect().
                rootPath("store.book").
                 body("category.size()", equalTo(4)).
                 body("author.size()", equalTo(4)).
                rootPath("").
                 body("store.book.category.size()", equalTo(4)).
        when().
                 get("/jsonStore");
    }

    @Test
    public void whenNotSpecifyingExplicitRootPathThenDefaultRootPathIsUsed() {
        rootPath = "store.book";
        try {
            expect().
                     body("category.size()", equalTo(4)).
                     body("author.size()", equalTo(4)).
            when().
                     get("/jsonStore");
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    public void resetSetsRootPathToEmptyString() {
        rootPath = "store.book";

        RestAssured.reset();

        assertThat(rootPath, equalTo(""));
    }

    @Test
    public void specifyingRootPathWithBodyArgs() {
        expect().
                rootPath("store.book.category[%d]").
                body(withArgs(0), equalTo("reference")).
                body(withArgs(1), equalTo("fiction")).
        when().
                get("/jsonStore");
    }

    @Test
    public void specifyingRootPathWithMultipleBodyArgs() {
        final String category = "category";
        expect().
                rootPath("store.book.%s[%d]").
                body(withArgs(category, 0), equalTo("reference")).
                body(withArgs(category, 1), equalTo("fiction")).
        when().
                get("/jsonStore");
    }

    @Test
    public void specifyingRootPathWithMultipleContentArguments() {
        final String category = "category";
        expect().
                rootPath("store.book.%s[%d]").
                body(withArgs(category, 0), equalTo("reference")).
                body(withArgs(category, 1), equalTo("fiction")).
        when().
                get("/jsonStore");
    }

    @Test
    public void specifyingRootPathInMultiBodyAddsTheRootPathForEachExpectation() {
        expect().
                rootPath("store.book").
                 body(
                    "category.size()", equalTo(4),
                    "author.size()", equalTo(4)
                 ).
        when().
                 get("/jsonStore");
    }

    @Test
    public void specifyingRootPathInMultiContentAddsTheRootPathForEachExpectation() {
        expect().
                rootPath("store.book").
                 body(
                    "category.size()", equalTo(4),
                    "author.size()", equalTo(4)
                 ).
        when().
                 get("/jsonStore");
    }

    @Test
    public void specifyingRootPathWithArguments() {
        expect().
                 rootPath("store.%s", withArgs("book")).
                 body(
                    "category.size()", equalTo(4),
                    "author.size()", equalTo(4)
                 ).
        when().
                 get("/jsonStore");
    }

    @Test
    public void appendingRootPathWithoutArgumentsWorks() {
        expect().
                 rootPath("store.%s", withArgs("book")).
                 body("category.size()", equalTo(4)).
                 appendRootPath("author").
                 body("size()", equalTo(4)).
        when().
                 get("/jsonStore");
    }

    @Test
    public void appendingRootPathWithArgumentsWorks() {
        expect().
                 rootPath("store.%s", withArgs("book")).
                 body("category.size()", equalTo(4)).
                 appendRootPath("%s.%s", withArgs("author", "size()")).
                 body(withNoArgs(), equalTo(4)).
        when().
                 get("/jsonStore");
    }

    @Test
    public void canAppendRootPathToEmptyRootPath() {
        expect().
                appendRootPath("store.%s").
                body(withArgs("book.category.size()"), equalTo(4)).
        when().
                get("/jsonStore");
    }

    @Test
    public void usingBodyExpectationWithoutPath() {
        expect().
                 rootPath("store.%s").
                 body(withArgs("book.category.size()"), equalTo(4)).
        when().
                 get("/jsonStore");
    }

    @Test
    public void cannotUseBodyExpectationWithNoPathWhenRootPathIsEmpty() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Cannot specify arguments when root path is empty");

        expect().body(withArgs("author", "size()"), equalTo("Something"));
    }

    @Test
    public void cannotDetachRootPathToFromRootPath() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Cannot detach path when root path is empty");

        expect().detachRootPath("path");
    }

    @Test
    public void detachingRootPathWorksWithOldSyntax() {
        expect().
                rootPath("store.%s", withArgs("book")).
                body("category.size()", equalTo(4)).
                detachRootPath("book").
                body("size()", equalTo(2)).
        when().
                get("/jsonStore");
    }

    @Test
    public void detachingRootPathWorksWithNewSyntax() {
        when().
                get("/jsonStore").
        then().
                rootPath("store.%s", withArgs("book")).
                body("category.size()", equalTo(4)).
                detachRootPath("book").
                body("size()", equalTo(2));
    }

    @Test
    public void detachingRootPathWorksWhenSpecifyingDot() {
        when().
                get("/jsonStore").
        then().
                rootPath("store.%s", withArgs("book")).
                body("category.size()", equalTo(4)).
                detachRootPath(".book").
                body("size()", equalTo(2));
    }

    @Test
    public void detachingRootPathThrowsISERootPathDoesntEndWithPathToDetach() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Cannot detach path 'another' since root path 'store.book' doesn't end with 'another'.");

        when().
                get("/jsonStore").
        then().
                rootPath("store.%s", withArgs("book")).
                body("category.size()", equalTo(4)).
                detachRootPath("another").
                body("size()", equalTo(2));
    }

    @Test
    public void supportsAppendingArgumentsDefinedInAppendRootAtALaterStage() {
        when().
                 get("/jsonStore").
        then().
                 rootPath("store.%s", withArgs("book")).
                 body("category.size()", equalTo(4)).
                 appendRootPath("%s.%s", withArgs("author")).
                 body(withArgs("size()"), equalTo(4));
    }

    @Test
    public void supportsAppendingArgumentsDefinedInRootAtALaterStage() {
        when().
                 get("/jsonStore").
        then().
                 rootPath("store.%s.%s", withArgs("book")).
                 body("size()", withArgs("category"), equalTo(4));
    }

    @Test
    public void supportsAppendingArgumentsDefinedInRootAtALaterStageInMultiExpectationBlocks() {
        when().
                get("/jsonStore").
        then().
                rootPath("store.book.find { it.author == '%s' }").
                body(
                        "price", withArgs("Nigel Rees"), is(8.95f),
                        "price", withArgs("Evelyn Waugh"), is(12.99f),
                        "price", withArgs("J. R. R. Tolkien"), is(22.99f),
                        "title", withArgs("J. R. R. Tolkien"), is("The Lord of the Rings")
                );
    }
}