package io.restassured.module.webtestclient.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;

import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;

/**
 * @author Olga Maciaszek-Sharma
 */
public class StandaloneWebTestClientFactory {

	public static WebTestClientFactory of(RouterFunction routerFunction, Object[] configurersOrExchangeFilterFunctions) {
		WebTestClient.Builder builder = WebTestClient.bindToRouterFunction(routerFunction)
				.configureClient();
		applyConfigurersAndFilter(configurersOrExchangeFilterFunctions, builder);
		return new BuilderBasedWebTestClientFactory(builder);
	}

	private static void applyConfigurersAndFilter(Object[] configurersOrExchangeFilterFunctions,
	                                              WebTestClient.Builder builder) {
		Map<Boolean, List<Object>> partitionedByConfigurer = Arrays.stream(configurersOrExchangeFilterFunctions)
				.collect(partitioningBy(element -> element instanceof WebTestClientConfigurer));
		partitionedByConfigurer.get(true).stream().map(configurer -> (WebTestClientConfigurer) configurer)
				.forEach(builder::apply);
		partitionedByConfigurer.get(false).stream()
				.filter(element -> element instanceof ExchangeFilterFunction)
				.map(element -> (ExchangeFilterFunction) element)
				.forEach(builder::filter);
	}

	public static WebTestClientFactory of(Object[] controllersOrConfigurersOrExchangeFilterFunctions) {
		Map<Boolean, List<Object>> partitionedByConfigurer = Arrays.stream(controllersOrConfigurersOrExchangeFilterFunctions)
				.collect(partitioningBy(element -> element instanceof WebTestClientConfigurer));
		List<Object> controllersAndExchangeFunctions = partitionedByConfigurer.get(false);
		Map<Boolean, List<Object>> partitionedByExchangeFunction = controllersAndExchangeFunctions.stream()
				.collect(partitioningBy(element -> element instanceof ExchangeFilterFunction));
		List<WebTestClientConfigurer> configurers = partitionedByConfigurer.get(true).stream()
				.map(configurer -> (WebTestClientConfigurer) configurer)
				.collect(toList());
		List<ExchangeFilterFunction> exchangeFilterFunctions = partitionedByExchangeFunction.get(true).stream()
				.map(element -> (ExchangeFilterFunction) element)
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
}

