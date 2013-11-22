/*
 * Copyright 2013 the original author or authors.
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

import com.jayway.restassured.itest.java.support.WithJetty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.*;

public class ErrorMessageITest extends WithJetty {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test public void
    shows_all_failing_json_path_expectations() {
        exception.expect(AssertionError.class);
        exception.expectMessage("2 expectations failed.\n" +
                "JSON path lotto.lottoId doesn't match.\n" +
                "Expected: a value less than <2>\n" +
                "  Actual: 5\n" +
                "\n" +
                "JSON path lotto.winning-numbers doesn't match.\n" +
                "Expected: a collection containing <21>\n" +
                "  Actual: [2, 45, 34, 23, 7, 5, 3]");

        expect().
                body("lotto.lottoId", lessThan(2)).
                body("lotto.winning-numbers", hasItem(21)).
        when().
                get("/lotto");
    }

    @Test public void
    shows_only_failing_expectations() {
        exception.expect(AssertionError.class);
        exception.expectMessage("2 expectations failed.\n" +
                "JSON path lotto.lottoId doesn't match.\n" +
                "Expected: a value less than <2>\n" +
                "  Actual: 5\n" +
                "\n" +
                "JSON path lotto.winning-numbers doesn't match.\n" +
                "Expected: a collection containing <21>\n" +
                "  Actual: [2, 45, 34, 23, 7, 5, 3]");

        expect().
                body("lotto.lottoId", greaterThan(4)).
                body("lotto.lottoId", lessThan(2)).
                body("lotto.winning-numbers", hasItem(21)).
        when().
                get("/lotto");
    }

    @Test public void
    error_message_look_ok_when_mixing_body_and_status_code_errors() {
        exception.expect(AssertionError.class);
        exception.expectMessage("3 expectations failed.\n" +
                "Expected status code <201> doesn't match actual status code <200>.\n" +
                "\n" +
                "JSON path lotto.lottoId doesn't match.\n" +
                "Expected: a value less than <2>\n" +
                "  Actual: 5\n" +
                "\n" +
                "JSON path lotto.winning-numbers doesn't match.\n" +
                "Expected: a collection containing <21>\n" +
                "  Actual: [2, 45, 34, 23, 7, 5, 3]");

        expect().
                statusCode(201).
                body("lotto.lottoId", greaterThan(4)).
                body("lotto.lottoId", lessThan(2)).
                body("lotto.winning-numbers", hasItem(21)).
        when().
                get("/lotto");
    }

    @Test public void
    error_message_with_failed_xpath_expected_looks_ok() {
        exception.expect(AssertionError.class);
        exception.expectMessage(equalTo("1 expectation failed.\n"+
                "Expected: an XML document with XPath /greeting/name/firstName[text()='John2']\n" +
                "  Actual: <greeting>\n" +
                "      <name>\n" +
                "        <firstName>John</firstName>\n" +
                "        <lastName>Doe</lastName>\n" +
                "      </name>\n" +
                "    </greeting>\n"));

        expect().body(hasXPath("/greeting/name/firstName[text()='John2']")).with().parameters("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }

    @Test public void
    error_message_with_failed_plain_body_expectation_looks_ok() {
        exception.expect(AssertionError.class);
        exception.expectMessage("1 expectation failed.\n" +
                "Response body doesn't match expectation.\n" +
                "Expected: a string containing \"something\"\n" +
                "  Actual: <greeting>\n" +
                "      <name>\n" +
                "        <firstName>John</firstName>\n" +
                "        <lastName>Doe</lastName>\n" +
                "      </name>\n" +
                "    </greeting>\n");

        expect().body(containsString("something")).with().parameters("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }
}