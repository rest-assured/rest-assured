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



package io.restassured.authentication

import io.restassured.internal.http.HTTPBuilder

/**
 * Authentication scheme that doesn't do any authentication.
 * This is different from NoAuthScheme because it's used to indicate
 * that a user has explicitly requested for no authentication
 * to override a default authentication scheme.
 */
class ExplicitNoAuthScheme implements AuthenticationScheme {
  @Override void authenticate(HTTPBuilder httpBuilder) {
  }
}
