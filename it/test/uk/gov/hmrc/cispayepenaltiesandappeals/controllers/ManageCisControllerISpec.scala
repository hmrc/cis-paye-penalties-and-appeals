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

import uk.gov.hmrc.cispayepenaltiesandappeals.itUtil.ApplicationWithWiremock
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, CREATED, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.cispayepenaltiesandappeals.base.BaseSpec
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*

import java.time.LocalDate

class ManageCisControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/cis-paye-penalties-and-appeals"
  lazy val createCisAppealUrl = s"$servicePrefix/cis/appeal"
  lazy val getCisAppealablePenaltiesUrl = s"$servicePrefix/cis/penalties"
  lazy val getAppealablePenaltyUrl = s"$servicePrefix/cis/penalty"
  lazy val getCisTaxpayerUrl = s"$servicePrefix/cis/taxpayer"
  lazy val getCisPenaltiesAllowedUrl = s"$servicePrefix/cis/penalties/access-allowed"
  lazy val hasCisClientUrl = s"$servicePrefix/cis/agent/clientlist/has-client"
  val getCisTaxpayerProxyUrl = "/rds-datacache-proxy/cis-taxpayer"
  val hasCisClientProxyUrl = "/rds-datacache-proxy/cis/has-client?irAgentId=123456&credentialId=CRED01&taxOfficeNumber=754&taxOfficeReference=EZ00100"

  def stubCreateAppealResponse(): Unit = {
    stubPost("/rds-datacache-proxy/pas/cis/appeal", Status.CREATED,
      Json.toJson(CreateAppealResponse(created = true)).toString)
  }

  def stubGetCisAppealablePenaltiesResponse(): Unit = {
    stubPost("/rds-datacache-proxy/pas/cis/penalties", Status.OK,
      Json.toJson(GetAppealablePenaltiesResponse(
        total = 2,
        appealablePenalties = List(
          GetAppealablePenaltyResponse(
            uniqueId = Some(10001),
            issueDate = Some(LocalDate.of(2023, 1, 1)),
            taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
            penaltyType = Some("CIS003"),
            penaltyAmount = Some(BigDecimal(999.23))
          ),
          GetAppealablePenaltyResponse(
            uniqueId = Some(10002),
            issueDate = Some(LocalDate.of(2026, 2, 20)),
            taxPeriodEndDate = Some(LocalDate.of(2026, 1, 5)),
            penaltyType = Some("CIS005"),
            penaltyAmount = Some(BigDecimal(928.23))
          )
        )
      )).toString)
  }

  def stubGetAppealablePenaltyResponse(): Unit = {
    stubPost("/rds-datacache-proxy/pas/cis/penalty", Status.OK,
      Json.toJson(GetAppealablePenaltyResponse(
        uniqueId = None,
        issueDate = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
        penaltyType = Some("CIS003"),
        penaltyAmount = Some(BigDecimal(999.23))
      )).toString)
  }

  def stubGetCisTaxpayerResponse(): Unit = {
    stubPost(getCisTaxpayerProxyUrl, Status.OK,
      Json.toJson(GetCisTaxpayerResponse(
        uniqueId = "1000123",
        taxOfficeNumber = "123",
        taxOfficeRef = "AB456",
        aoDistrict = Some("123"),
        aoPayType = Some("P"),
        aoCheckCode = Some("X"),
        aoReference = Some("00123456"),
        validBusinessAddr = None,
        correlation = None,
        ggAgentId = None,
        employerName1 = None,
        employerName2 = None,
        agentOwnRef = None,
        schemeName = Some("ABC Builders Ltd"),
        utr = Some("1234567890"),
        enrolledSig = None
      )).toString)
  }

  def stubGetPenaltyAllowedResponse(): Unit = {
    stubPost("/rds-datacache-proxy/pas/cis/penalties/access-allowed", Status.OK,
      Json.toJson(GetPenaltyAllowedResponse(allowed = true)).toString)
  }

  def stubHasCisClientResponse(): Unit = {
    stubGet(hasCisClientProxyUrl, Status.OK,
      Json.toJson(HasClientResponse(hasClient = true)).toString)
  }

  "ManageCisController" should {

    "call createCisAppeal" when {

      "return a 201:Created:: authorised request" in {
        stubAuthorised()
        stubCreateAppealResponse()
        val jsonBody = Json.toJson(
          CreateAppealRequest(penaltyId = 1L))

        val result = wsClient.url(createCisAppealUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubCreateAppealResponse()
        val jsonBody = Json.toJson(
          CreateAppealRequest(penaltyId = 1L))

        val result = wsClient.url(createCisAppealUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call getCisAppealablePenalties" when {

      "return a 200:Ok:: authorised request" in {
        stubAuthorised()
        stubGetCisAppealablePenaltiesResponse()
        val jsonBody = Json.toJson(
          GetAppealablePenaltiesRequest(
            hmrcOfficeNumber = "123",
            schemeReferenceNumber = "AB456",
            startRow = 1L,
            endRow = 10L
          ))

        val result = wsClient.url(getCisAppealablePenaltiesUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubGetCisAppealablePenaltiesResponse()
        val jsonBody = Json.toJson(
          GetAppealablePenaltiesRequest(
            hmrcOfficeNumber = "123",
            schemeReferenceNumber = "AB456",
            startRow = 1,
            endRow = 10
          ))

        val result = wsClient.url(getCisAppealablePenaltiesUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call getCisAppealablePenalty" when {

      val testRequest = GetAppealablePenaltyRequest(
        penaltyId = 1L,
        hmrcOfficeNumber = "840123",
        schemeReferenceNumber = "098345"
      )

      "return a 200:OK:: authorised request" in {
        stubAuthorised()
        stubGetAppealablePenaltyResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(getAppealablePenaltyUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubGetAppealablePenaltyResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(getAppealablePenaltyUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call getCisTaxpayer" when {

      val testRequest = GetTaxpayerRequest(
        hmrcOfficeNumber = "123",
        schemeReferenceNumber = "AB456"
      )

      "return a 200:OK:: authorised request" in {
        stubAuthorised()
        stubGetCisTaxpayerResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(getCisTaxpayerUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubGetCisTaxpayerResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(getCisTaxpayerUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 404:NotFound:: authorised request when taxpayer does not exist" in {
        stubAuthorised()
        stubPost(getCisTaxpayerProxyUrl, Status.NOT_FOUND,
          Json.obj("message" -> "CIS taxpayer not found").toString)
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(getCisTaxpayerUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe NOT_FOUND
      }
    }

    "call getCisPenaltyAllowed" when {

      "return a 200:Ok:: authorised request" in {
        stubAuthorised()
        stubGetPenaltyAllowedResponse()
        val jsonBody = Json.toJson(
          GetPenaltyAllowedRequest(
            districtNumber = 1L,
            employerReference = "hdz",
            uniqueId = 1L
          ))

        val result = wsClient.url(getCisPenaltiesAllowedUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubGetPenaltyAllowedResponse()
        val jsonBody = Json.toJson(
          GetPenaltyAllowedRequest(
            districtNumber = 1L,
            employerReference = "hdz",
            uniqueId = 1L
          ))

        val result = wsClient.url(getCisPenaltiesAllowedUrl)
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
  }
}
