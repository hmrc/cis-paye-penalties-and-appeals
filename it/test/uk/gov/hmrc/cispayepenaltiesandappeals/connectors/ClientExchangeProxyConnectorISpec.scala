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

package uk.gov.hmrc.cispayepenaltiesandappeals.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.*
import uk.gov.hmrc.cispayepenaltiesandappeals.itUtil.ApplicationWithWiremock
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

class ClientExchangeProxyConnectorISpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with ApplicationWithWiremock
  with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector: ClientExchangeProxyConnector = app.injector.instanceOf[ClientExchangeProxyConnector]

  private def request(service: String) = InitiateClientListDownloadRequest(service, "cred-1", "agent-1")
  private def url(service: String)     = s"/client-exchange-proxy/$service/cred-1/agent-1/clientlist"

  private def xml(browserInterval: String, intervals: String*): String =
    s"""<gwe:AsynchronousProcessWaitTime browserInterval="$browserInterval" xmlns:gwe="gwe">
       |${intervals.map(i => s"<BusinessServiceInterval>$i</BusinessServiceInterval>").mkString("\n")}
       |</gwe:AsynchronousProcessWaitTime>""".stripMargin

  "initiateClientListDownload" should {

    "return the intervals for CIS" in {
      stubGet(url("CIS"), OK, xml("8000", "1000", "2000", "3000"))

      connector.initiateClientListDownload(request("CIS")).futureValue mustBe
        InitiateClientListDownloadResponse(8000, List(1000, 2000, 3000))
    }

    "return the intervals for PAYE" in {
      stubGet(url("PAYE"), OK, xml("5000", "500"))

      connector.initiateClientListDownload(request("PAYE")).futureValue mustBe
        InitiateClientListDownloadResponse(5000, List(500))
    }

    "return an empty list when there are no business service intervals" in {
      stubGet(url("CIS"), OK, xml("8000"))

      connector.initiateClientListDownload(request("CIS")).futureValue mustBe
        InitiateClientListDownloadResponse(8000, Nil)
    }

    "fail with the upstream status when the proxy returns 404" in {
      stubGet(url("CIS"), NOT_FOUND, "")

      val error = connector.initiateClientListDownload(request("CIS")).failed.futureValue
      error mustBe a[UpstreamErrorResponse]
      error.asInstanceOf[UpstreamErrorResponse].statusCode mustBe NOT_FOUND
    }

    "fail with the upstream status when the proxy returns 500" in {
      stubGet(url("CIS"), INTERNAL_SERVER_ERROR, "")

      val error = connector.initiateClientListDownload(request("CIS")).failed.futureValue
      error.asInstanceOf[UpstreamErrorResponse].statusCode mustBe INTERNAL_SERVER_ERROR
    }

    "fail when the body is not well-formed XML" in {
      stubGet(url("CIS"), OK, "<not-closed")

      connector.initiateClientListDownload(request("CIS")).failed.futureValue mustBe a[Throwable]
    }

    "fail when the root element is unexpected" in {
      stubGet(url("CIS"), OK, """<other browserInterval="1"/>""")

      connector.initiateClientListDownload(request("CIS")).failed.futureValue mustBe a[Throwable]
    }

    "fail when browserInterval is missing" in {
      stubGet(url("CIS"), OK, "<gwe:AsynchronousProcessWaitTime xmlns:gwe=\"gwe\"/>")

      connector.initiateClientListDownload(request("CIS")).failed.futureValue mustBe a[Throwable]
    }

    "fail when browserInterval is not a number" in {
      stubGet(url("CIS"), OK, xml("abc", "1000"))

      connector.initiateClientListDownload(request("CIS")).failed.futureValue mustBe a[Throwable]
    }

    "fail when a business service interval is negative" in {
      stubGet(url("CIS"), OK, xml("8000", "-5"))

      connector.initiateClientListDownload(request("CIS")).failed.futureValue mustBe a[Throwable]
    }

    "reject a body containing a DOCTYPE" in {
      val body = """<!DOCTYPE foo [<!ENTITY x "y">]><gwe:AsynchronousProcessWaitTime browserInterval="1" xmlns:gwe="gwe"/>"""
      stubGet(url("CIS"), OK, body)

      connector.initiateClientListDownload(request("CIS")).failed.futureValue mustBe a[Throwable]
    }
  }
}
