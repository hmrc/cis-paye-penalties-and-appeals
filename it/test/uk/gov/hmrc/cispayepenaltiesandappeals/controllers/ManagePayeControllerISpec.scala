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
import play.api.http.Status.{CREATED, FORBIDDEN, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.cispayepenaltiesandappeals.base.BaseSpec
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*

import java.time.LocalDate

class ManagePayeControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/cis-paye-penalties-and-appeals"
  lazy val getPayeAppealablePenaltiesUrl = s"$servicePrefix/paye/penalties"
  lazy val getAppealablePenaltyUrl = s"$servicePrefix/paye/penalty"
  lazy val createPayeAppealUrl = s"$servicePrefix/paye/appeal"
  lazy val getPayeTaxpayerUrl = s"$servicePrefix/paye/taxpayer"
  val getPayeTaxpayerProxyUrl = "/rds-datacache-proxy/pas/paye/taxpayer"
  lazy val getPayePenaltiesAllowedUrl = s"$servicePrefix/paye/penalties/access-allowed"

  def stubCreatePayeAppealResponse(): Unit =
    stubPost("/rds-datacache-proxy/pas/paye/appeal", Status.CREATED,
      Json.toJson(CreateAppealResponse(created = true)).toString)

  def stubGetPayeAppealablePenaltiesResponse(): Unit = {
    stubPost("/rds-datacache-proxy/pas/paye/penalties", Status.OK,
      Json.toJson(GetAppealablePenaltiesResponse(
        total = 2,
        appealablePenalties = List(
          GetAppealablePenaltyResponse(
            uniqueId = Some(10001),
            issueDate = Some(LocalDate.of(2023, 1, 1)),
            taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
            penaltyType = Some("PAYE3"),
            penaltyAmount = Some(BigDecimal(999.23))
          ),
          GetAppealablePenaltyResponse(
            uniqueId = Some(10002),
            issueDate = Some(LocalDate.of(2026, 2, 20)),
            taxPeriodEndDate = Some(LocalDate.of(2026, 1, 5)),
            penaltyType = Some("PAYE3"),
            penaltyAmount = Some(BigDecimal(928.23))
          )
        )
      )).toString)
  }

  def stubGetAppealablePenaltyResponse(): Unit = {
    stubPost("/rds-datacache-proxy/pas/paye/penalty", Status.OK,
      Json.toJson(GetAppealablePenaltyResponse(
        uniqueId = None,
        issueDate = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
        penaltyType = Some("PAYE3"),
        penaltyAmount = Some(BigDecimal(999.23))
      )).toString)
  }

  def stubGetPayeTaxpayerResponse(): Unit = {
    stubPost(getPayeTaxpayerProxyUrl, Status.OK,
      Json.toJson(GetPayeTaxpayerResponse(
        uniqueId = 1000123L,
        instanceId = 2000123L,
        districtNumber = "123",
        employerRef = "AB456",
        employerName1 = Some("ABC"),
        employerName2 = None,
        agentOwnRef = None
      )).toString)
  }

  def stubGetPenaltyAllowedResponse(): Unit = {
    stubPost("/rds-datacache-proxy/pas/paye/penalties/access-allowed", Status.OK,
      Json.toJson(GetPenaltyAllowedResponse(allowed = true)).toString)
  }

  "PayeController" should {

    "call getPayeAppealablePenalties" when {

      val jsonBody = Json.toJson(
        GetAppealablePenaltiesRequest(
          hmrcOfficeNumber = "123",
          schemeReferenceNumber = "AB456",
          startRow = 1L,
          endRow = 10L
        ))

      "return a 200:Ok:: authorised request" in {
        stubAuthorised()
        stubGetPayeAppealablePenaltiesResponse()

        val result = wsClient.url(getPayeAppealablePenaltiesUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubGetPayeAppealablePenaltiesResponse()

        val result = wsClient.url(getPayeAppealablePenaltiesUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call getPayeAppealablePenalty" when {

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

    "call createPayeAppeal" when {

      val jsonBody = Json.toJson(CreateAppealRequest(penaltyId = 1L))

      "return a 201:Created:: authorised request" in {
        stubAuthorised()
        stubCreatePayeAppealResponse()

        val result = wsClient.url(createPayeAppealUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubCreatePayeAppealResponse()

        val result = wsClient.url(createPayeAppealUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }

    "call getPayeTaxpayer" when {

      val testRequest = GetTaxpayerRequest(
        hmrcOfficeNumber = "123",
        schemeReferenceNumber = "AB456"
      )

      "return a 200:OK:: authorised request" in {
        stubAuthorised()
        stubGetPayeTaxpayerResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(getPayeTaxpayerUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubGetPayeTaxpayerResponse()
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(getPayeTaxpayerUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }

      "return a 404:NotFound:: authorised request when taxpayer does not exist" in {
        stubAuthorised()
        stubPost(getPayeTaxpayerProxyUrl, Status.NOT_FOUND,
          Json.obj("message" -> "PAYE taxpayer not found").toString)
        val jsonBody = Json.toJson(testRequest)

        val result = wsClient.url(getPayeTaxpayerUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe NOT_FOUND
      }
    }

    "call getPayePenaltyAllowed" when {

      val jsonBody = Json.toJson(
        GetPenaltyAllowedRequest(
          districtNumber = 1L,
          employerReference = "hdz",
          uniqueId = 1L
        ))

      "return a 200:Ok:: authorised request" in {
        stubAuthorised()
        stubGetPenaltyAllowedResponse()

        val result = wsClient.url(getPayePenaltiesAllowedUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe OK
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubGetPenaltyAllowedResponse()

        val result = wsClient.url(getPayePenaltiesAllowedUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }
  }
}
