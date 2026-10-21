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
import play.api.http.Status.{CREATED, FORBIDDEN, OK}
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

  def stubCreateCisAppealResponse(): Unit = {
    stubPost("/rds-datacache-proxy/pas/cis/appeal", Status.CREATED,
      Json.toJson(CreateCisAppealResponse(created = true)).toString)
  }

  def stubGetCisAppealablePenaltiesResponse(): Unit = {
    stubPost("/rds-datacache-proxy/pas/cis/penalties", Status.OK,
      Json.toJson(GetCisAppealablePenaltiesResponse(
        total = 2,
        appealablePenalties = List(
          GetCisAppealablePenaltyResponse(
            uniqueId = Some(10001),
            issueDate = Some(LocalDate.of(2023, 1, 1)),
            taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
            penaltyType = Some("CIS003"),
            penaltyAmount = Some(BigDecimal(999.23))
          ),
          GetCisAppealablePenaltyResponse(
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
      Json.toJson(GetCisAppealablePenaltyResponse(
        uniqueId = None,
        issueDate = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
        penaltyType = Some("CIS003"),
        penaltyAmount = Some(BigDecimal(999.23))
      )).toString)
  }

  "ManageCisController" should {

    "call createCisAppeal" when {

      "return a 201:Created:: authorised request" in {
        stubAuthorised()
        stubCreateCisAppealResponse()
        val jsonBody = Json.toJson(
          CreateCisAppealRequest(penaltyId = 1L))

        val result = wsClient.url(createCisAppealUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubCreateCisAppealResponse()
        val jsonBody = Json.toJson(
          CreateCisAppealRequest(penaltyId = 1L))

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
          GetCisAppealablePenaltiesRequest(
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
          GetCisAppealablePenaltiesRequest(
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

      val testRequest = GetCisAppealablePenaltyRequest(
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
  }
}
