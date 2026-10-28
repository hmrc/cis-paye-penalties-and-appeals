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
import uk.gov.hmrc.cispayepenaltiesandappeals.config.AppConfig
import uk.gov.hmrc.cispayepenaltiesandappeals.connectors.RdsDataCacheConnector
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

final class ClientListServiceSpec extends SpecBase {

  "ClientListService" - {

    "getClientListDownloadStatus" - {

      val credentialId = "123"
      val serviceName = "CIS"
      val gracePeriodSeconds = 14400

      def clientListStatusRequest(credentialId: String = "123", serviceId: String = "CIS") =
        ClientListStatusRequest(credentialId, serviceId)

      val clientListStatusResponse = ClientListStatusResponse(status = "Succeeded")

      "must delegate to connector (happy path)" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getClientListDownloadStatus(eqTo(credentialId), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier]))
          .thenReturn(Future.successful(clientListStatusResponse))
        when(mockAppConfig.gracePeriod)
          .thenReturn(gracePeriodSeconds)

        val result: ClientListStatusResponse = service.getClientListDownloadStatus(clientListStatusRequest()).futureValue
        result mustBe clientListStatusResponse

        verify(connector).getClientListDownloadStatus(eqTo(credentialId), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate failures from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val boom = UpstreamErrorResponse("Service unavailable", 503)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getClientListDownloadStatus(eqTo(credentialId), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))
        when(mockAppConfig.gracePeriod)
          .thenReturn(gracePeriodSeconds)

        val ex: Throwable = service.getClientListDownloadStatus(clientListStatusRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).getClientListDownloadStatus(eqTo(credentialId), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must call connector exactly once per request" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getClientListDownloadStatus(eqTo(credentialId), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier]))
          .thenReturn(Future.successful(clientListStatusResponse))
        when(mockAppConfig.gracePeriod)
          .thenReturn(gracePeriodSeconds)

        service.getClientListDownloadStatus(clientListStatusRequest()).futureValue

        verify(connector, times(1)).getClientListDownloadStatus(eqTo(credentialId), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must handle consecutive requests independently" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val request1 = clientListStatusRequest()
        val request2 = clientListStatusRequest(credentialId = "124")
        val request3 = clientListStatusRequest(credentialId = "125")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getClientListDownloadStatus(eqTo(credentialId), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier]))
          .thenReturn(Future.successful(clientListStatusResponse))
        when(connector.getClientListDownloadStatus(eqTo("124"), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier]))
          .thenReturn(Future.successful(clientListStatusResponse))
        when(connector.getClientListDownloadStatus(eqTo("125"), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier]))
          .thenReturn(Future.successful(clientListStatusResponse))
        when(mockAppConfig.gracePeriod)
          .thenReturn(gracePeriodSeconds)

        service.getClientListDownloadStatus(request1).futureValue mustBe clientListStatusResponse
        service.getClientListDownloadStatus(request2).futureValue mustBe clientListStatusResponse
        service.getClientListDownloadStatus(request3).futureValue mustBe clientListStatusResponse

        verify(connector, times(3)).getClientListDownloadStatus(any(), any(), any())(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate RuntimeException from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val boom = new RuntimeException("Connection failed")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getClientListDownloadStatus(eqTo(credentialId), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))
        when(mockAppConfig.gracePeriod)
          .thenReturn(gracePeriodSeconds)

        val ex: Throwable = service.getClientListDownloadStatus(clientListStatusRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).getClientListDownloadStatus(eqTo(credentialId), eqTo(serviceName), eqTo(gracePeriodSeconds))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }
    }
  }
}
