/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.cispayepenaltiesandappeals.controllers

import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.cispayepenaltiesandappeals.base.BaseSpec
import uk.gov.hmrc.cispayepenaltiesandappeals.itUtil.ApplicationWithWiremock
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*

class ClientListControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  private val servicePrefix = s"http://localhost:$port/cis-paye-penalties-and-appeals"
  private val rdsDatacachePrefix = "/rds-datacache-proxy/pas/agent/clientlist"
  private lazy val getClientListDownloadStatusUrl = s"$servicePrefix/agent/clientlist/status"

  private def authoriseUrl(service: String) =
    s"$servicePrefix/${service.toLowerCase}/agent/authorise"

  private def hasClientUrl(service: String) =
    s"$servicePrefix/${service.toLowerCase}/agent/clientlist/clients/status"

  private def hasClientProxyUrl(service: String) =
    s"$rdsDatacachePrefix/clients/status?irAgentId=123456&credentialId=credId&taxOfficeNumber=123&taxOfficeReference=AB456&serviceName=${service.toUpperCase}"

  private def statusUrl(service: String) =
    s"$rdsDatacachePrefix/status?credentialId=credId&serviceName=${service.toUpperCase}&gracePeriod=14400"

  private def initiateUrl(service: String) =
    s"/client-exchange-proxy/${service.toUpperCase}/credId/123456/clientlist"

  private val proxyBody =
    """<gwe:AsynchronousProcessWaitTime browserInterval="10" xmlns:gwe="gwe">
      |<BusinessServiceInterval>10</BusinessServiceInterval>
      |<BusinessServiceInterval>10</BusinessServiceInterval>
      |</gwe:AsynchronousProcessWaitTime>""".stripMargin

  private val body = Json.toJson(AgentAuthoriseRequest("123", "AB456"))

  private def post(service: String = "cis", payload: JsValue = body, urlBuilder: String => String = authoriseUrl) =
    wsClient.url(urlBuilder(service)).withHttpHeaders("Authorization" -> "Bearer123").post(payload)
  
  def stubClientListStatusResponse(credentialId: String, serviceName: String, gracePeriodSeconds: Int): Unit = {
    stubGet(s"$rdsDatacachePrefix/status?credentialId=$credentialId&serviceName=$serviceName&gracePeriod=$gracePeriodSeconds", OK,
      Json.toJson(ClientListStatusResponse("Succeeded")).toString)
  }

  def stubHasCisClientResponse(): Unit = {
    stubGet(hasClientProxyUrl("cis"), OK,
      Json.toJson(HasClientResponse(hasClient = true)).toString)
  }

  def stubHasPayeClientResponse(): Unit = {
    stubGet(hasClientProxyUrl("paye"), OK,
      Json.toJson(HasClientResponse(hasClient = true)).toString)
  }

  val credentialId = "123"
  val serviceName = "CIS"
  val gracePeriodSeconds = 14400

  "ClientListController" should {

    "call get" when {

      "return a 200:Ok:: authorised request" in {
        stubAuthorisedAgent()
        stubClientListStatusResponse(credentialId, serviceName, gracePeriodSeconds)
        val jsonBody = Json.toJson(ClientListStatusRequest(credentialId, serviceName))

        val result = wsClient.url(getClientListDownloadStatusUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubClientListStatusResponse(credentialId, serviceName, gracePeriodSeconds)
        val jsonBody = Json.toJson(ClientListStatusRequest(credentialId, serviceName))

        val result = wsClient.url(getClientListDownloadStatusUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 403:Forbidden:: organisation user" in {
        stubAuthorised()
        stubClientListStatusResponse(credentialId, serviceName, gracePeriodSeconds)
        val jsonBody = Json.toJson(ClientListStatusRequest(credentialId, serviceName))

        val result = wsClient.url(getClientListDownloadStatusUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call hasCisClient" when {

      val testRequest = HasCisClientRequest(
        agentDetails = AgentDetails(credentialId = "credId", irAgentId = "123456"),
        contractorDetails = ContractorDetails(hmrcOfficeNumber = "123", schemeReferenceNumber = "AB456")
      )

      "return a 200:OK:: authorised request" in {
        stubAuthorisedAgent()
        stubHasCisClientResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasClientUrl("cis"))
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
        result.json shouldBe Json.toJson(HasClientResponse(hasClient = true))
      }

      "return a 400:BadRequest:: authorised request with invalid payload" in {
        stubAuthorisedAgent()
        stubHasCisClientResponse()
        val jsonBody = Json.obj("invalid" -> "data")

        val result = wsClient.url(hasClientUrl("cis"))
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe BAD_REQUEST
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubHasCisClientResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasClientUrl("cis"))
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 500:InternalServerError:: authorised request when proxy fails" in {
        stubAuthorisedAgent()
        stubGet(hasClientProxyUrl("cis"), INTERNAL_SERVER_ERROR, "boom")
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasClientUrl("cis"))
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "call hasPayeClient" when {

      val testRequest = HasPayeClientRequest(
        agentDetails = AgentDetails(credentialId = "credId", irAgentId = "123456"),
        employerDetails = EmployerDetails(hmrcOfficeNumber = "123", schemeReferenceNumber = "AB456")
      )

      "return a 200:OK:: authorised request" in {
        stubAuthorisedAgent()
        stubHasPayeClientResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasClientUrl("paye"))
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
        result.json shouldBe Json.toJson(HasClientResponse(hasClient = true))
      }

      "return a 400:BadRequest:: authorised request with invalid payload" in {
        stubAuthorisedAgent()
        stubHasPayeClientResponse()
        val jsonBody = Json.obj("invalid" -> "data")

        val result = wsClient.url(hasClientUrl("paye"))
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe BAD_REQUEST
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubHasPayeClientResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasClientUrl("paye"))
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 500:InternalServerError:: authorised request when proxy fails" in {
        stubAuthorisedAgent()
        stubGet(hasClientProxyUrl("paye"), INTERNAL_SERVER_ERROR, "boom")
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasClientUrl("paye"))
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "call authoriseCisAgent" when {

      "return 200 authorised true:: status Succeeded and the agent has the client" in {
        stubAuthorisedAgent()
        stubGet(statusUrl("cis"), OK, """{"status":"Succeeded"}""")
        stubGet(hasClientProxyUrl("cis"), OK, """{"hasClient":true}""")

        val result = post()

        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.obj("authorised" -> true)
      }

      "return 200 authorised false:: status Succeeded but the agent has no client" in {
        stubAuthorisedAgent()
        stubGet(statusUrl("cis"), OK, """{"status":"Succeeded"}""")
        stubGet(hasClientProxyUrl("cis"), OK, """{"hasClient":false}""")

        val result = post()

        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.obj("authorised" -> false, "reason" -> "NoClientRelationship")
      }

      "return 200 authorised false:: status Failed" in {
        stubAuthorisedAgent()
        stubGet(statusUrl("cis"), OK, """{"status":"Failed"}""")

        val result = post()

        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.obj("authorised" -> false, "reason" -> "Failed")
      }

      "initiate the download and poll until Succeeded" in {
        stubAuthorisedAgent()
        stubGetInScenario(statusUrl("cis"), "download", Scenario.STARTED, Some("initiated"), OK, """{"status":"InitiateDownload"}""")
        stubGetInScenario(statusUrl("cis"), "download", "initiated", None, OK, """{"status":"Succeeded"}""")
        stubGet(initiateUrl("cis"), OK, proxyBody)
        stubGet(hasClientProxyUrl("cis"), OK, """{"hasClient":true}""")

        val result = post()

        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.obj("authorised" -> true)
      }

      "return 400:: invalid tax office number" in {
        stubAuthorisedAgent()

        val result = post(payload = Json.toJson(AgentAuthoriseRequest("12", "AB456")))

        result.status shouldBe BAD_REQUEST
      }

      "return 400:: malformed payload" in {
        stubAuthorisedAgent()

        val result = post(payload = Json.obj("x" -> "y"))

        result.status shouldBe BAD_REQUEST
      }

      "return 500:: has-client call fails" in {
        stubAuthorisedAgent()
        stubGet(statusUrl("cis"), OK, """{"status":"Succeeded"}""")
        stubGet(hasClientProxyUrl("cis"), INTERNAL_SERVER_ERROR, "{}")

        val result = post()

        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "return 403:: agent without an IR-PAYE-AGENT enrolment" in {
        stubAuthorisedAgentWithoutEnrolment()

        post().status shouldBe FORBIDDEN
      }

      "return 403:: organisation user" in {
        stubAuthorised()

        post().status shouldBe FORBIDDEN
      }

      "return 403:: unauthorised request" in {
        stubUnauthorised()

        post().status shouldBe FORBIDDEN
      }
    }

    "call authorisePayeAgent" when {

      "return 200 authorised true:: status Succeeded and the agent has the client" in {
        stubAuthorisedAgent()
        stubGet(statusUrl("paye"), OK, """{"status":"Succeeded"}""")
        stubGet(hasClientProxyUrl("paye"), OK, """{"hasClient":true}""")

        val result = post("paye")

        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.obj("authorised" -> true)
      }

      "return 200 authorised false:: status Succeeded but the agent has no client" in {
        stubAuthorisedAgent()
        stubGet(statusUrl("paye"), OK, """{"status":"Succeeded"}""")
        stubGet(hasClientProxyUrl("paye"), OK, """{"hasClient":false}""")

        val result = post("paye")

        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.obj("authorised" -> false, "reason" -> "NoClientRelationship")
      }

      "return 200 authorised false:: status Failed" in {
        stubAuthorisedAgent()
        stubGet(statusUrl("paye"), OK, """{"status":"Failed"}""")

        val result = post("paye")

        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.obj("authorised" -> false, "reason" -> "Failed")
      }

      "initiate the download and poll until Succeeded" in {
        stubAuthorisedAgent()
        stubGetInScenario(statusUrl("paye"), "download", Scenario.STARTED, Some("initiated"), OK, """{"status":"InitiateDownload"}""")
        stubGetInScenario(statusUrl("paye"), "download", "initiated", None, OK, """{"status":"Succeeded"}""")
        stubGet(initiateUrl("paye"), OK, proxyBody)
        stubGet(hasClientProxyUrl("paye"), OK, """{"hasClient":true}""")

        val result = post("paye")

        result.status shouldBe OK
        Json.parse(result.body) shouldBe Json.obj("authorised" -> true)
      }

      "return 400:: invalid tax office number" in {
        stubAuthorisedAgent()

        val result = post(service = "paye", payload = Json.toJson(AgentAuthoriseRequest("12", "AB456")))

        result.status shouldBe BAD_REQUEST
      }

      "return 400:: malformed payload" in {
        stubAuthorisedAgent()

        val result = post(service = "paye", payload = Json.obj("x" -> "y"))

        result.status shouldBe BAD_REQUEST
      }

      "return 500:: has-client call fails" in {
        stubAuthorisedAgent()
        stubGet(statusUrl("paye"), OK, """{"status":"Succeeded"}""")
        stubGet(hasClientProxyUrl("paye"), INTERNAL_SERVER_ERROR, "{}")

        val result = post("paye")

        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      "return 403:: agent without an IR-PAYE-AGENT enrolment" in {
        stubAuthorisedAgentWithoutEnrolment()

        post("paye").status shouldBe FORBIDDEN
      }

      "return 403:: organisation user" in {
        stubAuthorised()

        post("paye").status shouldBe FORBIDDEN
      }

      "return 403:: unauthorised request" in {
        stubUnauthorised()

        post("paye").status shouldBe FORBIDDEN
      }
    }
  }
}
