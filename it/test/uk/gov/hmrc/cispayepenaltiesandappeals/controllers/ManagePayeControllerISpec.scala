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
import play.api.http.Status.{BAD_REQUEST, CREATED, FORBIDDEN}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.cispayepenaltiesandappeals.base.BaseSpec
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*

class ManagePayeControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/cis-paye-penalties-and-appeals"
  lazy val createPayeAppealUrl = s"$servicePrefix/paye/appeal"

  def stubCreatePayeAppealResponse(): Unit = {
    stubPost("/rds-datacache-proxy/pas/paye/appeal", Status.CREATED,
      Json.toJson(CreatePayeAppealResponse(created = true)).toString)
  }

  "ManagePayeController" should {

    "call createPayeAppeal" when {

      "return a 201:Created:: authorised request" in {
        stubAuthorised()
        stubCreatePayeAppealResponse()
        val jsonBody = Json.toJson(
          CreatePayeAppealRequest(penaltyId = 1L))

        val result = wsClient.url(createPayeAppealUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe CREATED
      }

      "return a 400:Bad Request:: invalid payload" in {
        stubAuthorised()
        stubCreatePayeAppealResponse()

        val result = wsClient.url(createPayeAppealUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(Json.obj("invalid" -> "data"))

        result.status shouldBe BAD_REQUEST
      }

      "return a 403:Forbidden:: unauthorised request" in {
        stubUnauthorised()
        stubCreatePayeAppealResponse()
        val jsonBody = Json.toJson(
          CreatePayeAppealRequest(penaltyId = 1L))

        val result = wsClient.url(createPayeAppealUrl)
          .withHttpHeaders("Authorization" -> "Bearer123")
          .post(jsonBody)

        result.status shouldBe FORBIDDEN
      }
    }
  }
}
