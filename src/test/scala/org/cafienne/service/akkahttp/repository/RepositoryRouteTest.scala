package org.cafienne.service.akkahttp.repository

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import org.cafienne.infrastructure.akkahttp.ValueMarshallers
import org.cafienne.system.CaseSystem
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

class RepositoryRouteTest extends AnyFlatSpec with Matchers with ScalatestRouteTest {

  val logger: LoggingAdapter = Logging(system, getClass)

  implicit val caseSystem: CaseSystem = new CaseSystem(ActorSystem("RepositoryRouteTest"))

  val repositoryRoute: RepositoryRoute = new RepositoryRoute(caseSystem) {
  }

  testValidationRoute("fail when an invalid definition is given", "testdefinition/invaliddefinition.xml", "[ \"helloworld.case: Plan item Receive Greeting and Send response refers to a definition named pid_cm_csVQy_167, but that definition is not found\" ]", StatusCodes.BadRequest)
  testValidationRoute("fail when no definition is given", "testdefinition/nodefinition.xml", "[ \"The definitions document does not contain any definitions\" ]", StatusCodes.BadRequest)
  testValidationRoute("succeed when a valid definition is given ", "testdefinition/helloworld.xml", "OK", StatusCodes.OK)

  /**
    * Test method for the /validation sub-route of the /repository route
    * @param testName
    * @param fileName
    * @param expectedResponseMessage
    * @param expectedResponseCode
    */
  def testValidationRoute(testName: String, fileName: String, expectedResponseMessage: String, expectedResponseCode: StatusCode): Unit = {
    import scala.concurrent.duration._
    implicit def default: RouteTestTimeout = RouteTestTimeout(5.second) // Validation likes to take some of your time ;)

    val entity = createHTTPEntity(fileName)

    "The validation route" should testName in {
      Post("/repository/validate", entity) ~> Route.seal(repositoryRoute.route) ~> check {
        status must be(expectedResponseCode)
        responseAs[String] must be(expectedResponseMessage)
      }
    }
  }

  // Below some too stupid helper methods to be able to read the definition files from the classpath and convert them to a HttpEntity
  def createHTTPEntity(fileName: String): HttpEntity.Strict = {
    val fileStream = getClass.getClassLoader.getResourceAsStream(fileName)
    if (fileStream == null) {
      throw new IllegalArgumentException("The file with name "+fileName+" cannot be loaded from the classpath")
    }
    val fileBytes = LazyList.continually(fileStream.read).takeWhile(b => b != -1).map(_.toByte).toArray
    HttpEntity(ValueMarshallers.`application/xml`, fileBytes)
  }
}
