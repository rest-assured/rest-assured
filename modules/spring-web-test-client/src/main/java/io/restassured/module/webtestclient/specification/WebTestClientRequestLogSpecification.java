package io.restassured.module.webtestclient.specification;

import io.restassured.specification.LogSpecification;

/**
 * @author Olga Maciaszek-Sharma
 */
public interface WebTestClientRequestLogSpecification extends LogSpecification<WebTestClientRequestSpecification> {

	/**
	 * Logs only the parameters of the request. Same as {@link #parameters()} but slightly shorter syntax.
	 *
	 * @return The response specification
	 */
	WebTestClientRequestSpecification params();

	/**
	 * Logs only the parameters of the request. Same as {@link #params()} but more explicit syntax.
	 *
	 * @return The response specification
	 */
	WebTestClientRequestSpecification parameters();
}
