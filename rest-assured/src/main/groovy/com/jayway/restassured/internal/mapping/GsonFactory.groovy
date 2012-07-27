package com.jayway.restassured.internal.mapping

import com.google.gson.Gson

class GsonFactory {
	def Gson createGson(Class cls) {
		return new Gson();
	}
}
