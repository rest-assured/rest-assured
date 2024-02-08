package io.restassured.module.scala.extensions

import scala.util.chaining.*
import scala.reflect.ClassTag
import io.restassured.RestAssured.`given`
import io.restassured.RestAssured.`when`
import io.restassured.response.{ExtractableResponse, Response, ValidatableResponse}
import io.restassured.specification.{RequestSender, RequestSpecification}
import io.restassured.internal.{ValidatableResponseImpl, ResponseSpecificationImpl}

// Main wrappers

/**
 * A wrapper around [given] that starts building the request part of the test.
 * E.g.
 * {{{
 * Given(_.params("firstName", "John"))
 *  .When(_.post("/greetXML"))
 *  .Then(res =>
 *    res.statusCode(200)
 *    res.body("greeting.firstName", equalTo("John"))
 * )
 * }}}
 * will send a POST request to "/greetXML" with request parameters
 * `firstName=John` and `lastName=Doe` and expect that the response body
 * containing JSON or XML firstName equal to John.
 *
 * @return
 *   A request specification.
 *
 * @see
 *   io.restassured.RestAssured.given
 */
def Given(block: RequestSpecification => RequestSpecification): RequestSpecification =
  `given`().pipe(block)

/**
 * A wrapper around [given] that starts building the request part of the test.
 * This is an overloaded version of [Given] that does not take any arguments.
 * E.g.
 * {{{
 * Given()
 *  .When(_.post("/greetXML"))
 *  .Then(_.statusCode(200))
 * }}}
 * will send a POST request to "/greetXML" request parameters and expect that
 * the statusCode 200.
 *
 * @return
 *   A request specification.
 *
 * @see
 *   io.restassured.RestAssured.given
 */
def Given(): RequestSpecification =
  `given`()

/**
 * A wrapper around [io.restassured.RestAssured.when] to start building the DSL
 * expression by sending a request without any parameters or headers etc. E.g.
 * {{{
 * Given()
 *   .When(_.get("/x"))
 *   .Then(_.body("x.y.z1", equalTo("Z1")))
 * }}}
 * Note that if you need to add parameters, headers, cookies or other request
 * properties use the [[Given()]] method.
 *
 * @see
 *   io.restassured.RestAssured.when
 * @return
 *   A request sender interface that let's you call resources on the server
 */
extension (spec: RequestSpecification)
  infix def When(block: RequestSpecification => Response): Response =
    spec.`when`().pipe(block)

/**
 * A wrapper around [io.restassured.RestAssured.when] to start building the DSL
 * expression by sending a request without any parameters or headers etc. E.g.
 * {{{
 * Given()
 *   .When(_.get("/x"))
 *   .Then(_.body("x.y.z1", equalTo("Z1")))
 * }}}
 * Note that if you need to add parameters, headers, cookies or other request
 * properties use the [[Given()]] method.
 *
 * @see
 *   io.restassured.RestAssured.when
 * @return
 *   A request sender interface that let's you call resources on the server
 */
def When(block: RequestSender => Response): Response =
  `when`().pipe(block)

/**
 * A wrapper around [then] that lets you validate the response. Usage example:
 * {{{
 * Given(_.params("firstName", "John")
 *  .When(_.post("/greetXML"))
 *  .Then(_.body("greeting.firstName", equalTo("John")))
 * }}}
 *
 * @return
 *   A validatable response
 */
extension (resp: Response)
  infix def Then(block: ValidatableResponse => ValidatableResponse): ValidatableResponse =
    resp
      .`then`()
      .tap(doIfValidatableResponseImpl(resp => resp.forceDisableEagerAssert()))
      .pipe(block)
      .tap(doIfValidatableResponseImpl(resp => resp.forceValidateResponse()))

/**
 * A wrapper around [ExtractableResponse] that lets you validate the response.
 * Usage example:
 * {{{
 * val firstName: String = Given(_.params("firstName", "John")
 *  .When(_.post("/greetXML"))
 *  .Then(_.body("greeting.firstName", equalTo("John")))
 *  .Extract(_.path("greeting.firstName"))
 * }}}
 * The above code will send a POST request to "/greetXML" with request
 * parameters `firstName=John` and `lastName=Doe` and expect that the response
 * body containing JSON or XML firstName equal to John. The response is then
 * validated and the firstName is extracted from the response. The extracted
 * firstName is then returned. The type of the extracted value is needs to be
 * specified as a type parameter.
 *
 * @return
 *   The extracted value
 */
extension [T](resp: Response)
  infix def Extract(
    block: ExtractableResponse[Response] => T
  )(using
    ClassTag[T]
  ): T =
    resp.`then`().extract().pipe(block)

/**
 * A wrapper around [ValidatableResponse] that lets you validate the response.
 * Usage example:
 * {{{
 * val firstName: String = Given(_.params("firstName", "John")
 *  .When(_.post("/greetXML"))
 *  .Then(_.body("greeting.firstName", equalTo("John")))
 *  .Extract(_.path("greeting.firstName"))
 * }}}
 * The above code will send a POST request to "/greetXML" with request
 * parameters `firstName=John` and `lastName=Doe` and expect that the response
 * body containing JSON or XML firstName equal to John. The response is then
 * validated and the firstName is extracted from the response. The extracted
 * firstName is then returned. The type of the extracted value is needs to be
 * specified as a type parameter.
 *
 * @return
 *   The extracted value
 */
extension [T](resp: ValidatableResponse)
  infix def Extract(
    block: ExtractableResponse[Response] => T
  )(using
    ClassTag[T]
  ): T =
    resp.extract().pipe(block)

// End main wrappers

private def doIfValidatableResponseImpl(fn: ResponseSpecificationImpl => Unit): ValidatableResponse => Unit =
  resp => if resp.isInstanceOf[ValidatableResponseImpl] then fn(resp.asInstanceOf[ValidatableResponseImpl].responseSpec)
