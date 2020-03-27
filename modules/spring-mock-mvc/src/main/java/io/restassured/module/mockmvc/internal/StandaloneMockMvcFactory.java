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

import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import java.util.ArrayList;
import java.util.List;

public class StandaloneMockMvcFactory {

    /**
     * Create a new {@link MockMvcFactory} with the supplied controllers or mock mvc configurers
     *
     * @param controllerOrMockMvcConfigurers Array of controllers or configurers
     * @return A new {@link MockMvcFactory}
     */
    public static MockMvcFactory of(Object[] controllerOrMockMvcConfigurers) {
        List<Object> controllers = new ArrayList<Object>();
        List<MockMvcConfigurer> configurers = new ArrayList<MockMvcConfigurer>();
        for (Object object : controllerOrMockMvcConfigurers) {
            if (object instanceof MockMvcConfigurer) {
                configurers.add((MockMvcConfigurer) object);
            } else {
                controllers.add(object);
            }
        }
        StandaloneMockMvcBuilder mockMvc = MockMvcBuilders.standaloneSetup(controllers.toArray());
        if (!configurers.isEmpty()) {
            for (MockMvcConfigurer configurer : configurers) {
                mockMvc.apply(configurer);
            }
        }
        return new MockMvcFactory(mockMvc);
    }
}
