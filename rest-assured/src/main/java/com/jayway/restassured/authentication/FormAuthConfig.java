/*
 * Copyright 2011 the original author or authors.
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

package com.jayway.restassured.authentication;

import static com.jayway.restassured.assertion.AssertParameter.notNull;

/**
 * Configuration of form authentication to correctly identify which form that contains the username and password
 * and the action of the form.
 */
public class FormAuthConfig {
    private final String formAction;
    private final String userInputTagName;
    private final String passwordInputTagName;

    /**
     * Create a form auth config with a pre-defined form action, username input tag, password input tag.
     * E.g. let's say that the login form on your login page looks like this:
     * <pre>
     *  &lt;form action=&quot;/j_spring_security_check&quot;&gt;
     *     &lt;label for=&quot;j_username&quot;&gt;Username&lt;/label&gt;
     *     &lt;input type=&quot;text&quot; name=&quot;j_username&quot; id=&quot;j_username&quot;/&gt;
     *     &lt;br/&gt;
     *     &lt;label for=&quot;j_password&quot;&gt;Password&lt;/label&gt;
     *     &lt;input type=&quot;password&quot; name=&quot;j_password&quot; id=&quot;j_password&quot;/&gt;
     *     &lt;br/&gt;
     *     &lt;input type=&#39;checkbox&#39; name=&#39;_spring_security_remember_me&#39;/&gt; Remember me on this computer.
     *     &lt;br/&gt;
     *     &lt;input type=&quot;submit&quot; value=&quot;Login&quot;/&gt;
     * &lt;/form&gt;
     * </pre>
     *
     * This means that <code>formAction</code> should be set to <code>/j_spring_security_check</code>, <code>userNameInputTagName</code>
     * should be set to <code>j_username</code> and <code>passwordInputTagName</code> should be set to <code>j_password</code>.
     *
     * @param formAction The action of the form
     * @param userNameInputTagName The name of the username input tag in the login form
     * @param passwordInputTagName The name of the password input tag in the login form
     */
    public FormAuthConfig(String formAction, String userNameInputTagName, String passwordInputTagName) {
        notNull(formAction, "Form action");
        notNull(userNameInputTagName, "User input tag name");
        notNull(passwordInputTagName, "Password input tag name");
        this.formAction = formAction;
        this.userInputTagName = userNameInputTagName;
        this.passwordInputTagName = passwordInputTagName;
    }

    /**
     * @return A predefined form authentication config for default Spring Security configuration (tested in version 3.0.5).
     */
    public static FormAuthConfig springSecurity() {
        return new FormAuthConfig("/j_spring_security_check", "j_username", "j_password");
    }

    public String getFormAction() {
        return formAction;
    }

    public String getUserInputTagName() {
        return userInputTagName;
    }

    public String getPasswordInputTagName() {
        return passwordInputTagName;
    }
}
