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

package io.restassured.internal;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class UriValidatorTest {

    @Test public void
    returns_false_when_uri_is_empty() {
        assertThat(UriValidator.isUri(""), is(false));
    }

    @Test public void
    returns_false_when_uri_is_null() {
        assertThat(UriValidator.isUri(null), is(false));
    }

    @Test public void
    returns_false_when_uri_is_blank() {
        assertThat(UriValidator.isUri("   "), is(false));
    }

    @Test public void
    returns_false_when_uri_doesnt_contain_scheme() {
        assertThat(UriValidator.isUri("127.0.0.1"), is(false));
    }

    @Test public void
    returns_false_when_uri_doesnt_contain_host() {
        assertThat(UriValidator.isUri("http://"), is(false));
    }

    @Test public void
    returns_false_when_uri_is_malformed() {
        assertThat(UriValidator.isUri("&%!!"), is(false));
    }

    @Test public void
    returns_true_when_uri_contains_scheme_and_host() {
        assertThat(UriValidator.isUri("http://127.0.0.1"), is(true));
    }
}