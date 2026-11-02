/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.cispayepenaltiesandappeals.base.BaseSpec
import uk.gov.hmrc.cispayepenaltiesandappeals.itUtil.ApplicationWithWiremock
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*

class ClientListDownloadControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  val servicePrefix = s"http://localhost:$port/cis-paye-penalties-and-appeals"
  lazy val initiateUrl = s"$servicePrefix/agent/clientlist"

  private val proxyUrl = "/client-exchange-proxy/CIS/cred-1/agent-1/clientlist"
  private val proxyBody =
    """<gwe:AsynchronousProcessWaitTime browserInterval="8000" xmlns:gwe="gwe">
      |<BusinessServiceInterval>1000</BusinessServiceInterval>
      |<BusinessServiceInterval>2000</BusinessServiceInterval>
      |</gwe:AsynchronousProcessWaitTime>""".stripMargin

  private val jsonBody = Json.toJson(InitiateClientListDownloadRequest("CIS", "cred-1", "agent-1"))

  "ClientListDownloadController" should {

    "return a 200:OK with the intervals:: authorised request" in {
      stubAuthorised()
      stubGet(proxyUrl, OK, proxyBody)

      val result = wsClient.url(initiateUrl)
        .withHttpHeaders("Authorization" -> "Bearer123")
        .post(jsonBody)

      result.status shouldBe OK
      Json.parse(result.body) shouldBe
        Json.obj("browserInterval" -> 8000, "businessServiceIntervals" -> Json.arr(1000, 2000))
    }

    "return a 400:BadRequest:: invalid payload" in {
      stubAuthorised()

      val result = wsClient.url(initiateUrl)
        .withHttpHeaders("Authorization" -> "Bearer123")
        .post(Json.obj("serviceId" -> "VAT", "credentialId" -> "cred-1", "agentId" -> "agent-1"))

      result.status shouldBe BAD_REQUEST
    }

    "return a 403:Forbidden:: unauthorised request" in {
      stubUnauthorised()
      stubGet(proxyUrl, OK, proxyBody)

      val result = wsClient.url(initiateUrl)
        .withHttpHeaders("Authorization" -> "Bearer123")
        .post(jsonBody)

      result.status shouldBe FORBIDDEN
    }

    "return a 500:InternalServerError:: client exchange proxy fails" in {
      stubAuthorised()
      stubGet(proxyUrl, INTERNAL_SERVER_ERROR, "")

      val result = wsClient.url(initiateUrl)
        .withHttpHeaders("Authorization" -> "Bearer123")
        .post(jsonBody)

      result.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
