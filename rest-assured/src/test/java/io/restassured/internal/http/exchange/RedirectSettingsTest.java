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

package io.restassured.internal.http.exchange;

import io.restassured.config.RedirectConfig;
import io.restassured.internal.http.exchange.RedirectSettings.Overrides;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RedirectSettingsTest {

    @Test
    void without_overrides_everything_comes_from_the_config() {
        RedirectConfig config = new RedirectConfig().followRedirects(false).maxRedirects(7).allowCircularRedirects(true)
                .rejectRelativeRedirect(true).followRedirectsForAllMethods(true).stripSensitiveHeadersOnCrossHostRedirect(false);

        assertThat(new Overrides().resolve(config)).isEqualTo(new RedirectSettings(false, 7, true, true, true, false));
    }

    @Test
    void the_defaults_are_used_without_a_config() {
        assertThat(new Overrides().resolve(null)).isEqualTo(new Overrides().resolve(new RedirectConfig()));
        assertThat(new Overrides().resolve(null)).isEqualTo(new RedirectSettings(true, 100, false, false, false, true));
    }

    @Test
    void an_override_outranks_the_config() {
        Overrides overrides = new Overrides();
        overrides.follow(false);
        overrides.max(3);
        overrides.allowCircular(true);
        overrides.rejectRelative(true);

        assertThat(overrides.resolve(new RedirectConfig().followRedirects(true).maxRedirects(7)))
                .isEqualTo(new RedirectSettings(false, 3, true, true, false, true));
    }

    @Test
    void follow_all_methods_and_stripping_only_come_from_the_config() {
        RedirectConfig config = new RedirectConfig().followRedirectsForAllMethods(true).stripSensitiveHeadersOnCrossHostRedirect(false);

        RedirectSettings settings = new Overrides().resolve(config);

        assertThat(settings.followAllMethods()).isTrue();
        assertThat(settings.stripSensitiveHeaders()).isFalse();
    }

    @Test
    void a_copy_doesnt_change_with_the_original() {
        Overrides overrides = new Overrides();
        overrides.max(3);

        Overrides copy = overrides.copy();
        overrides.max(5);
        overrides.follow(false);

        assertThat(copy.resolve(null).max()).isEqualTo(3);
        assertThat(copy.resolve(null).follow()).isTrue();
    }
}
