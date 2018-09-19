package io.restassured.module.webtestclient.internal;

import io.restassured.filter.log.LogDetail;
import io.restassured.internal.LogSpecificationImpl;
import io.restassured.module.webtestclient.specification.WebTestClientRequestLogSpecification;
import io.restassured.module.webtestclient.specification.WebTestClientRequestSpecification;

/**
 * @author Olga Maciaszek-Sharma
 */
public class WebTestClientRequestLogSpecificationImpl extends LogSpecificationImpl implements WebTestClientRequestLogSpecification {

	private final WebTestClientRequestSpecification requestSpecification;

	public WebTestClientRequestLogSpecificationImpl(WebTestClientRequestSpecification requestSpecification) {
		this.requestSpecification = requestSpecification;
	}

	@Override
	public WebTestClientRequestSpecification body() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification body(boolean shouldPrettyPrint) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification all() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification all(boolean shouldPrettyPrint) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification everything() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification everything(boolean shouldPrettyPrint) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification headers() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification cookies() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification ifValidationFails() {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification ifValidationFails(LogDetail logDetail) {
		throw new UnsupportedOperationException("Please, implement me.");
	}

	@Override
	public WebTestClientRequestSpecification ifValidationFails(LogDetail logDetail, boolean shouldPrettyPrint) {
		throw new UnsupportedOperationException("Please, implement me.");
	}
}
