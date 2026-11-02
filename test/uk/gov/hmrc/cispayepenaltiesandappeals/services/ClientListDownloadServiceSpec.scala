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

package uk.gov.hmrc.cispayepenaltiesandappeals.services

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase
import uk.gov.hmrc.cispayepenaltiesandappeals.connectors.ClientExchangeProxyConnector
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class ClientListDownloadServiceSpec extends SpecBase {

  private val request  = InitiateClientListDownloadRequest("CIS", "cred-1", "agent-1")
  private val response = InitiateClientListDownloadResponse(8000, Seq(1000, 2000))

  "ClientListDownloadService initiateClientListDownload" - {

    "must delegate to the connector" in {
      val connector = mock[ClientExchangeProxyConnector]
      when(connector.initiateClientListDownload(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val service = new ClientListDownloadService(connector)

      service.initiateClientListDownload(request).futureValue mustBe response
      verify(connector).initiateClientListDownload(eqTo(request))(any[HeaderCarrier])
    }

    "must propagate connector failures" in {
      val connector = mock[ClientExchangeProxyConnector]
      val error     = UpstreamErrorResponse("boom", 502)
      when(connector.initiateClientListDownload(any[InitiateClientListDownloadRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(error))

      val service = new ClientListDownloadService(connector)

      service.initiateClientListDownload(request).failed.futureValue mustBe error
    }
  }
}
