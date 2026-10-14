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
import uk.gov.hmrc.cispayepenaltiesandappeals.models.{CreateCisAppealRequest, CreateCisAppealResponse}
import uk.gov.hmrc.http.HeaderCarrier

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

}