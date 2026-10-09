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

package io.restassured.internal.common.assertion;

/**
 * A {@link PathFragmentEscaper} that is is specific to path fragments that consists of one or more hyphens
 */
public abstract class HyphenQuoteFragmentEscaper implements PathFragmentEscaper {
    private static final String INDEX_START_CHAR = "[";
    private static final String INDEX_END_CHAR = "]";

    @Override
    public String escape(String pathFragment) {
        // Check if this path fragment contains reads an index from a collection (for example some-list[0])
        // If this is the case we should escape "some-list" but leave the index lookup ([0]) outside, i.e. 'some-list'[0]
        if (pathFragment.trim().endsWith(INDEX_END_CHAR)) {
            int indexOfStart = pathFragment.indexOf(INDEX_START_CHAR);
            if (indexOfStart > 1 && pathFragment.indexOf(INDEX_END_CHAR) > indexOfStart) {
                // Split at the first index so that chained lookups, such as some-list[0][1] or some-list[0..-1][0], stay outside the quotes
                String toEscape = pathFragment.substring(0, indexOfStart);
                String indexLookup = pathFragment.substring(indexOfStart);
                return doEscape(toEscape) + indexLookup;
            }
        }
        return doEscape(pathFragment);
    }

    private static String doEscape(String str) {
        return "'" + str + "'";
    }
}
