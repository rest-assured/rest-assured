/*
 * Copyright 2026 the original author or authors.
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

import io.restassured.config.RedirectConfig;
import io.restassured.internal.http.exchange.RedirectSettings;
import io.restassured.internal.http.exchange.RedirectSettings.Overrides;
import io.restassured.specification.RedirectSpecification;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class RedirectSpecificationImplTest {

    @Test
    void each_method_sets_its_override_and_returns_the_request_specification() {
        RequestSpecification spec = given();
        Overrides overrides = new Overrides();
        RedirectSpecification redirects = new RedirectSpecificationImpl(spec, overrides);

        assertThat(redirects.max(3)).isSameAs(spec);
        assertThat(redirects.follow(false)).isSameAs(spec);
        assertThat(redirects.allowCircular(true)).isSameAs(spec);
        assertThat(redirects.rejectRelative(true)).isSameAs(spec);

        assertThat(overrides.resolve(new RedirectConfig()))
                .isEqualTo(new RedirectSettings(false, 3, true, true, false, true));
    }

    @Test
    void later_calls_replace_earlier_values() {
        Overrides overrides = new Overrides();
        RedirectSpecification redirects = new RedirectSpecificationImpl(given(), overrides);

        redirects.max(3);
        redirects.max(5);

        assertThat(overrides.resolve(new RedirectConfig()).max()).isEqualTo(5);
    }

    @Test
    void what_isnt_set_comes_from_the_redirect_config() {
        Overrides overrides = new Overrides();
        new RedirectSpecificationImpl(given(), overrides).max(5);

        assertThat(overrides.resolve(new RedirectConfig().followRedirects(false).allowCircularRedirects(true)))
                .isEqualTo(new RedirectSettings(false, 5, true, false, false, true));
    }

    @Test
    void redirects_writes_to_the_redirect_overrides_of_the_request_specification() throws Exception {
        RequestSpecification spec = given();

        RequestSpecification returned = spec.redirects().max(2).redirects().follow(true);

        assertThat(returned).isSameAs(spec);
        Field field = RequestSpecificationImpl.class.getDeclaredField("redirects");
        field.setAccessible(true);
        Overrides overrides = (Overrides) field.get(spec);
        RedirectSettings settings = overrides.resolve(new RedirectConfig().followRedirects(false).maxRedirects(7));
        assertThat(settings.max()).isEqualTo(2);
        assertThat(settings.follow()).isTrue();
    }
}
