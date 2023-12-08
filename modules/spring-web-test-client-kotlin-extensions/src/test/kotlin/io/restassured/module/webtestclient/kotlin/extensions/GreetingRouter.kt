package io.restassured.module.webtestclient.kotlin.extensions

import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.router
import java.util.concurrent.atomic.AtomicLong

class GreetingRouter {

    private val template = "Hello, %s!"
    private val counter = AtomicLong()

    fun route(): RouterFunction<ServerResponse> =
            router {
                GET("/greeting") {
                    ServerResponse.ok()
                            .body(BodyInserters.fromObject(Greeting(
                                    counter.incrementAndGet(),
                                    String.format(template, it.queryParam("name").get())
                            )))
                }

            }
}
