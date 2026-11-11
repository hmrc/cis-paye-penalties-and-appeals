/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.cispayepenaltiesandappeals.itUtil

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.{Application, Environment, Mode}


trait ApplicationWithWiremock
  extends AnyWordSpec
    with GuiceOneServerPerSuite
    with BeforeAndAfterAll
    with BeforeAndAfterEach {

  lazy val wireMock = new WireMock

  def extraConfig(stubEnabled: Boolean = false): Map[String, Any] = {
    Map[String, Any](
      "microservice.services.auth.host" -> WireMockConstants.stubHost,
      "microservice.services.auth.port" -> WireMockConstants.stubPort,
      "microservice.services.rds-datacache-proxy.host" -> WireMockConstants.stubHost,
      "microservice.services.rds-datacache-proxy.port" -> WireMockConstants.stubPort,
      "microservice.services.cis-paye-penalties-and-appeals-stub.port" -> WireMockConstants.stubPort,
      "microservice.services.cis-paye-penalties-and-appeals-stub.host" -> WireMockConstants.stubHost,
      "features.stub-rds-datacache-enabled" -> stubEnabled,
      "microservice.services.client-exchange-proxy.host" -> WireMockConstants.stubHost,
      "microservice.services.client-exchange-proxy.port" -> WireMockConstants.stubPort
    )
  }

  override lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(extraConfig())
    .build()

  lazy val appWithStub: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(extraConfig(true))
    .build()
  
  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  override protected def beforeAll(): Unit =
    wireMock.start()
    super.beforeAll()

  override def beforeEach(): Unit =
    wireMock.resetAll()
    super.beforeEach()

  override def afterAll(): Unit = {
    wireMock.stop()
    super.afterAll()
  }

  def stubGet(url: String, status: Integer, body: String): StubMapping =
    stubFor(get(urlEqualTo(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(body)
      )
    )

  def stubPost(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(post(urlMatching(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  private val postAuthoriseUrl = "/auth/authorise"

  private val irPayeAgentEnrolment: String =
    """{"key":"IR-PAYE-AGENT","identifiers":[{"key":"IRAgentReference","value":"123456"}],"state":"Activated"}"""

  private def agentAuthoriseBody(enrolments: String): String =
    s"""{
       |  "authorise": [{"confidenceLevel": 200}],
       |  "retrieve": ["affinityGroup", "allEnrolments", "optionalCredentials"],
       |  "affinityGroup": "Agent",
       |  "allEnrolments": [$enrolments],
       |  "optionalCredentials": {"providerId": "credId", "providerType": "GovernmentGateway"}
       |}""".stripMargin

  private val authoriseBodyWithOrgEnrolmentsRetrieval: String =
    """{
      |  "authorise": [{"confidenceLevel": 200}],
      |  "retrieve": ["allEnrolments"],
      |  "credId": "credId",
      |  "individualEnrolments":{"sa":"1111111111"},
      |  "allEnrolments": [
      |               {
      |                 "key":"IR-PAYE",
      |                 "identifiers": [
      |                    {
      |                       "key":"IR-PAYE",
      |                       "value": "value"
      |                    }
      |                   ],
      |                "state": "Activated"
      |              }
      |  ],
      |  "affinityGroup": "Organisation",
      |  "credentialRole": "User",
      |  "internalId": "internalId",
      |  "optionalCredentials": {
      |    "providerId": "credId",
      |    "providerType": "GovernmentGateway"
      |  }
      |}""".stripMargin

  def stubAuthorised(): Unit = {
    stubPost(postAuthoriseUrl, Status.OK, authoriseBodyWithOrgEnrolmentsRetrieval)
  }

  def stubUnauthorised(): Unit = {
    stubPost(postAuthoriseUrl, Status.UNAUTHORIZED, "{}")
  }

  def stubAuthorisedAgent(): Unit =
    stubPost(postAuthoriseUrl, Status.OK, agentAuthoriseBody(irPayeAgentEnrolment))

  def stubAuthorisedAgentWithoutEnrolment(): Unit =
    stubPost(postAuthoriseUrl, Status.OK, agentAuthoriseBody(""))

  def stubGetInScenario(url: String, scenario: String, fromState: String, toState: Option[String], status: Integer, body: String): StubMapping = {
    val mapping = get(urlEqualTo(url))
      .inScenario(scenario)
      .whenScenarioStateIs(fromState)
      .willReturn(aResponse().withStatus(status).withBody(body))
    stubFor(toState.fold(mapping)(state => mapping.willSetStateTo(state)))
  }

}