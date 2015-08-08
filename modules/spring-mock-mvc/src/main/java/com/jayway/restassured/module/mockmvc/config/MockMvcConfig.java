/*
 * Copyright 2015 the original author or authors.
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

package com.jayway.restassured.module.mockmvc.config;

import com.jayway.restassured.config.Config;

import java.util.Collections;
import java.util.List;

/**
 * General configuration of the Spring Mock MVC module
 */
public class MockMvcConfig implements Config {

    private final boolean userConfigured;
    private final boolean automaticallyApplySpringSecurityMockMvcConfigurer;

    /**
     * Creates a default {@link MockMvcConfig} that automatically applies the <code>SecurityMockMvcConfigurer</code> if available in classpath.
     */
    public MockMvcConfig() {
        this(true, false);
    }

    private MockMvcConfig(boolean shouldAutomaticallyApplySpringSecurityMockMvcConfigurer, boolean isUserConfigured) {
        this.automaticallyApplySpringSecurityMockMvcConfigurer = shouldAutomaticallyApplySpringSecurityMockMvcConfigurer;
        this.userConfigured = isUserConfigured;
    }

    /**
     * Instruct REST Assured Mock Mvc not to automatically apply the SpringSecurityMockMvcConfigurer even if it's available in the classpath.
     */
    public MockMvcConfig dontAutomaticallyApplySpringSecurityMockMvcConfigurer() {
        return new MockMvcConfig(false, true);
    }

    /**
     * @param shouldAutomaticallyApplySpringSecurityMockMvcConfigurer <code>true</code> if SecurityMockMvcConfigurer should be automatically applied if available in classpath, <code>false</code> otherwise.
     * @return a new instance of {@link MockMvcConfig}.
     */
    public MockMvcConfig automaticallyApplySpringSecurityMockMvcConfigurer(boolean shouldAutomaticallyApplySpringSecurityMockMvcConfigurer) {
        return new MockMvcConfig(shouldAutomaticallyApplySpringSecurityMockMvcConfigurer, true);
    }

    /**
     * Instruct REST Assured Mock Mvc to automatically apply the SpringSecurityMockMvcConfigurer even if it's available in the classpath.
     *
     * @return a new instance of {@link MockMvcConfig}.
     */
    public MockMvcConfig automaticallyApplySpringSecurityMockMvcConfigurer() {
        return new MockMvcConfig(true, true);
    }

    /**
     * @return A list of packages to scan for mock mvc configurers
     */
    public boolean shouldAutomaticallyApplySpringSecurityMockMvcConfigurer() {
        return automaticallyApplySpringSecurityMockMvcConfigurer;
    }

    public boolean isUserConfigured() {
        return userConfigured;
    }

    /**
     * Just syntactic sugar to make the DSL more english like.
     */
    public MockMvcConfig with() {
        return this;
    }

    /**
     * Just syntactic sugar.
     *
     * @return A new instance of {@link MockMvcConfig}.
     */
    public static MockMvcConfig mockMvcConfig() {
        return new MockMvcConfig();
    }

    private List<String> addToList(List<String> list, String firstPackage, String[] additionalPackages) {
        list.add(firstPackage);
        if (additionalPackages != null && additionalPackages.length > 0) {
            Collections.addAll(list, additionalPackages);
        }
        return list;
    }
}
