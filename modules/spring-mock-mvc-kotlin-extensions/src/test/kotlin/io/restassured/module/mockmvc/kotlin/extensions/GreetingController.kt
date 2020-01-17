package io.restassured.module.mockmvc.kotlin.extensions

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.util.concurrent.atomic.AtomicLong

@Controller
class GreetingController {
    private val template = "Hello, %s!"
    private val counter = AtomicLong()

    @RequestMapping(
        value = ["/greeting"],
        method = [RequestMethod.GET]
    )
    @ResponseBody
    fun greeting(
        @RequestParam(
            value = "name",
            required = false,
            defaultValue = "World"
        ) name: String
    ) = Greeting(
        counter.incrementAndGet(),
        String.format(template, name)
    )

    @RequestMapping(
        value = ["/greeting"],
        method = [RequestMethod.POST],
        consumes = ["application/json"],
        produces = ["application/json"]
    )
    @ResponseBody
    fun greetingWithRequiredContentType(
        @RequestParam(
            value = "name",
            required = false,
            defaultValue = "World"
        ) name: String
    ) = greeting(name)
}

data class Greeting(
    val id: Long,
    val content: String
)
