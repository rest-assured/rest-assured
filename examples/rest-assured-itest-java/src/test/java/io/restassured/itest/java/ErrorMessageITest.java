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

import io.restassured.config.MatcherConfig;
import io.restassured.itest.java.support.WithJetty;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.restassured.RestAssured.*;
import static io.restassured.config.MatcherConfig.ErrorDescriptionType.HAMCREST;
import static org.hamcrest.Matchers.*;

public class ErrorMessageITest extends WithJetty {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test public void
    shows_all_failing_json_path_expectations_when_using_legacy_syntax() {
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
                "Expected status code <201> but was <200>.\n" +
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

        expect().body(hasXPath("/greeting/name/firstName[text()='John2']")).with().params("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
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

        expect().body(containsString("something")).with().params("firstName", "John", "lastName", "Doe").get("/anotherGreetXML");
    }

    @Test public void
    rest_assured_is_configurable_to_use_default_hamcrest_error_messages() {
        exception.expect(AssertionError.class);
        exception.expectMessage("JSON path lotto.lottoId doesn't match.\n" +
                "\n" +
                "Expected: a value less than <2>\n" +
                "  Actual: <5> was greater than <2>");

        given().
                config(config().matcherConfig(new MatcherConfig(HAMCREST))).
        when().
                get("/lotto").
        then().
                statusCode(200).
                body("lotto.lottoId", lessThan(2));
    }

    @Test public void
    shows_all_failing_json_path_expectations_when_single_body_with_new_syntax() {
        exception.expect(AssertionError.class);
        exception.expectMessage("2 expectations failed.\n" +
                "JSON path lotto.lottoId doesn't match.\n" +
                "Expected: a value less than <2>\n" +
                "  Actual: 5\n" +
                "\n" +
                "JSON path lotto.winning-numbers doesn't match.\n" +
                "Expected: a collection containing <21>\n" +
                "  Actual: [2, 45, 34, 23, 7, 5, 3]");

        when().
                get("/lotto").
        then().
                body("lotto.lottoId", lessThan(2),
                     "lotto.winning-numbers", hasItem(21));
    }

    /**
     * Makes sure that <a href="https://github.com/jayway/rest-assured/issues/668">issue 668</a> is resolved
     */
    @Test public void
    throws_nice_error_messages_when_json_path_property_doesnt_exist() {
        exception.expect(AssertionError.class);
        exception.expectMessage("1 expectation failed.\n" +
                "JSON path lotto1.lottoId doesn't match.\n" +
                "Expected: a value less than <2>\n" +
                "  Actual: null");

        when().
                get("/lotto").
        then().
                statusCode(200).
                body("lotto1.lottoId", lessThan(2));
    }
}