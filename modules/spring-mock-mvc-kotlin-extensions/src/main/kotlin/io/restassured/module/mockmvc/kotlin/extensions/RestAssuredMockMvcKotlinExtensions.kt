@file:Suppress("FunctionName")

package io.restassured.module.mockmvc.kotlin.extensions

import io.restassured.internal.ResponseSpecificationImpl
import io.restassured.module.mockmvc.RestAssuredMockMvc.`when`
import io.restassured.module.mockmvc.RestAssuredMockMvc.given
import io.restassured.module.mockmvc.internal.MockMvcRestAssuredResponseImpl
import io.restassured.module.mockmvc.internal.ValidatableMockMvcResponseImpl
import io.restassured.module.mockmvc.response.MockMvcResponse
import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse
import io.restassured.module.mockmvc.specification.MockMvcRequestAsyncSender
import io.restassured.module.mockmvc.specification.MockMvcRequestSender
import io.restassured.module.mockmvc.specification.MockMvcRequestSpecification
import io.restassured.response.ExtractableResponse

/**
 * A wrapper around [given] that starts building the request part of the test.
 * @see given
 */
fun Given(block: MockMvcRequestSpecification.() -> MockMvcRequestSpecification): MockMvcRequestSpecification =
    given().run(block)

/**
 * A wrapper around [ MockMvcRequestSpecification.when ] that configures how the request is dispatched.
 * @see MockMvcRequestSpecification.when
 */
infix fun MockMvcRequestSpecification.When(
    block: MockMvcRequestAsyncSender.() ->
    MockMvcResponse
): MockMvcResponse =
    `when`().run(block)

/**
 * A wrapper around [ io.restassured.module.mockmvc.RestAssuredMockMvc.when ] that configures how the request is dispatched.
 * @see io.restassured.module.mockmvc.RestAssuredMockMvc.when
 */
fun When(block: MockMvcRequestSender.() -> MockMvcRestAssuredResponseImpl): MockMvcRestAssuredResponseImpl =
    `when`().run(block)

/**
 * A wrapper around [then] that allow configuration of response expectations.
 * @see then
 */
infix fun MockMvcResponse.Then(block: ValidatableMockMvcResponse.() -> Unit): ValidatableMockMvcResponse = then()
    .also(doIfValidatableResponseImpl {
        forceDisableEagerAssert()
    })
    .apply(block)
    .also(doIfValidatableResponseImpl {
        forceValidateResponse()
    })

/**
 * A wrapper around [ExtractableResponse] that allow for extract data out of the response
 * @see ExtractableResponse
 */
infix fun <T> MockMvcResponse.Extract(block: ExtractableResponse<MockMvcResponse>.() -> T): T =
    then().extract().run(block)

/**
 * A wrapper around [ExtractableResponse] that allow for extract data out of the response
 * @see ExtractableResponse
 */
infix fun <T> ValidatableMockMvcResponse.Extract(
    block: ExtractableResponse<MockMvcResponse>.() -> T
): T = extract().run(block)

//  End main wrappers

private fun doIfValidatableResponseImpl(
    fn: ResponseSpecificationImpl.() -> Unit
): (ValidatableMockMvcResponse) -> Unit = { resp ->
    if (resp is ValidatableMockMvcResponseImpl) {
        fn(resp.responseSpec)
    }
}
