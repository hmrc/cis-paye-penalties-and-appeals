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

import uk.gov.hmrc.cispayepenaltiesandappeals.base.SpecBase
import uk.gov.hmrc.cispayepenaltiesandappeals.connectors.RdsDataCacheConnector
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import uk.gov.hmrc.cispayepenaltiesandappeals.models.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.Future

final class CisServiceSpec extends SpecBase {

  "CisService" - {

    "createCisAppeal" - {

      def createCisAppealRequest(penaltyId: Long = 1L) = CreateCisAppealRequest(penaltyId = penaltyId)

      val createCisAppealResponse = CreateCisAppealResponse(created = true)

      "must delegate to connector (happy path)" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.createCisAppeal(eqTo(createCisAppealRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.successful(createCisAppealResponse))

        val result: CreateCisAppealResponse = service.createCisAppeal(createCisAppealRequest()).futureValue
        result mustBe createCisAppealResponse

        verify(connector).createCisAppeal(eqTo(createCisAppealRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate failures from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val boom = UpstreamErrorResponse("Service unavailable", 503)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.createCisAppeal(eqTo(createCisAppealRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.createCisAppeal(createCisAppealRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).createCisAppeal(eqTo(createCisAppealRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must call connector exactly once per request" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.createCisAppeal(eqTo(createCisAppealRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.successful(createCisAppealResponse))

        service.createCisAppeal(createCisAppealRequest()).futureValue

        verify(connector, times(1)).createCisAppeal(eqTo(createCisAppealRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must handle consecutive requests independently" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val request1 = createCisAppealRequest()
        val request2 = createCisAppealRequest(2L)
        val request3 = createCisAppealRequest(3L)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.createCisAppeal(eqTo(request1))(any[HeaderCarrier]))
          .thenReturn(Future.successful(createCisAppealResponse))
        when(connector.createCisAppeal(eqTo(request2))(any[HeaderCarrier]))
          .thenReturn(Future.successful(createCisAppealResponse))
        when(connector.createCisAppeal(eqTo(request3))(any[HeaderCarrier]))
          .thenReturn(Future.successful(createCisAppealResponse))

        service.createCisAppeal(request1).futureValue mustBe createCisAppealResponse
        service.createCisAppeal(request2).futureValue mustBe createCisAppealResponse
        service.createCisAppeal(request3).futureValue mustBe createCisAppealResponse

        verify(connector, times(3)).createCisAppeal(any[CreateCisAppealRequest])(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate RuntimeException from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val boom = new RuntimeException("Connection failed")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.createCisAppeal(eqTo(createCisAppealRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.createCisAppeal(createCisAppealRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).createCisAppeal(eqTo(createCisAppealRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }
    }

    "getCisAppealablePenalty" - {

      def getCisAppealablePenaltyRequest(penaltyId: Long = 1L) =
        GetCisAppealablePenaltyRequest(
          penaltyId = penaltyId,
          hmrcOfficeNumber = "840123",
          schemeReferenceNumber = "098345"
        )

      val getCisAppealablePenaltyResponse = GetCisAppealablePenaltyResponse(
        uniqueId = None,
        issueDate = Some(LocalDate.of(2023, 1, 1)),
        taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
        penaltyType = Some("CIS003"),
        penaltyAmount = Some(BigDecimal("999.23"))
      )

      "must delegate to connector (happy path)" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisAppealablePenalty(eqTo(getCisAppealablePenaltyRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.successful(getCisAppealablePenaltyResponse))

        val result: GetCisAppealablePenaltyResponse = service.getCisAppealablePenalty(getCisAppealablePenaltyRequest()).futureValue
        result mustBe getCisAppealablePenaltyResponse

        verify(connector).getCisAppealablePenalty(eqTo(getCisAppealablePenaltyRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate failures from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val boom = UpstreamErrorResponse("Service unavailable", 503)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisAppealablePenalty(eqTo(getCisAppealablePenaltyRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.getCisAppealablePenalty(getCisAppealablePenaltyRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).getCisAppealablePenalty(eqTo(getCisAppealablePenaltyRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must call connector exactly once per request" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisAppealablePenalty(eqTo(getCisAppealablePenaltyRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.successful(getCisAppealablePenaltyResponse))

        service.getCisAppealablePenalty(getCisAppealablePenaltyRequest()).futureValue

        verify(connector, times(1)).getCisAppealablePenalty(eqTo(getCisAppealablePenaltyRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must handle consecutive requests independently" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val request1 = getCisAppealablePenaltyRequest()
        val request2 = getCisAppealablePenaltyRequest(2L)
        val request3 = getCisAppealablePenaltyRequest(3L)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisAppealablePenalty(eqTo(request1))(any[HeaderCarrier]))
          .thenReturn(Future.successful(getCisAppealablePenaltyResponse))
        when(connector.getCisAppealablePenalty(eqTo(request2))(any[HeaderCarrier]))
          .thenReturn(Future.successful(getCisAppealablePenaltyResponse))
        when(connector.getCisAppealablePenalty(eqTo(request3))(any[HeaderCarrier]))
          .thenReturn(Future.successful(getCisAppealablePenaltyResponse))

        service.getCisAppealablePenalty(request1).futureValue mustBe getCisAppealablePenaltyResponse
        service.getCisAppealablePenalty(request2).futureValue mustBe getCisAppealablePenaltyResponse
        service.getCisAppealablePenalty(request3).futureValue mustBe getCisAppealablePenaltyResponse

        verify(connector, times(3)).getCisAppealablePenalty(any[GetCisAppealablePenaltyRequest])(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate RuntimeException from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val boom = new RuntimeException("Connection failed")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisAppealablePenalty(eqTo(getCisAppealablePenaltyRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.getCisAppealablePenalty(getCisAppealablePenaltyRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).getCisAppealablePenalty(eqTo(getCisAppealablePenaltyRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }
    }
  }
}
