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

package io.restassured.module.mockmvc.config;

import io.restassured.config.Config;
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification;
import io.restassured.module.spring.commons.config.ClientConfig;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

/**
 * General configuration of the Spring Mock MVC module
 */
public class MockMvcConfig implements ClientConfig, Config {

    private final boolean userConfigured;
    private final boolean automaticallyApplySpringSecurityMockMvcConfigurer;
    private final boolean automaticallyApplySpringRestDocsMockMvcSupport;

    /**
     * Creates a default {@link MockMvcConfig} that automatically applies the <code>SecurityMockMvcConfigurer</code> if available in classpath.
     */
    public MockMvcConfig() {
        this(true, true, false);
    }

    private MockMvcConfig(boolean shouldAutomaticallyApplySpringSecurityMockMvcConfigurer, boolean automaticallyApplySpringRestDocsMockMvcSupport,
                          boolean isUserConfigured) {
        this.automaticallyApplySpringSecurityMockMvcConfigurer = shouldAutomaticallyApplySpringSecurityMockMvcConfigurer;
        this.automaticallyApplySpringRestDocsMockMvcSupport = automaticallyApplySpringRestDocsMockMvcSupport;
        this.userConfigured = isUserConfigured;
    }

    /**
     * Instruct REST Assured Mock Mvc not to automatically apply the SpringSecurityMockMvcConfigurer even if it's available in the classpath.
     */
    public MockMvcConfig dontAutomaticallyApplySpringSecurityMockMvcConfigurer() {
        return new MockMvcConfig(false, automaticallyApplySpringRestDocsMockMvcSupport, true);
    }

    /**
     * Instruct REST Assured Mock Mvc not to automatically apply support for <code>spring-restdocs-mockmvc</code> even if it's available in the classpath.
     */
    public MockMvcConfig dontAutomaticallyApplySpringRestDocsMockMvcSupport() {
        return new MockMvcConfig(automaticallyApplySpringSecurityMockMvcConfigurer, false, true);
    }

    /**
     * Instructs REST Assured to automatically apply the SecurityMockMvcConfigurer that allows for Spring Security testing. This only works if
     * <code>spring-security-test</code> is in the classpath and you've configured Rest Assured Mock MVC an instance of {@link org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder}.
     * For example by using {@link MockMvcRequestSpecification#webAppContextSetup(WebApplicationContext, MockMvcConfigurer...)} or
     * {@link MockMvcRequestSpecification#standaloneSetup(MockMvcBuilder)}.
     *
     * @param shouldAutomaticallyApplySpringSecurityMockMvcConfigurer <code>true</code> if SecurityMockMvcConfigurer should be automatically applied if available in classpath, <code>false</code> otherwise.
     * @return a new instance of {@link MockMvcConfig}.
     */
    public MockMvcConfig automaticallyApplySpringSecurityMockMvcConfigurer(boolean shouldAutomaticallyApplySpringSecurityMockMvcConfigurer) {
        return new MockMvcConfig(shouldAutomaticallyApplySpringSecurityMockMvcConfigurer, automaticallyApplySpringRestDocsMockMvcSupport, true);
    }

    /**
     * Instructs REST Assured whether or not to automatically support <code>spring-restdocs-mockmvc</code> if available in classpath.
     *
     * @param automaticallyApplySpringRestDocsMockMvcSupport <code>true</code> if Spring Rest Docs MockMvc support should be automatically applied if available in classpath, <code>false</code> otherwise.
     * @return a new instance of {@link MockMvcConfig}.
     */
    public MockMvcConfig automaticallyApplySpringRestDocsMockMvcSupport(boolean automaticallyApplySpringRestDocsMockMvcSupport) {
        return new MockMvcConfig(automaticallyApplySpringSecurityMockMvcConfigurer, automaticallyApplySpringRestDocsMockMvcSupport, true);
    }

    /**
     * Instruct REST Assured Mock Mvc to automatically apply the SpringSecurityMockMvcConfigurer if it's available in the classpath.
     *
     * @return a new instance of {@link MockMvcConfig}.
     * @see #automaticallyApplySpringSecurityMockMvcConfigurer(boolean)
     */
    public MockMvcConfig automaticallyApplySpringSecurityMockMvcConfigurer() {
        return new MockMvcConfig(true, automaticallyApplySpringRestDocsMockMvcSupport, true);
    }

    /**
     * Instruct REST Assured Mock Mvc to automatically apply support for <code>spring-restdocs-mockmvc</code> if it's available in the classpath.
     *
     * @return a new instance of {@link MockMvcConfig}.
     * @see #automaticallyApplySpringRestDocsMockMvcSupport(boolean)
     */
    public MockMvcConfig automaticallyApplySpringRestDocsMockMvcSupport() {
        return new MockMvcConfig(automaticallyApplySpringSecurityMockMvcConfigurer, true, true);
    }

    /**
     * @return whether or not REST Assured should automatically try to apply support for Spring Security test if it's available in classpath
     */
    public boolean shouldAutomaticallyApplySpringSecurityMockMvcConfigurer() {
        return automaticallyApplySpringSecurityMockMvcConfigurer;
    }

    /**
     * @return whether or not REST Assured should automatically try to apply support for <code>spring-restdocs-mockmvc</code> if it's available in classpath
     */
    public boolean shouldAutomaticallyApplySpringRestDocsMockMvcSupport() {
        return automaticallyApplySpringRestDocsMockMvcSupport;
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
}
