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

package uk.gov.hmrc.cispayepenaltiesandappeals.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.cispayepenaltiesandappeals.itUtil.ApplicationWithWiremock
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class RdsDataCacheConnectorISpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with ApplicationWithWiremock
  with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector: RdsDataCacheConnector = app.injector.instanceOf[RdsDataCacheConnector]

  "createCisAppeal" should {

    val url = "/rds-datacache-proxy/pas/cis/appeal"

    val payload = CreateCisAppealRequest(penaltyId = 1)

    "return CreateCisAppealResponse when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "created": true
                   |}""".stripMargin
              )
          )
      )

      val result = connector.createCisAppeal(payload).futureValue

      result mustBe CreateCisAppealResponse(created = true)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.createCisAppeal(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Invalid request"))
      )

      val ex = intercept[Exception] {
        connector.createCisAppeal(payload).futureValue
      }
      ex.getMessage must include("400")
    }

    "propagate Exception when BE throws unexpected JSResult" in {
      val payloadJson = Json.toJson(payload)
      val connector = app.injector.instanceOf[RdsDataCacheConnector]

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "created": "ANY_VALUE"
                   |}""".stripMargin
              )
          )
      )

      val ex = intercept[Exception] {
        connector.createCisAppeal(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }
  }

  "getCisAppealablePenalties" should {

    val url = "/rds-datacache-proxy/pas/cis/penalties"

    val payload = GetCisAppealablePenaltiesRequest(
      hmrcOfficeNumber = "123",
      schemeReferenceNumber = "AB456",
      startRow = 1,
      endRow = 10
    )

    val penaltiesReturnJson = Json.obj(
      "total" -> 2,
      "appealablePenalties" -> Json.arr(
        Json.obj(
          "uniqueId" -> 10001,
          "issueDate" -> "2023-01-01",
          "taxPeriodEndDate" -> "2023-04-01",
          "penaltyType" -> "CIS003",
          "penaltyAmount" -> 999.23
        ),
        Json.obj(
          "uniqueId" -> 10002,
          "issueDate" -> "2026-02-20",
          "taxPeriodEndDate" -> "2026-01-05",
          "penaltyType" -> "CIS005",
          "penaltyAmount" -> 928.23
        )
      )
    )

    "return GetCisAppealablePenaltiesRequest when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(penaltiesReturnJson.toString())
          )
      )

      val result = connector.getCisAppealablePenalties(payload).futureValue

      result mustBe GetCisAppealablePenaltiesResponse(
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
      )
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.getCisAppealablePenalties(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Invalid request"))
      )

      val ex = intercept[Exception] {
        connector.getCisAppealablePenalties(payload).futureValue
      }
      ex.getMessage must include("400")
    }

    "propagate Exception when BE throws unexpected JSResult" in {
      val payloadJson = Json.toJson(payload)
      val connector = app.injector.instanceOf[RdsDataCacheConnector]

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "hmrcOfficeNumber": "ANY_VALUE"
                   |}""".stripMargin
              )
          )
      )

      val ex = intercept[Exception] {
        connector.getCisAppealablePenalties(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }
  }

  "getCisAppealablePenalty" should {

    val url = "/rds-datacache-proxy/pas/cis/penalty"

    val payload = GetCisAppealablePenaltyRequest(
      penaltyId = 1L,
      hmrcOfficeNumber = "840123",
      schemeReferenceNumber = "098345"
    )

    "return GetCisAppealablePenaltyResponse when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "issueDate": "2023-01-01",
                   |  "taxPeriodEndDate": "2023-04-01",
                   |  "penaltyType": "CIS003",
                   |  "penaltyAmount": 999.23
                   |}""".stripMargin
              )
          )
      )

      val result = connector.getCisAppealablePenalty(payload).futureValue

      result mustBe GetCisAppealablePenaltyResponse(
        uniqueId = None,
        issueDate = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
        penaltyType = Some("CIS003"),
        penaltyAmount = Some(BigDecimal("999.23"))
      )
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.getCisAppealablePenalty(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Invalid request"))
      )

      val ex = intercept[Exception] {
        connector.getCisAppealablePenalty(payload).futureValue
      }
      ex.getMessage must include("400")
    }

    "propagate Exception when BE throws unexpected JSResult" in {
      val payloadJson = Json.toJson(payload)
      val connector = app.injector.instanceOf[RdsDataCacheConnector]

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "penaltyAmount": "invalid"
                   |}""".stripMargin
              )
          )
      )

      val ex = intercept[Exception] {
        connector.getCisAppealablePenalty(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }

  }

  "createPayeAppeal" should {

    val url = "/rds-datacache-proxy/pas/paye/appeal"

    val payload = CreatePayeAppealRequest(penaltyId = 1)

    "return CreatePayeAppealResponse when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "created": true
                   |}""".stripMargin
              )
          )
      )

      val result = connector.createPayeAppeal(payload).futureValue

      result mustBe CreatePayeAppealResponse(created = true)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.createPayeAppeal(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Invalid request"))
      )

      val ex = intercept[Exception] {
        connector.createPayeAppeal(payload).futureValue
      }
      ex.getMessage must include("400")
    }

    "propagate Exception when BE returns unexpected JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "created": "ANY_VALUE"
                   |}""".stripMargin
              )
          )
      )

      val ex = intercept[Exception] {
        connector.createPayeAppeal(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }
  }
}
