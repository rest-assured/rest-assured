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
package io.restassured.module.webtestclient.internal;

import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

public class StandaloneWebTestClientFactory {

	public static WebTestClientFactory of(RouterFunction routerFunction, Object[] configurersOrExchangeFilterFunctions) {
		WebTestClient.Builder builder = WebTestClient.bindToRouterFunction(routerFunction)
				.configureClient();
		applyConfigurersAndFilter(configurersOrExchangeFilterFunctions, builder);
		return new BuilderBasedWebTestClientFactory(builder);
	}

	public static WebTestClientFactory of(Object[] controllersOrConfigurersOrExchangeFilterFunctions) {
		Map<Boolean, List<Object>> partitionedByConfigurer = Arrays.stream(controllersOrConfigurersOrExchangeFilterFunctions)
				.collect(partitioningBy(WebTestClientConfigurer.class::isInstance));
		List<Object> controllersAndExchangeFunctions = partitionedByConfigurer.get(false);
		Map<Boolean, List<Object>> partitionedByExchangeFunction = controllersAndExchangeFunctions.stream()
				.collect(partitioningBy(ExchangeFilterFunction.class::isInstance));
		List<WebTestClientConfigurer> configurers = partitionedByConfigurer.get(true).stream()
				.map(WebTestClientConfigurer.class::cast)
				.collect(toList());
		List<ExchangeFilterFunction> exchangeFilterFunctions = partitionedByExchangeFunction.get(true).stream()
				.map(ExchangeFilterFunction.class::cast)
				.collect(toList());
		WebTestClient.Builder builder = WebTestClient.bindToController(partitionedByExchangeFunction.get(false)
				.toArray())
				.configureClient();
		configurers.forEach(builder::apply);
		exchangeFilterFunctions.forEach(builder::filter);
		return new BuilderBasedWebTestClientFactory(builder);
	}

	public static WebTestClientFactory of(ApplicationContext applicationContext,
	                                      Object[] configurersOrExchangeFilterFunctions) {
		WebTestClient.Builder builder = WebTestClient.bindToApplicationContext(applicationContext)
				.configureClient();
		applyConfigurersAndFilter(configurersOrExchangeFilterFunctions, builder);
		return new BuilderBasedWebTestClientFactory(builder);
	}

	private static void applyConfigurersAndFilter(Object[] configurersOrExchangeFilterFunctions,
												  WebTestClient.Builder builder) {
		Map<Boolean, List<Object>> partitionedByConfigurer = Arrays.stream(configurersOrExchangeFilterFunctions)
				.collect(partitioningBy(WebTestClientConfigurer.class::isInstance));
		partitionedByConfigurer.get(true).stream().map(WebTestClientConfigurer.class::cast)
				.forEach(builder::apply);
		partitionedByConfigurer.get(false).stream()
				.filter(ExchangeFilterFunction.class::isInstance)
				.map(ExchangeFilterFunction.class::cast)
				.forEach(builder::filter);
	}
}

