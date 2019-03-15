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

import io.restassured.itest.java.support.WithJetty;
import org.junit.Test;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.get;
import static org.hamcrest.Matchers.*;

public class TextAndPathParsingCombinedITest extends WithJetty {

    @Test public void
    expectations_can_mix_text_and_path_validation() {
        expect().body(containsString("\"numbers\":[52")).and().body("lotto.winners.winnerId", hasItems(23, 54)).when().get("/lotto");
    }

    @Test public void
    expectations_can_mix_path_and_text_validation() {
        expect().body("lotto.winners.winnerId", hasItems(23, 54)).and().body(containsString("\"numbers\":[52")).when().get("/lotto");
    }

    @Test public void
    expectations_can_mix_text_followed_by_path_then_text_validation() {
        expect().body(containsString("\"numbers\":[52")).and().body("lotto.winners.winnerId", hasItems(23, 54)).and().body(containsString("lottoId")).when().get("/lotto");
    }

    @Test public void
    expectations_can_mix_path_followed_by_text_then_path_validation() {
        expect().body("lotto.winners.winnerId", hasItems(23, 54)).and().body(containsString("\"numbers\":[52")).and().body("lotto.lottoId", equalTo(5)).when().get("/lotto");
    }
    
    @Test public void
    assertions_can_mix_text_and_path_validation() {
        get("/lotto").then().body(containsString("\"numbers\":[52")).and().body("lotto.winners.winnerId", hasItems(23, 54));
    }

    @Test public void
    assertions_can_mix_path_and_text_validation() {
        get("/lotto").then().body("lotto.winners.winnerId", hasItems(23, 54)).and().body(containsString("\"numbers\":[52"));
    }

    @Test public void
    assertions_can_mix_text_followed_by_path_then_text_validation() {
        get("/lotto").then().body(containsString("\"numbers\":[52")).and().body("lotto.winners.winnerId", hasItems(23, 54)).and().body(containsString("lottoId"));
    }

    @Test public void
    assertions_can_mix_path_followed_by_text_then_path_validation() {
        get("/lotto").then().body("lotto.winners.winnerId", hasItems(23, 54)).and().body(containsString("\"numbers\":[52")).and().body("lotto.lottoId", equalTo(5));
    }
}
