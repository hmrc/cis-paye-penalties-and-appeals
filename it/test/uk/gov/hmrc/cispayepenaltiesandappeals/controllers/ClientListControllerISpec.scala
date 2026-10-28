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
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.cispayepenaltiesandappeals.base.BaseSpec
import uk.gov.hmrc.cispayepenaltiesandappeals.itUtil.ApplicationWithWiremock
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*

class ClientListControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/cis-paye-penalties-and-appeals"
  lazy val getClientListDownloadStatusUrl = s"$servicePrefix/agent/clientlist/status"

  def stubClientListStatusResponse(credentialId: String, serviceName: String, gracePeriodSeconds: Int): Unit = {
    stubGet(s"/rds-datacache-proxy/cis/client-list-status?credentialId=$credentialId&serviceName=$serviceName&gracePeriod=$gracePeriodSeconds", Status.OK,
      Json.toJson(ClientListStatusResponse("Succeeded")).toString)
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
  }
}
