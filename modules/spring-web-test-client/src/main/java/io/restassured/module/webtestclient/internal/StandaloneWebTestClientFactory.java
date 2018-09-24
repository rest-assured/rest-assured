package io.restassured.module.webtestclient.internal;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.reactive.server.WebTestClientConfigurer;
import org.springframework.web.reactive.function.server.RouterFunction;

/**
 * @author Olga Maciaszek-Sharma
 */
public class StandaloneWebTestClientFactory {

	public static WebTestClientFactory of(RouterFunction routerFunction, WebTestClientConfigurer[] configurers) {
		WebTestClient.Builder builder = WebTestClient.bindToRouterFunction(routerFunction)
				.configureClient();
		Arrays.asList(configurers).forEach(builder::apply);
		return new BuilderBasedWebTestClientFactory(builder);
	}

	public static WebTestClientFactory of(Object[] controllersOrConfigurers) {
		Map<Boolean, List<Object>> partitioned = Arrays.stream(controllersOrConfigurers)
				.collect(Collectors.partitioningBy(element -> element instanceof WebTestClientConfigurer));
		List<Object> controllers = partitioned.get(false);
		List<WebTestClientConfigurer> configurers = partitioned.get(true).stream()
				.map(configurer -> (WebTestClientConfigurer) configurer)
				.collect(Collectors.toList());
		WebTestClient.Builder builder = WebTestClient.bindToController(controllers.toArray())
				.configureClient();
		configurers.forEach(builder::apply);
		return new BuilderBasedWebTestClientFactory(builder);
	}

	public static WebTestClientFactory of(ApplicationContext applicationContext, WebTestClientConfigurer[] configurers){
		WebTestClient.Builder builder = WebTestClient.bindToApplicationContext(applicationContext)
				.configureClient();
		Arrays.asList(configurers).forEach(builder::apply);
		return new BuilderBasedWebTestClientFactory(builder);
	}
}

