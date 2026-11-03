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

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.cispayepenaltiesandappeals.base.BaseSpec
import uk.gov.hmrc.cispayepenaltiesandappeals.itUtil.ApplicationWithWiremock
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*

class ClientListControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/cis-paye-penalties-and-appeals"
  lazy val getClientListDownloadStatusUrl = s"$servicePrefix/agent/clientlist/status"
  lazy val hasCisClientUrl = s"$servicePrefix/cis/agent/clientlist/has-client"
  lazy val hasPayeClientUrl = s"$servicePrefix/paye/agent/clientlist/has-client"
  val hasCisClientProxyUrl = "/rds-datacache-proxy/pas/agent/clientlist/has-client?irAgentId=123456&credentialId=CRED01&taxOfficeNumber=754&taxOfficeReference=EZ00100&serviceName=CIS"
  val hasPayeClientProxyUrl = "/rds-datacache-proxy/pas/agent/clientlist/has-client?irAgentId=123456&credentialId=CRED01&taxOfficeNumber=840&taxOfficeReference=MZ00064&serviceName=PAYE"

  def stubClientListStatusResponse(credentialId: String, serviceName: String, gracePeriodSeconds: Int): Unit = {
    stubGet(s"/rds-datacache-proxy/pas/agent/clientlist/status?credentialId=$credentialId&serviceName=$serviceName&gracePeriod=$gracePeriodSeconds", Status.OK,
      Json.toJson(ClientListStatusResponse("Succeeded")).toString)
  }

  def stubHasCisClientResponse(): Unit = {
    stubGet(hasCisClientProxyUrl, Status.OK,
      Json.toJson(HasClientResponse(hasClient = true)).toString)
  }

  def stubHasPayeClientResponse(): Unit = {
    stubGet(hasPayeClientProxyUrl, Status.OK,
      Json.toJson(HasClientResponse(hasClient = true)).toString)
  }

  val credentialId = "123"
  val serviceName = "CIS"
  val gracePeriodSeconds = 14400

  "ClientListController" should {

    "call get" when {

      "return a 200:Ok:: authorised request" in {
        stubAuthorised()
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
    }

    "call hasCisClient" when {

      val testRequest = HasCisClientRequest(
        agentDetails = AgentDetails(credentialId = "CRED01", irAgentId = "123456"),
        contractorDetails = ContractorDetails(hmrcOfficeNumber = "754", schemeReferenceNumber = "EZ00100")
      )

      "return a 200:OK:: authorised request" in {
        stubAuthorised()
        stubHasCisClientResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasCisClientUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
        result.json shouldBe Json.toJson(HasClientResponse(hasClient = true))
      }

      "return a 400:BadRequest:: authorised request with invalid payload" in {
        stubAuthorised()
        stubHasCisClientResponse()
        val jsonBody = Json.obj("invalid" -> "data")

        val result = wsClient.url(hasCisClientUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe BAD_REQUEST
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubHasCisClientResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasCisClientUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 500:InternalServerError:: authorised request when proxy fails" in {
        stubAuthorised()
        stubGet(hasCisClientProxyUrl, Status.INTERNAL_SERVER_ERROR, "boom")
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasCisClientUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "call hasPayeClient" when {

      val testRequest = HasPayeClientRequest(
        agentDetails = AgentDetails(credentialId = "CRED01", irAgentId = "123456"),
        employerDetails = EmployerDetails(hmrcOfficeNumber = "840", schemeReferenceNumber = "MZ00064")
      )

      "return a 200:OK:: authorised request" in {
        stubAuthorised()
        stubHasPayeClientResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasPayeClientUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
        result.json shouldBe Json.toJson(HasClientResponse(hasClient = true))
      }

      "return a 400:BadRequest:: authorised request with invalid payload" in {
        stubAuthorised()
        stubHasPayeClientResponse()
        val jsonBody = Json.obj("invalid" -> "data")

        val result = wsClient.url(hasPayeClientUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe BAD_REQUEST
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubHasPayeClientResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasPayeClientUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 500:InternalServerError:: authorised request when proxy fails" in {
        stubAuthorised()
        stubGet(hasPayeClientProxyUrl, Status.INTERNAL_SERVER_ERROR, "boom")
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(hasPayeClientUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
