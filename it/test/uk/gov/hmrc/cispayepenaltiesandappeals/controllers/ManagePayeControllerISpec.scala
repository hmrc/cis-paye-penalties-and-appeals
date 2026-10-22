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
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.cispayepenaltiesandappeals.base.BaseSpec
import uk.gov.hmrc.cispayepenaltiesandappeals.models.{GetAppealablePenaltiesRequest, GetAppealablePenaltiesResponse, GetAppealablePenaltyResponse}

import java.time.LocalDate

class ManagePayeControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/cis-paye-penalties-and-appeals"
  lazy val getPayeAppealablePenaltiesUrl = s"$servicePrefix/paye/penalties"

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
  }
}
