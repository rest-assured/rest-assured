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

package io.restassured.module.mockmvc.specification;

import java.util.concurrent.TimeUnit;

/**
 * Options available when sending a request using Mock MVC module.
 * Includes possibility to send an async request.
 *
 * @author Marcin Grzejszczak
 * @author Johan Haleby
 * @see MockMvcRequestSender
 */
public interface MockMvcRequestAsyncConfigurer extends MockMvcRequestSender {

    /**
     * Syntactic sugar
     */
    MockMvcRequestAsyncConfigurer with();

    /**
     * Syntactic sugar
     */
    MockMvcRequestAsyncConfigurer and();

    /**
     * Add timeout to async execution - takes precedence over the default setup
     *
     * @param duration The duration
     * @param timeUnit of the duration
     */
    MockMvcRequestAsyncConfigurer timeout(long duration, TimeUnit timeUnit);

    /**
     * Add timeout to async execution - takes precedence over the default setup
     *
     * @param durationInMs The duration in milliseconds
     */
    MockMvcRequestAsyncConfigurer timeout(long durationInMs);

    /**
     * Return back to request sender
     */
    MockMvcRequestSender then();

}