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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.regex.Pattern;

public class AssertionSupport {

    private static final char CLOSURE_START_FRAGMENT = '{';
    private static final char CLOSURE_END_FRAGMENT = '}';
    private static final char LIST_GETTER_FRAGMENT = '(';
    private static final char LIST_INDEX_START_FRAGMENT = '[';
    private static final char LIST_INDEX_END_FRAGMENT = ']';
    private static final char SPACE = ' ';
    // Splits after each single quote, keeping the quote at the end of the preceding fragment
    private static final Pattern quoteSeparator = Pattern.compile("(?<=')");
    // A dot that isn't inside a list index, such as the range in some-list[0..-1]
    private static final Pattern pathSeparator = Pattern.compile("\\.(?![^\\[\\]]*\\])");

    public static String escapePath(String key, PathFragmentEscaper... pathFragmentEscapers) {
        String[] pathFragments = quoteSeparator.split(key);
        for (int i = 0; i < pathFragments.length; i++) {
            String pathFragment = pathFragments[i];
            if (!pathFragment.endsWith("'") || pathFragment.contains("**")) {
                String[] dotFragments = pathSeparator.split(pathFragment);
                for (int k = 0; k < dotFragments.length; k++) {
                    String dotFragment = dotFragments[k];
                    for (PathFragmentEscaper escaper : pathFragmentEscapers) {
                        if (escaper.shouldEscape(dotFragment)) {
                            dotFragments[k] = escaper.escape(dotFragment.trim());
                            break;
                        }
                    }
                }
                pathFragments[i] = joinDotFragments(dotFragments);
            }
        }
        return String.join("", pathFragments);
    }

    private static String joinDotFragments(String[] fragments) {
        if (fragments.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(fragments[0]);
        for (int i = 1; i < fragments.length; i++) {
            String frag = fragments[i];
            // If the fragment starts with '[' it's already an index expression
            // like ['properties']; do NOT prefix it with a dot.
            if (frag.startsWith("[")) {
                sb.append(frag);
            } else {
                sb.append('.').append(frag);
            }
        }
        return sb.toString();
    }

    public static HyphenQuoteFragmentEscaper hyphen() {
        return new HyphenQuoteFragmentEscaper() {
            @Override
            public boolean shouldEscape(String pathFragment) {
                return !pathFragment.startsWith("'") && !pathFragment.endsWith("'") && StringUtils.substringBefore(pathFragment, "[").contains("-") && !containsAny(pathFragment, CLOSURE_START_FRAGMENT, CLOSURE_END_FRAGMENT, LIST_GETTER_FRAGMENT);
            }
        };
    }

    public static EndToEndQuoteFragmentEscaper properties() {
        return new EndToEndQuoteFragmentEscaper() {
            @Override
            public boolean shouldEscape(String pathFragment) {
                return !pathFragment.startsWith("'") && !pathFragment.endsWith("'") && pathFragment.contains("properties") && !containsAny(pathFragment, CLOSURE_START_FRAGMENT, CLOSURE_END_FRAGMENT, LIST_GETTER_FRAGMENT, LIST_INDEX_START_FRAGMENT, SPACE, LIST_INDEX_END_FRAGMENT);
            }
        };
    }

    public static GetAtPathFragmentEscaper classKeyword() {
        return new GetAtPathFragmentEscaper() {
            @Override
            public boolean shouldEscape(String pathFragment) {
                return !pathFragment.startsWith("'") && !pathFragment.endsWith("'") && pathFragment.contains("class") && !containsAny(pathFragment, CLOSURE_START_FRAGMENT, CLOSURE_END_FRAGMENT, LIST_GETTER_FRAGMENT, LIST_INDEX_START_FRAGMENT, SPACE, LIST_INDEX_END_FRAGMENT);
            }
        };
    }

    public static EndToEndQuoteFragmentEscaper attributeGetter() {
        return new EndToEndQuoteFragmentEscaper() {
            @Override
            public boolean shouldEscape(String pathFragment) {
                return pathFragment.startsWith("@") && !pathFragment.endsWith("'") && !containsAny(pathFragment, CLOSURE_START_FRAGMENT, CLOSURE_END_FRAGMENT, SPACE);
            }
        };
    }

    public static EndToEndQuoteFragmentEscaper doubleStar() {
        return new EndToEndQuoteFragmentEscaper() {
            @Override
            public boolean shouldEscape(String pathFragment) {
                return "**".equals(pathFragment);
            }
        };
    }

    public static HyphenQuoteFragmentEscaper colon() {
        return new HyphenQuoteFragmentEscaper() {
            @Override
            public boolean shouldEscape(String pathFragment) {
                return !pathFragment.startsWith("'") && !pathFragment.endsWith("'") && pathFragment.contains(":") && !containsAny(pathFragment, CLOSURE_START_FRAGMENT, CLOSURE_END_FRAGMENT, LIST_GETTER_FRAGMENT);
            }
        };
    }

    public static EndToEndQuoteFragmentEscaper integer() {
        return new EndToEndQuoteFragmentEscaper() {
            @Override
            public boolean shouldEscape(String pathFragment) {
                return (startsWithDigit(pathFragment) || NumberUtils.isDigits(pathFragment)) && !containsAny(pathFragment, CLOSURE_START_FRAGMENT, CLOSURE_END_FRAGMENT, SPACE, LIST_GETTER_FRAGMENT, LIST_INDEX_START_FRAGMENT, LIST_INDEX_END_FRAGMENT);
            }
        };
    }

    private static boolean startsWithDigit(String pathFragment) {
        if (StringUtils.isEmpty(pathFragment)) {
            return false;
        }
        return Character.isDigit(pathFragment.charAt(0));
    }

    public static String generateWhitespace(int number) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < number; i++) {
            builder.append(' ');
        }
        return builder.toString();
    }

    /**
     * Copied from Apache commons lang (String utils).
     * <p>
     * The Groovy version compared one-character Strings to each char with {@code ==}, which Groovy evaluates as
     * {@code "{".equals(String.valueOf(ch))}. Comparing chars directly gives the same result.
     */
    private static boolean containsAny(String str, char... searchChars) {
        if (str == null || str.length() == 0 || searchChars.length == 0) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            for (char searchChar : searchChars) {
                if (searchChar == ch) {
                    return true;
                }
            }
        }
        return false;
    }
}
