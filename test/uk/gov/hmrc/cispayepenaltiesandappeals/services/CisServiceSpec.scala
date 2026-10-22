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

    "getCisAppealablePenalties" - {

      def testRequest(hmrcOfficeNum: String = "123", schemeRefNum: String = "AB456") = GetAppealablePenaltiesRequest(
        hmrcOfficeNumber = hmrcOfficeNum,
        schemeReferenceNumber = schemeRefNum,
        startRow = 1L,
        endRow = 10L
      )

      val testResponse = GetAppealablePenaltiesResponse(
        total = 2,
        appealablePenalties = List(
          GetAppealablePenaltyResponse(
            uniqueId = Some(10001),
            issueDate = Some(LocalDate.of(2023, 1, 1)),
            taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
            penaltyType = Some("CIS03"),
            penaltyAmount = Some(BigDecimal(999.23))
          ),
          GetAppealablePenaltyResponse(
            uniqueId = Some(10002),
            issueDate = Some(LocalDate.of(2023, 1, 1)),
            taxPeriodEndDate = Some(LocalDate.of(2023, 4, 1)),
            penaltyType = Some("CIS03"),
            penaltyAmount = Some(BigDecimal(999.23))
          )
        )
      )

      "must delegate to connector (happy path)" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        val result: GetAppealablePenaltiesResponse = service.getCisAppealablePenalties(testRequest()).futureValue
        result mustBe testResponse

        verify(connector).getCisAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate failures from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val boom = UpstreamErrorResponse("Service unavailable", 503)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.getCisAppealablePenalties(testRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).getCisAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must call connector exactly once per request" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        service.getCisAppealablePenalties(testRequest()).futureValue

        verify(connector, times(1)).getCisAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must handle consecutive requests independently" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val request1 = testRequest()
        val request2 = testRequest("456", "CD789")
        val request3 = testRequest("789", "EF123")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisAppealablePenalties(eqTo(request1))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))
        when(connector.getCisAppealablePenalties(eqTo(request2))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))
        when(connector.getCisAppealablePenalties(eqTo(request3))(any[HeaderCarrier]))
          .thenReturn(Future.successful(testResponse))

        service.getCisAppealablePenalties(request1).futureValue mustBe testResponse
        service.getCisAppealablePenalties(request2).futureValue mustBe testResponse
        service.getCisAppealablePenalties(request3).futureValue mustBe testResponse

        verify(connector, times(3)).getCisAppealablePenalties(any[GetAppealablePenaltiesRequest])(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate RuntimeException from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val boom = new RuntimeException("Connection failed")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.getCisAppealablePenalties(testRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).getCisAppealablePenalties(eqTo(testRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }
    }

    "getCisAppealablePenalty" - {

      def getCisAppealablePenaltyRequest(penaltyId: Long = 1L) =
        GetAppealablePenaltyRequest(
          penaltyId = penaltyId,
          hmrcOfficeNumber = "840123",
          schemeReferenceNumber = "098345"
        )

      val getCisAppealablePenaltyResponse = GetAppealablePenaltyResponse(
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

        val result: GetAppealablePenaltyResponse = service.getCisAppealablePenalty(getCisAppealablePenaltyRequest()).futureValue
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

        verify(connector, times(3)).getCisAppealablePenalty(any[GetAppealablePenaltyRequest])(any[HeaderCarrier])
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

    "getCisTaxpayer" - {

      def getCisTaxpayerRequest(schemeReferenceNumber: String = "AB456") =
        GetCisTaxpayerRequest(
          hmrcOfficeNumber = "123",
          schemeReferenceNumber = schemeReferenceNumber
        )

      val getCisTaxpayerResponse = GetCisTaxpayerResponse(
        uniqueId = "1000123",
        taxOfficeNumber = "123",
        taxOfficeRef = "AB456",
        aoDistrict = Some("123"),
        aoPayType = Some("P"),
        aoCheckCode = Some("X"),
        aoReference = Some("00123456"),
        validBusinessAddr = None,
        correlation = None,
        ggAgentId = None,
        employerName1 = Some("ABC"),
        employerName2 = None,
        agentOwnRef = None,
        schemeName = Some("ABC Builders Ltd"),
        utr = Some("1234567890"),
        enrolledSig = None
      )

      "must delegate to connector (happy path)" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisTaxpayer(eqTo(getCisTaxpayerRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.successful(getCisTaxpayerResponse))

        val result: GetCisTaxpayerResponse = service.getCisTaxpayer(getCisTaxpayerRequest()).futureValue
        result mustBe getCisTaxpayerResponse

        verify(connector).getCisTaxpayer(eqTo(getCisTaxpayerRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate failures from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val boom = UpstreamErrorResponse("CIS taxpayer not found", 404)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisTaxpayer(eqTo(getCisTaxpayerRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.getCisTaxpayer(getCisTaxpayerRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).getCisTaxpayer(eqTo(getCisTaxpayerRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must call connector exactly once per request" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisTaxpayer(eqTo(getCisTaxpayerRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.successful(getCisTaxpayerResponse))

        service.getCisTaxpayer(getCisTaxpayerRequest()).futureValue

        verify(connector, times(1)).getCisTaxpayer(eqTo(getCisTaxpayerRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must handle consecutive requests independently" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val request1 = getCisTaxpayerRequest()
        val request2 = getCisTaxpayerRequest("CD789")
        val request3 = getCisTaxpayerRequest("EF012")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisTaxpayer(eqTo(request1))(any[HeaderCarrier]))
          .thenReturn(Future.successful(getCisTaxpayerResponse))
        when(connector.getCisTaxpayer(eqTo(request2))(any[HeaderCarrier]))
          .thenReturn(Future.successful(getCisTaxpayerResponse))
        when(connector.getCisTaxpayer(eqTo(request3))(any[HeaderCarrier]))
          .thenReturn(Future.successful(getCisTaxpayerResponse))

        service.getCisTaxpayer(request1).futureValue mustBe getCisTaxpayerResponse
        service.getCisTaxpayer(request2).futureValue mustBe getCisTaxpayerResponse
        service.getCisTaxpayer(request3).futureValue mustBe getCisTaxpayerResponse

        verify(connector, times(3)).getCisTaxpayer(any[GetCisTaxpayerRequest])(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }

      "must propagate RuntimeException from connector" in {
        val connector = mock[RdsDataCacheConnector]
        val service = new CisService(connector)
        val boom = new RuntimeException("Connection failed")
        implicit val hc: HeaderCarrier = HeaderCarrier()

        when(connector.getCisTaxpayer(eqTo(getCisTaxpayerRequest()))(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex: Throwable = service.getCisTaxpayer(getCisTaxpayerRequest()).failed.futureValue
        ex mustBe boom

        verify(connector).getCisTaxpayer(eqTo(getCisTaxpayerRequest()))(any[HeaderCarrier])
        verifyNoMoreInteractions(connector)
      }
    }
  }
}
