package com.jayway.restassured.internal.mapping

import javax.xml.bind.JAXBContext

class JAXBContextFactory {
	public JAXBContext createJAXBContext(Class<?> clazz) {
		return JAXBContext.newInstance(clazz);
	}
}
