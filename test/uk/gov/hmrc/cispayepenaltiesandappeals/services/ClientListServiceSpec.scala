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

    "hasCisClient" - {

      val irAgentId = "123456"
      val credentialId = "CRED01"
      val hmrcOfficeNumber = "754"
      val schemeReferenceNumber = "EZ00100"
      val serviceName = "CIS"

      def hasCisClientRequest(schemeReferenceNumber: String = "EZ00100") =
        HasCisClientRequest(
          agentDetails = AgentDetails(credentialId = credentialId, irAgentId = irAgentId),
          contractorDetails = ContractorDetails(hmrcOfficeNumber = hmrcOfficeNumber, schemeReferenceNumber = schemeReferenceNumber)
        )

      val hasClientResponse = HasClientResponse(
        hasClient = true
      )

      "must delegate to connector (happy path)" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(hasClientResponse))

        val result: HasClientResponse = service.hasCisClient(hasCisClientRequest()).futureValue
        result mustBe hasClientResponse

        verify(connector).hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must return hasClient false when connector returns false" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val notClientResponse = HasClientResponse(hasClient = false)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(notClientResponse))

        val result: HasClientResponse = service.hasCisClient(hasCisClientRequest()).futureValue
        result mustBe notClientResponse

        verify(connector).hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate failures from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val boom = UpstreamErrorResponse("Service unavailable", 503)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.hasCisClient(hasCisClientRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must call connector exactly once per request" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(hasClientResponse))

        service.hasCisClient(hasCisClientRequest()).futureValue

        verify(connector, times(1)).hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must handle consecutive requests independently" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val request1 = hasCisClientRequest()
        val request2 = hasCisClientRequest("EZ00125")
        val request3 = hasCisClientRequest("EZ00150")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(hasClientResponse))
        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo("EZ00125"), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(hasClientResponse))
        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo("EZ00150"), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(hasClientResponse))

        service.hasCisClient(request1).futureValue mustBe hasClientResponse
        service.hasCisClient(request2).futureValue mustBe hasClientResponse
        service.hasCisClient(request3).futureValue mustBe hasClientResponse

        verify(connector, times(3)).hasClient(any(), any(), any(), any(), any())(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate RuntimeException from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val boom = new RuntimeException("Connection failed")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.hasCisClient(hasCisClientRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }
    }

    "hasPayeClient" - {

      val irAgentId = "123456"
      val credentialId = "CRED01"
      val hmrcOfficeNumber = "840"
      val schemeReferenceNumber = "MZ00064"
      val serviceName = "PAYE"

      def hasPayeClientRequest(schemeReferenceNumber: String = "MZ00064") =
        HasPayeClientRequest(
          agentDetails = AgentDetails(credentialId = credentialId, irAgentId = irAgentId),
          employerDetails = EmployerDetails(hmrcOfficeNumber = hmrcOfficeNumber, schemeReferenceNumber = schemeReferenceNumber)
        )

      val hasClientResponse = HasClientResponse(
        hasClient = true
      )

      "must delegate to connector (happy path)" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(hasClientResponse))

        val result: HasClientResponse = service.hasPayeClient(hasPayeClientRequest()).futureValue
        result mustBe hasClientResponse

        verify(connector).hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must return hasClient false when connector returns false" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val notClientResponse = HasClientResponse(hasClient = false)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(notClientResponse))

        val result: HasClientResponse = service.hasPayeClient(hasPayeClientRequest()).futureValue
        result mustBe notClientResponse

        verify(connector).hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate failures from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val boom = UpstreamErrorResponse("Service unavailable", 503)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.hasPayeClient(hasPayeClientRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must call connector exactly once per request" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(hasClientResponse))

        service.hasPayeClient(hasPayeClientRequest()).futureValue

        verify(connector, times(1)).hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must handle consecutive requests independently" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val request1 = hasPayeClientRequest()
        val request2 = hasPayeClientRequest("MZ00065")
        val request3 = hasPayeClientRequest("MZ00066")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(hasClientResponse))
        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo("MZ00065"), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(hasClientResponse))
        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo("MZ00066"), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.successful(hasClientResponse))

        service.hasPayeClient(request1).futureValue mustBe hasClientResponse
        service.hasPayeClient(request2).futureValue mustBe hasClientResponse
        service.hasPayeClient(request3).futureValue mustBe hasClientResponse

        verify(connector, times(3)).hasClient(any(), any(), any(), any(), any())(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate RuntimeException from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val mockAppConfig = mock[AppConfig]
        val service = new ClientListService(connector, mockAppConfig)
        val boom = new RuntimeException("Connection failed")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.hasPayeClient(hasPayeClientRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).hasClient(eqTo(irAgentId), eqTo(credentialId), eqTo(hmrcOfficeNumber), eqTo(schemeReferenceNumber), eqTo(serviceName))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }
    }
  }
}
