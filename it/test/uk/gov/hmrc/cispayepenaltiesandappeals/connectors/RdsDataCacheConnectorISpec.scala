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

  // CIS
  "createCisAppeal" should {

    val url = "/rds-datacache-proxy/pas/cis/appeal"

    val payload = CreateAppealRequest(penaltyId = 1)

    "return CreateAppealResponse when BE returns OK with valid JSON" in {
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

      result mustBe CreateAppealResponse(created = true)
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

    val payload = GetAppealablePenaltiesRequest(
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

    "return GetAppealablePenaltiesRequest when BE returns OK with valid JSON" in {
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

      result mustBe GetAppealablePenaltiesResponse(
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

    val payload = GetAppealablePenaltyRequest(
      penaltyId = 1L,
      hmrcOfficeNumber = "840123",
      schemeReferenceNumber = "098345"
    )

    "return GetAppealablePenaltyResponse when BE returns OK with valid JSON" in {
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

      result mustBe GetAppealablePenaltyResponse(
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

  "getCisTaxpayer" should {

    val url = "/rds-datacache-proxy/cis-taxpayer"

    val payload = GetTaxpayerRequest(
      hmrcOfficeNumber = "123",
      schemeReferenceNumber = "AB456"
    )

    "return GetCisTaxpayerResponse when BE returns OK with valid JSON" in {
      val payloadJson = Json.obj("taxOfficeNumber" -> "123", "taxOfficeReference" -> "AB456")

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "uniqueId": "1000123",
                   |  "taxOfficeNumber": "123",
                   |  "taxOfficeRef": "AB456",
                   |  "aoDistrict": "123",
                   |  "aoPayType": "P",
                   |  "aoCheckCode": "X",
                   |  "aoReference": "00123456",
                   |  "schemeName": "ABC Builders Ltd",
                   |  "utr": "1234567890"
                   |}""".stripMargin
              )
          )
      )

      val result = connector.getCisTaxpayer(payload).futureValue

      result mustBe GetCisTaxpayerResponse(
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
      )
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      val payloadJson = Json.obj("taxOfficeNumber" -> "123", "taxOfficeReference" -> "AB456")

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.getCisTaxpayer(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      val payloadJson = Json.obj("taxOfficeNumber" -> "123", "taxOfficeReference" -> "AB456")

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("Invalid request"))
      )

      val ex = intercept[Exception] {
        connector.getCisTaxpayer(payload).futureValue
      }
      ex.getMessage must include("400")
    }

    "propagate an upstream error when BE returns NOT_FOUND" in {
      val payloadJson = Json.obj("taxOfficeNumber" -> "123", "taxOfficeReference" -> "AB456")

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("""{"message":"CIS taxpayer not found"}"""))
      )

      val ex = intercept[Exception] {
        connector.getCisTaxpayer(payload).futureValue
      }
      ex.getMessage must include("404")
    }

    "propagate Exception when BE throws unexpected JSResult" in {
      val payloadJson = Json.obj("taxOfficeNumber" -> "123", "taxOfficeReference" -> "AB456")
      val connector = app.injector.instanceOf[RdsDataCacheConnector]

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "uniqueId": 1000123
                   |}""".stripMargin
              )
          )
      )

      val ex = intercept[Exception] {
        connector.getCisTaxpayer(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }

  }
  "getCisPenaltyAllowed" should {

    val url = "/rds-datacache-proxy/pas/cis/penalties/access-allowed"

    val payload = GetPenaltyAllowedRequest(
      districtNumber = 1L,
      employerReference = "hdz",
      uniqueId = 1L
    )

    val appealablePenaltiesResponse = Json.obj(
      "allowed" -> true
    )

    "return GetAppealablePenaltiesResponse when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(appealablePenaltiesResponse.toString())
          )
      )

      val result = connector.getCisPenaltyAllowed(payload).futureValue

      result mustBe GetPenaltyAllowedResponse(
        allowed = true
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
        connector.getCisPenaltyAllowed(payload).futureValue
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
        connector.getCisPenaltyAllowed(payload).futureValue
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
                   |  "uniqueId": "ANY_VALUE"
                   |}""".stripMargin
              )
          )
      )

      val ex = intercept[Exception] {
        connector.getCisPenaltyAllowed(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }
  }

  "hasCisClient" should {

    val url = "/rds-datacache-proxy/cis/has-client"

    val payload = HasCisClientRequest(
      agentDetails = AgentDetails(credentialId = "CRED01", irAgentId = "123456"),
      contractorDetails = ContractorDetails(hmrcOfficeNumber = "754", schemeReferenceNumber = "EZ00100")
    )

    def stubHasClient(status: Int, body: String): Unit =
      stubFor(
        get(urlPathEqualTo(url))
          .withQueryParam("irAgentId", equalTo("123456"))
          .withQueryParam("credentialId", equalTo("CRED01"))
          .withQueryParam("taxOfficeNumber", equalTo("754"))
          .withQueryParam("taxOfficeReference", equalTo("EZ00100"))
          .willReturn(aResponse().withStatus(status).withBody(body))
      )

    "return HasClientResponse true when BE returns OK with hasClient true" in {
      stubHasClient(OK, Json.obj("hasClient" -> true).toString())

      val result = connector.hasCisClient(payload).futureValue

      result mustBe HasClientResponse(hasClient = true)
    }

    "return HasClientResponse false when BE returns OK with hasClient false" in {
      stubHasClient(OK, Json.obj("hasClient" -> false).toString())

      val result = connector.hasCisClient(payload).futureValue

      result mustBe HasClientResponse(hasClient = false)
    }

    "propagate an upstream error when BE returns INTERNAL_SERVER_ERROR" in {
      stubHasClient(INTERNAL_SERVER_ERROR, "boom")

      val ex = intercept[Exception] {
        connector.hasCisClient(payload).futureValue
      }
      ex.getMessage must include("500")
    }

    "propagate an upstream error when BE returns BAD_REQUEST" in {
      stubHasClient(BAD_REQUEST, "Invalid request")

      val ex = intercept[Exception] {
        connector.hasCisClient(payload).futureValue
      }
      ex.getMessage must include("400")
    }

    "propagate Exception when BE throws unexpected JSResult" in {
      stubHasClient(
        OK,
        s"""{
           |  "uniqueId": "ANY_VALUE"
           |}""".stripMargin
      )

      val ex = intercept[Exception] {
        connector.hasCisClient(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }
  }

  // PAYE
  "getPayeAppealablePenalties" should {

    val url = "/rds-datacache-proxy/pas/paye/penalties"

    val payload = GetAppealablePenaltiesRequest(
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
          "penaltyType" -> "PAYE3",
          "penaltyAmount" -> 999.23
        ),
        Json.obj(
          "uniqueId" -> 10002,
          "issueDate" -> "2026-02-20",
          "taxPeriodEndDate" -> "2026-01-05",
          "penaltyType" -> "PAYE5",
          "penaltyAmount" -> 928.23
        )
      )
    )

    "return GetAppealablePenaltiesResponse when BE returns OK with valid JSON" in {
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

      val result = connector.getPayeAppealablePenalties(payload).futureValue

      result mustBe GetAppealablePenaltiesResponse(
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
            penaltyType = Some("PAYE5"),
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
        connector.getPayeAppealablePenalties(payload).futureValue
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
        connector.getPayeAppealablePenalties(payload).futureValue
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
        connector.getPayeAppealablePenalties(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }
  }

  "createPayeAppeal" should {

    val url = "/rds-datacache-proxy/pas/paye/appeal"

    val payload = CreateAppealRequest(penaltyId = 1)

    "return CreateAppealResponse when BE returns OK with valid JSON" in {
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

      result mustBe CreateAppealResponse(created = true)
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
        connector.createPayeAppeal(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }
  }

  "getPayeAppealablePenalty" should {

    val url = "/rds-datacache-proxy/pas/paye/penalty"

    val payload = GetAppealablePenaltyRequest(
      penaltyId = 1L,
      hmrcOfficeNumber = "840123",
      schemeReferenceNumber = "098345"
    )

    "return GetAppealablePenaltyResponse when BE returns OK with valid JSON" in {
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
                   |  "penaltyType": "PAYE3",
                   |  "penaltyAmount": 999.23
                   |}""".stripMargin
              )
          )
      )

      val result = connector.getPayeAppealablePenalty(payload).futureValue

      result mustBe GetAppealablePenaltyResponse(
        uniqueId = None,
        issueDate = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
        penaltyType = Some("PAYE3"),
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
        connector.getPayeAppealablePenalty(payload).futureValue
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
        connector.getPayeAppealablePenalty(payload).futureValue
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
        connector.getPayeAppealablePenalty(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }

  }
  "getPayeTaxpayer" should {

    val url = "/rds-datacache-proxy/pas/paye/taxpayer"

    val payload = GetTaxpayerRequest(
      hmrcOfficeNumber = "123",
      schemeReferenceNumber = "AB456"
    )

    "return GetPayeTaxpayerResponse when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                s"""{
                   |  "uniqueId": 1000123,
                   |  "instanceId": 2000123,
                   |  "districtNumber": "123",
                   |  "employerRef": "AB456",
                   |  "employerName1": "ABC"
                   |}""".stripMargin
              )
          )
      )

      val result = connector.getPayeTaxpayer(payload).futureValue

      result mustBe GetPayeTaxpayerResponse(
        uniqueId = 1000123L,
        instanceId = 2000123L,
        districtNumber = "123",
        employerRef = "AB456",
        employerName1 = Some("ABC"),
        employerName2 = None,
        agentOwnRef = None
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
        connector.getPayeTaxpayer(payload).futureValue
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
        connector.getPayeTaxpayer(payload).futureValue
      }
      ex.getMessage must include("400")
    }

    "propagate an upstream error when BE returns NOT_FOUND" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("""{"message":"PAYE taxpayer not found"}"""))
      )

      val ex = intercept[Exception] {
        connector.getPayeTaxpayer(payload).futureValue
      }
      ex.getMessage must include("404")
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
                   |  "uniqueId": "1000123"
                   |}""".stripMargin
              )
          )
      )

      val ex = intercept[Exception] {
        connector.getPayeTaxpayer(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }
  }
  "getPayePenaltyAllowed" should {

    val url = "/rds-datacache-proxy/pas/paye/penalties/access-allowed"

    val payload = GetPenaltyAllowedRequest(
      districtNumber = 1L,
      employerReference = "hdz",
      uniqueId = 1L
    )

    val appealablePenaltiesResponse = Json.obj(
      "allowed" -> true
    )

    "return GetAppealablePenaltiesResponse when BE returns OK with valid JSON" in {
      val payloadJson = Json.toJson(payload)

      stubFor(
        post(urlPathEqualTo(url))
          .withRequestBody(equalToJson(Json.stringify(payloadJson), true, true))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(appealablePenaltiesResponse.toString())
          )
      )

      val result = connector.getPayePenaltyAllowed(payload).futureValue

      result mustBe GetPenaltyAllowedResponse(
        allowed = true
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
        connector.getPayePenaltyAllowed(payload).futureValue
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
        connector.getPayePenaltyAllowed(payload).futureValue
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
                   |  "uniqueId": "ANY_VALUE"
                   |}""".stripMargin
              )
          )
      )

      val ex = intercept[Exception] {
        connector.getPayePenaltyAllowed(payload).futureValue
      }

      ex mustBe a[RuntimeException]
    }
  }

}
