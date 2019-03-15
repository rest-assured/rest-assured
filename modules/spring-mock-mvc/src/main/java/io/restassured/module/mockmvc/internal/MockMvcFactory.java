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

package io.restassured.module.mockmvc.internal;

import io.restassured.module.mockmvc.config.MockMvcConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.AbstractMockMvcBuilder;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.web.context.WebApplicationContext;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MockMvcFactory {

    private Object mockMvc;

    MockMvcFactory() {
        mockMvc = null;
    }

    public MockMvcFactory(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public MockMvcFactory(MockMvcBuilder mockMvc) {
        this.mockMvc = mockMvc;
    }

    public synchronized MockMvc build(final MockMvcConfig config) {
        final MockMvc mockMvcToReturn;
        if (!isAssigned()) {
            throw new IllegalStateException("You haven't configured a MockMVC instance. You can do this statically\n\nRestAssuredMockMvc.mockMvc(..)\nRestAssuredMockMvc.standaloneSetup(..);\nRestAssuredMockMvc.webAppContextSetup(..);\n\nor using the DSL:\n\ngiven().\n\t\tmockMvc(..). ..\n");
        } else if (mockMvc instanceof MockMvc) {
            mockMvcToReturn = (MockMvc) this.mockMvc;
        } else if (mockMvc instanceof AbstractMockMvcBuilder) {
            AbstractMockMvcBuilder builder = (AbstractMockMvcBuilder) this.mockMvc;
            if (config.shouldAutomaticallyApplySpringSecurityMockMvcConfigurer() && SpringSecurityTestClassPathChecker.isSpringSecurityTestInClasspath()) {
                // Avoid duplicates if possible
                List<MockMvcConfigurer> configurers = getAlreadyConfiguredMockMacConfigurers(builder);
                boolean isOkToAdd = true;

                Iterator<MockMvcConfigurer> iterator = configurers.iterator();
                while (iterator.hasNext()) {
                    MockMvcConfigurer configurer = iterator.next();
                    if (configurer.getClass().getName().equals("org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurer")) {
                        isOkToAdd = false;
                        break;
                    } else if (configurer instanceof ConditionalSpringMockMvcConfigurer) {
                        // Remove to avoid duplicates
                        iterator.remove();
                    }
                }
                // End avoid duplicates
                if (isOkToAdd) {
                    final MockMvcConfigurer configurer = org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity();
                    builder.apply(new ConditionalSpringMockMvcConfigurer(configurer));
                }
            }
            mockMvcToReturn = builder.build();
        } else if (mockMvc instanceof MockMvcBuilder) {
            mockMvcToReturn = ((MockMvcBuilder) mockMvc).build();
        } else {
            throw new IllegalStateException("Cannot construct MockMvc instance because mock mvc instance is of type " + mockMvc.getClass().getName());
        }

        // We cache the created MockMvc instance
        this.mockMvc = mockMvcToReturn;
        return mockMvcToReturn;
    }

    @SuppressWarnings("unchecked")
    private List<MockMvcConfigurer> getAlreadyConfiguredMockMacConfigurers(AbstractMockMvcBuilder builder) {
        try {
            Field configurers = AbstractMockMvcBuilder.class.getDeclaredField("configurers");
            configurers.setAccessible(true);
            Object o = configurers.get(builder);
            configurers.setAccessible(false);
            return (List<MockMvcConfigurer>) o;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public boolean isAssigned() {
        return mockMvc != null;
    }

    /**
     * Delegates to the {@link MockMvcConfigurer} only if the <code>springSecurityFilterChain</code> bean is available in the context.
     * The reason for checking this is that SpringSecurity may not be available if starting from a standalone setup.
     */
    private static class ConditionalSpringMockMvcConfigurer implements MockMvcConfigurer {
        private static final String SPRING_SECURITY_FILTER_CHAIN = "springSecurityFilterChain";
        private final MockMvcConfigurer configurer;

        public ConditionalSpringMockMvcConfigurer(MockMvcConfigurer configurer) {
            this.configurer = configurer;
        }

        public void afterConfigurerAdded(ConfigurableMockMvcBuilder<?> builder) {
            configurer.afterConfigurerAdded(builder);
        }

        public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder, WebApplicationContext context) {
            if (!context.containsBean(SPRING_SECURITY_FILTER_CHAIN)) {
                return null;
            }
            return configurer.beforeMockMvcCreated(builder, context);
        }
    }
}
