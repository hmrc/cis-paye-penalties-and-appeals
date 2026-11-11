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

import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.cispayepenaltiesandappeals.base.BaseSpec
import uk.gov.hmrc.cispayepenaltiesandappeals.itUtil.ApplicationWithWiremock
import uk.gov.hmrc.cispayepenaltiesandappeals.models.AgentAuthoriseRequest

class AgentAuthorisationControllerISpec extends BaseSpec
  with GuiceOneServerPerSuite with ApplicationWithWiremock {

  private val servicePrefix = s"http://localhost:$port/cis-paye-penalties-and-appeals"
  private lazy val authoriseUrl = s"$servicePrefix/cis/agent/authorise"

  private val statusUrl =
    "/rds-datacache-proxy/pas/agent/clientlist/status?credentialId=credId&serviceName=CIS&gracePeriod=14400"
  private val hasClientUrl =
    "/rds-datacache-proxy/cis/has-client?irAgentId=123456&credentialId=credId&taxOfficeNumber=123&taxOfficeReference=AB456"
  private val initiateUrl = "/client-exchange-proxy/CIS/credId/123456/clientlist"
  private val proxyBody =
    """<gwe:AsynchronousProcessWaitTime browserInterval="10" xmlns:gwe="gwe">
      |<BusinessServiceInterval>10</BusinessServiceInterval>
      |<BusinessServiceInterval>10</BusinessServiceInterval>
      |</gwe:AsynchronousProcessWaitTime>""".stripMargin

  private val body = Json.toJson(AgentAuthoriseRequest("123", "AB456"))

  private def post(payload: play.api.libs.json.JsValue = body) =
    wsClient.url(authoriseUrl).withHttpHeaders("Authorization" -> "Bearer123").post(payload)

  "AgentAuthorisationController" should {

    "return 200 authorised true:: status Succeeded and the agent has the client" in {
      stubAuthorisedAgent()
      stubGet(statusUrl, OK, """{"status":"Succeeded"}""")
      stubGet(hasClientUrl, OK, """{"hasClient":true}""")

      val result = post()

      result.status shouldBe OK
      Json.parse(result.body) shouldBe Json.obj("authorised" -> true)
    }

    "return 200 authorised false:: status Succeeded but the agent has no client" in {
      stubAuthorisedAgent()
      stubGet(statusUrl, OK, """{"status":"Succeeded"}""")
      stubGet(hasClientUrl, OK, """{"hasClient":false}""")

      val result = post()

      result.status shouldBe OK
      Json.parse(result.body) shouldBe Json.obj("authorised" -> false, "reason" -> "NoClientRelationship")
    }

    "return 200 authorised false:: status Failed" in {
      stubAuthorisedAgent()
      stubGet(statusUrl, OK, """{"status":"Failed"}""")

      val result = post()

      result.status shouldBe OK
      Json.parse(result.body) shouldBe Json.obj("authorised" -> false, "reason" -> "Failed")
    }

    "initiate the download and poll until Succeeded" in {
      stubAuthorisedAgent()
      stubGetInScenario(statusUrl, "download", Scenario.STARTED, Some("initiated"), OK, """{"status":"InitiateDownload"}""")
      stubGetInScenario(statusUrl, "download", "initiated", None, OK, """{"status":"Succeeded"}""")
      stubGet(initiateUrl, OK, proxyBody)
      stubGet(hasClientUrl, OK, """{"hasClient":true}""")

      val result = post()

      result.status shouldBe OK
      Json.parse(result.body) shouldBe Json.obj("authorised" -> true)
    }

    "return 400:: invalid tax office number" in {
      stubAuthorisedAgent()

      val result = post(Json.toJson(AgentAuthoriseRequest("12", "AB456")))

      result.status shouldBe BAD_REQUEST
    }

    "return 400:: malformed payload" in {
      stubAuthorisedAgent()

      val result = post(Json.obj("x" -> "y"))

      result.status shouldBe BAD_REQUEST
    }

    "return 500:: has-client call fails" in {
      stubAuthorisedAgent()
      stubGet(statusUrl, OK, """{"status":"Succeeded"}""")
      stubGet(hasClientUrl, INTERNAL_SERVER_ERROR, "{}")

      val result = post()

      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return 403:: agent without an IR-PAYE-AGENT enrolment" in {
      stubAuthorisedAgentWithoutEnrolment()

      post().status shouldBe FORBIDDEN
    }

    "return 403:: organisation user" in {
      stubAuthorised()

      post().status shouldBe FORBIDDEN
    }

    "return 403:: unauthorised request" in {
      stubUnauthorised()

      post().status shouldBe FORBIDDEN
    }
  }
}
