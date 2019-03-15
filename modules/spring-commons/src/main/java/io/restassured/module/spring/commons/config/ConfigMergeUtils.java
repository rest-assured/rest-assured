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
package io.restassured.module.spring.commons.config;

public class ConfigMergeUtils {

    public static SpecificationConfig mergeConfig(SpecificationConfig base, SpecificationConfig toMerge) {
        boolean thisIsUserConfigured = base.isUserConfigured();
        boolean otherIsUserConfigured = toMerge.isUserConfigured();
        if (!otherIsUserConfigured) {
            return base;
        }
        if (thisIsUserConfigured) {
            if (toMerge.getDecoderConfig().isUserConfigured()) {
                base = base.decoderConfig(toMerge.getDecoderConfig());
            }

            if (toMerge.getEncoderConfig().isUserConfigured()) {
                base = base.encoderConfig(toMerge.getEncoderConfig());
            }

            if (toMerge.getHeaderConfig().isUserConfigured()) {
                base = base.headerConfig(toMerge.getHeaderConfig());
            }

            if (toMerge.getJsonConfig().isUserConfigured()) {
                base = base.jsonConfig(toMerge.getJsonConfig());
            }

            if (toMerge.getLogConfig().isUserConfigured()) {
                base = base.logConfig(toMerge.getLogConfig());
            }

            if (toMerge.getObjectMapperConfig().isUserConfigured()) {
                base = base.objectMapperConfig(toMerge.getObjectMapperConfig());
            }

            if (toMerge.getSessionConfig().isUserConfigured()) {
                base = base.sessionConfig(toMerge.getSessionConfig());
            }

            if (toMerge.getXmlConfig().isUserConfigured()) {
                base = base.xmlConfig(toMerge.getXmlConfig());
            }

            if (toMerge.getAsyncConfig().isUserConfigured()) {
                base = base.asyncConfig(toMerge.getAsyncConfig());
            }

            if (toMerge.getMultiPartConfig().isUserConfigured()) {
                base = base.multiPartConfig(toMerge.getMultiPartConfig());
            }

            if (toMerge.getClientConfig().isUserConfigured()) {
                base = base.clientConfig(toMerge.getClientConfig());
            }

            if (toMerge.getParamConfig().isUserConfigured()) {
                base = base.paramConfig(toMerge.getParamConfig());
            }

            if (toMerge.getMatcherConfig().isUserConfigured()) {
                base = base.matcherConfig(toMerge.getMatcherConfig());
            }

            return base;
        } else {
            return toMerge;
        }
    }
}
